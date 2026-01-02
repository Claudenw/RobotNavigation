package org.xenei.robot.common.mapping;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import com.google.common.annotations.VisibleForTesting;

import org.apache.commons.collections4.map.LRUMap;

import org.apache.sis.util.collection.WeakValueHashMap;
import org.locationtech.jts.geom.Coordinate;

import org.locationtech.jts.geom.Geometry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.common.GeometricObject;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.Obstacle;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.ScaleInfo;
import org.xenei.robot.common.planning.Solution;
import org.xenei.robot.common.sensor.distance.DistanceSensor;
import org.xenei.robot.common.utils.CoordUtils;
import org.xenei.robot.common.utils.GeometryUtils;
import org.xenei.robot.common.utils.RobutContext;

public final class Map {
    private static final Logger LOG = LoggerFactory.getLogger(Map.class);
    private final RobutContext ctxt;
    final MapStorage storage;
    final ScaleInfo scaleInfo;
    private final ObstacleHandler obstacleHandler;
    private RobutContext.TopicRegistration distanceSensorRegistration;

    /**
     * Compares Coordinates by XY positions.
     */
    public final Comparator<MapCoordinate> XY_COMPARE = MapCoordinate::compareTo;

    /**
     * Compares Coordinates by angle and then range.
     */
    public final Comparator<MapCoordinate> THETA_COMPARE = Comparator.comparingDouble(MapCoordinate::theta)
          .thenComparingDouble(MapCoordinate::range);

    /**
     * Compares Coordinates by range and then angle.
     */
    public final Comparator<MapCoordinate> RANGE_COMPARE = Comparator.comparingDouble(MapCoordinate::range).thenComparingDouble(MapCoordinate::theta);

    // do not expose without wrapping in synchronized map.
    private final WeakValueHashMap<Coordinate, MapLocation> coordinateMapLocationMap = new WeakValueHashMap<>(
            Coordinate.class);

    public Map(RobutContext ctxt, MapStorage storage) {
        this.ctxt = ctxt;
        this.storage = storage;
        this.scaleInfo = ctxt.scaleInfo;
        this.obstacleHandler = new ObstacleHandler();
    }

    /**
     * returns true if the coordinate is  being tracked as a location.
     * @param coordinate the coordinate to check/
     * @return {@code true} if the coordinate is being tracked.
     */
    @VisibleForTesting
    boolean hasRecordedCoordinate(Coordinate coordinate) {
        return coordinateMapLocationMap.containsKey(coordinate);
    }

    @VisibleForTesting
    ObstacleHandler getObstacleHandler() {
        return obstacleHandler;
    }

    /**
     * Clears the map of all objects.
     */
    public void clear() {
        obstacleHandler.clear();
    }

    /**
     * Returns {@code true} if there is a clear view from {@code source} to
     * {@code dest}.
     *
     * @param source
     *            the coordinates to start at
     * @param dest
     *            the coordinates to end it
     * @return true if there are no obstacles between source and dest.
     */
    public boolean isClearPath(MapCoordinate source, MapCoordinate dest) {
        return obstacleHandler.isClearPath(source, dest);
    }

    public MapCoordinate asMapCoordinate(ThetaAndRange thetaAndRange) {
        return new MapCoordinate(this, thetaAndRange.getCoordinate());
    }

    public MapCoordinate asMapCoordinate(Coordinate coordinate) {
        return new MapCoordinate(this, coordinate);
    }

    public MapCoordinate asMapCoordinate(Location location) {
        if (location instanceof MapCoordinate) {
            return (MapCoordinate) location;
        }
        return asMapCoordinate(location.getCoordinate());
    }

    public MapLocation asMapLocation(Coordinate coordinate) {
        return asMapLocation(asMapCoordinate(coordinate));
    }

    /**
     * Returns the map location for the location.
     * @param location the location to map.
     * @return the map location of {@code null} if the location is null.
     */
    public MapLocation asMapLocation(Location location) {
        if (location == null) {
            return null;
        }
        if (location instanceof MapLocation) {
            return (MapLocation) location;
        }
        return coordinateMapLocationMap.compute(location.getCoordinate(),
                (k, v) -> v != null ? v : storage.saveLocation(new MapLocation(this, asMapCoordinate(location))).join());
    }

    public MapPosition asMapPosition(Position position) {
        if (position instanceof MapPosition) {
            return (MapPosition) position;
        }
        return asMapPosition(asMapCoordinate(position), position.heading());
    }

    public MapPosition asMapPosition(Location location, double heading) {
        return new MapPosition(this, asMapLocation(location), heading);
    }

    public MapPosition asMapPosition(Coordinate coordinate, double heading) {
        return new MapPosition(this, asMapLocation(coordinate), heading);
    }

    public MapPath asPath(Collection<? extends MapLocation> points) {
        return new MapPath(this, points.stream().map(this::asMapLocation));
    }

    public MapObstacle asMapObstacle(Obstacle obstacle) {
        if (obstacle instanceof MapObstacle) {
            return (MapObstacle) obstacle;
        }
        return new MapObstacle(this, obstacle.getGeometry(), obstacle.uuid());
    }

    /**
     * Create an Obstacle.
     *
     * @param location
     *            The position of the object.
     * @return An obstacle.
     */
    public MapObstacle createObstacle(Coordinate location) {
        return obstacleHandler.addObstacleInBackground(asMapCoordinate(location)).join();
    }

    public MapObstacle createObstacle(GeometricObject geometricObject) {
        return obstacleHandler.addObstacleInBackground(geometricObject).join();
    }

    public CompletableFuture<MapObstacle> createObstacleInBackground(GeometricObject geometricObject) {
        return obstacleHandler.addObstacleInBackground(geometricObject);
    }

    /**
     * Create an obstacle that stretches from @{code first} to {@code last}.
     * @param start the first coordinate for the obstacle
     * @param end the last coordinate for the obstacle.
     * @return a MapObstacle including first and last.
     */
    public CompletableFuture<MapObstacle> createObstacleInBackground(MapCoordinate start, MapCoordinate end) {
        return createObstacleInBackground(createObstacleGeometry(start, end));
    }

    /**
     * Creates a geometric line from {@code start} to {@code end} with point at every resolution.
     * @param start the starting coordinate.
     * @param end the ending coordinate
     * @return a GeometricObject with the specified geometry.
     */
    private GeometricObject createObstacleGeometry(MapCoordinate start, MapCoordinate end) {
        double heading = CoordUtils.calcHeading(start, end);
        List<MapCoordinate> lst = new ArrayList<>();
        MapCoordinate current = start;
        do {
            lst.add(current);
            current = current.nextCoordinate(heading, scaleInfo.getResolution());
        } while(!current.sameCoordinate(end));
        lst.add(current);
        lst.add(end);

        return GeometricObject.of(ctxt.geometryUtils.asLine(lst.stream()));
    }

    /**
     * Create an obstacle that stretches from @{code first} to {@code last}.
     * @param start the first coordinate for the obstacle
     * @param end the last coordinate for the obstacle.
     * @return a MapObstacle including first and last.
     */
    public MapObstacle createObstacle(Coordinate start, Coordinate end) {
        return createObstacle(asMapCoordinate(start), asMapCoordinate(end));
    }

    /**
     * Create an obstacle that stretches from @{code first} to {@code last}.
     * @param start the first coordinate for the obstacle
     * @param end the last coordinate for the obstacle.
     * @return a MapObstacle including first and last.
     */
    public MapObstacle createObstacle(MapCoordinate start, MapCoordinate end) {
        return createObstacle(createObstacleGeometry(start, end));
    }

    /**
     * Adds a path to the planning graph.
     *
     * @param locations
     *            the coordinates of the path.
     */
    public MapPath addPath(Stream<MapLocation> locations) {
        return new MapPath(this, locations);
    }

    /**
     * Determines if there is a path from 'a' to 'b'
     *
     * @param a
     *            one end of a path.
     * @param b
     *            the other end of a path.
     * @return {@code true} if a path exists.
     */
    public boolean hasPath(MapLocation a, MapLocation b) {
        return storage.hasPath(a, b).join();
    }

    /**
     * Returns true if the coordinate is within an obstacle.
     *
     * @param mapCoordinate
     *            the coordinate to check.
     * @return true if the point is in an obstacle, false otherwise.
     */
    public boolean isObstacle(MapCoordinate mapCoordinate) {
        return obstacleHandler.isObstacle(mapCoordinate);
    }

    /**
     * Gets the MapObstacle instance all the known obstacles.
     *
     * @return a CompletableFuture of stream of all known MapObstacles
     */
    public CompletableFuture<Stream<MapObstacle>> getObstacles() {
        return getObstacles(null);
    }

    /**
     * Gets the MapObstacle instance for all the known obstacles within the bounding box.
     * If the bounding box is {@code null} returns all the MapObstacles.
     *
     * @param boundingBox the geometry to restrict the search by.  May be {@code null}
     * @return a CompletableFuture of stream of all known MapObstacles within the bounding box.
     */
    public CompletableFuture<Stream<MapObstacle>> getObstacles(Geometry boundingBox) {
        CompletableFuture<Stream<Obstacle>> obstacleStream =  boundingBox == null ? storage.getObstacles() : storage.findTouchingObstacles(GeometricObject.of(boundingBox));
        return obstacleStream.thenApply(obstacles -> obstacles.map(this::asMapObstacle));
    }

    /**
     * Gets the MapLocation from the storage level
     * @return A stream of the MapLocations
     */
    public CompletableFuture<Stream<MapLocation>> getLocations() {
        return storage.getLocations(null).thenApply(stream -> stream.map(coord -> {
            MapLocation mapLocation = coordinateMapLocationMap.get(coord);
            return (mapLocation == null) ? new MapLocation(this, asMapCoordinate(coord)) : mapLocation;
        }));
    }

    /**
     * Get the context info for this map.
     *
     * @return the Context.
     */
    public RobutContext getContext() {
        return ctxt;
    }

    /**
     * Registers the map with the distance sensors
     */
    public void registerDistanceSensors() {
        if (distanceSensorRegistration == null) {
            ScaleInfo scaleInfo = getContext().scaleInfo;
            RobutContext.Topic<DistanceSensor.Readings> topic = getContext().distanceSensorTopic;
            distanceSensorRegistration = topic.listen(readings -> readings.readings().stream().map(scaleInfo::round)
                    .filter(relativeLocation -> !relativeLocation.isNaN() && !relativeLocation.isInfinite())
                    .map(relativeLocation -> asMapCoordinate(CoordUtils.add(readings.origin().getCoordinate(), relativeLocation.getCoordinate())))
                    .forEach(this::createObstacleInBackground));
        }
    }

    /**
     * A Visualization of a map.
     */
    @FunctionalInterface
    public interface Visualization {
        void redraw();
    }

    public interface VisualizationInitializer {
        Map map();
        Supplier<Solution> solutionSupplier();
        Supplier<Position> positionSupplier();
        Supplier<Location> targetSupplier();
    }

    public Stream<MapLocation> getCandidateLocations(MapLocation location, MapLocation target) {
        return storage.getLocations(GeometryUtils.createBoundingBox(location, target).toGeometry(ctxt.geometryFactory)).join()
                .map(this::asMapLocation);

    }

    @VisibleForTesting
    class ObstacleHandler {
        private final RobutContext ctxt = Map.this.ctxt;
        private final java.util.Map<UUID, Geometry> activeObstacles = Collections.synchronizedMap(new WeakValueHashMap<>(
                UUID.class));
        @VisibleForTesting
        final java.util.Map<UUID, Geometry> cache = Collections.synchronizedMap(new LRUMap<>());

        /**
         * Adds the obstacle to the list of active obstacles and puts it in the LRU cache.
         * @param obstacle the obstacle to cache
         * @return the argument.
         */
        private MapObstacle addCache(MapObstacle obstacle) {
            LOG.debug("Added obstacle {}", obstacle.uuid());
            activeObstacles.put(obstacle.uuid(), obstacle.getGeometry());
            cache.put(obstacle.uuid(), obstacle.getGeometry());
            return obstacle;
        }

        private void removeCache(Obstacle obstacle) {
            activeObstacles.remove(obstacle.uuid());
            cache.remove(obstacle.uuid());
        }

        void clear() {
            cache.clear();
        }

        /**
         * Retrieve all the obstacles from the cache that touch geometry.
         * Updates the LRU cache entry for the found objects.
         * @param geometry the geometry to match.
         * @return the set of Obstacles that interset the geometry.
         */
        @VisibleForTesting
        Stream<Obstacle> matchingCache(Geometry geometry) {
            HashMap<UUID, Geometry> result = new HashMap<>(activeObstacles);
            result.entrySet().removeIf(entry -> ctxt.scaleInfo.compare(ScaleInfo.OP.NE, entry.getValue().distance(geometry), 0));
            result.keySet().forEach(cache::get);
            return result.entrySet().stream().map(entry -> new MapObstacle(Map.this, entry.getValue(), entry.getKey()));
        }

        private CompletableFuture<MapObstacle> processObstacles(GeometricObject obstacle, Stream<Obstacle> obstacles) {
            List<Obstacle> obstacleList = obstacles.toList();

            if (obstacleList.isEmpty()) {
                if (obstacle instanceof MapObstacle) {
                    return CompletableFuture.completedFuture(addCache((MapObstacle) obstacle));
                } else {
                    MapObstacle mapObstacle = addCache(new MapObstacle(Map.this, obstacle.getGeometry()));
                    return storage.addObstacle(mapObstacle).thenApply(nada -> mapObstacle);
                }
            }
            Set<GeometricObject> touching = new HashSet<>(obstacleList);
            touching.add(obstacle);
            Geometry geometry = ctxt.geometryUtils.makeCloud(ctxt.scaleInfo.getResolution(), touching.stream());
            MapObstacle mapObstacle = addCache(new MapObstacle(Map.this, geometry));
            CompletableFuture<?> future = CompletableFuture.allOf(
                    storage.addObstacle(mapObstacle),
                    storage.removeObstacles(obstacleList.stream().map(Obstacle::uuid)),
                    storage.removeCoordinates(mapObstacle),
                    ctxt.submit(() -> obstacleList.forEach(this::removeCache)));
            // delete the target records
            Set<MapLocation> locationsToRemove =
                    coordinateMapLocationMap.values().stream().filter(mapLocation -> geometry.covers(mapLocation.getGeometry()))
                            .collect(Collectors.toSet());
            if (!locationsToRemove.isEmpty()) {
                coordinateMapLocationMap.values().forEach(mapLocation -> mapLocation.removeTargets(locationsToRemove));
                for (MapLocation location : locationsToRemove) {
                    coordinateMapLocationMap.remove(location.getCoordinate());
                }
            }
            return future.thenApply(x -> mapObstacle);
        }

        /**
         * Creates adds an obstacle to the map.
         *
         * @param obstacle
         *            The geometric object that is the obstacle.
         * @return the MapObstacle that was created.
         */
        public CompletableFuture<MapObstacle> addObstacleInBackground(GeometricObject obstacle) {
            return storage.findTouchingObstacles(obstacle)
                    .thenApply(obstacles -> processObstacles(obstacle, obstacles).join());
        }

        boolean isClearPath(MapCoordinate start, MapCoordinate end) {
            Geometry path = ctxt.geometryUtils.asPath(ctxt.scaledRadius, start.getCoordinate(), end.getCoordinate());
            return matchingCache(path).findAny().isEmpty() && storage.findTouchingObstacles(GeometricObject.of(path)).join().findAny().isEmpty();
        }

        boolean isObstacle(MapCoordinate point) {
            return matchingCache(point.getGeometry())
                    .anyMatch(obstacle -> ctxt.geometryUtils.scale(obstacle.getGeometry()).covers(point.getGeometry()))
                     || storage.findTouchingObstacles(point).join()
                    .anyMatch(obstacle -> ctxt.geometryUtils.scale(obstacle.getGeometry()).covers(point.getGeometry()));
        }
    }
}
