package org.xenei.robot.common.mapping;

import org.locationtech.jts.geom.Coordinate;
import org.xenei.robot.common.GeometricObject;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.ScaleInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

/**
 * A coordinate that has been quantized into a map coordinate.
 */
public class MapLocation extends MapCoordinate implements GeometricObject {

    private final ConcurrentHashMap<Coordinate, MapTargetData> targetData;
    private final AtomicBoolean visited;

    protected MapLocation(final Map onMap, MapCoordinate mapCoordinate) {
        super(onMap, mapCoordinate.getCoordinate());
        MapLocation mapLocation = mapCoordinate instanceof MapLocation ? (MapLocation) mapCoordinate : null;
        this.targetData = mapLocation == null ? new ConcurrentHashMap<>() : mapLocation.targetData;
        this.visited = mapLocation == null ? new AtomicBoolean(false) : mapLocation.visited;
    }

    public final void setVisited() {
        if (visited.compareAndSet(false, true)) {
            getMap().storage.saveLocation(this);
        }
    }

    public boolean wasVisited() {
        return visited.get();
    }

    @Override
    public int compareTo(Location location) {
        if (getMap().getContext().scaleInfo.compare(ScaleInfo.OP.EQ, this, location)) {
            return 0;
        }
        return getCoordinate().compareTo(location.getCoordinate());
    }

    private MapTargetData computeTargetData(MapLocation target) {
        return targetData.computeIfAbsent(target.getCoordinate(), k -> new MapTargetData(this, target));
    }

    public final boolean isIndirect(MapLocation target) {
        return computeTargetData(target).indirect();
    }

    public final boolean hasPath(MapLocation target) {
        return getMap().hasPath(this, target);
    }

    public final MapPath getPath(MapLocation target) {
        if (!isIndirect(target)) {
            return getMap().asPath(Arrays.asList(this, target));
        }
        return getMap().asPath(dijkstra(target, getMap().storage.getPath(this, target).join()));
    }

    public final MapTargetData getTargetData(MapLocation mapLocation) {
        return computeTargetData(mapLocation);
    }

    final void removeTargets(Set<MapLocation> targets) {
        for (MapLocation target : targets) {
            if (this.targetData.remove(target.getCoordinate()) != null) {
                target.targetData.remove(this.getCoordinate());
            }
        }
    }

    /**
     * Gets the known positions in the bounding box defined by the start position with the target in the middle.
     *
     * @param target the ending location.
     * @return the list of potential map locations to get to the target.
     */
    public final Stream<MapLocation> getCandidateLocations(MapLocation target) {
        return getMap().getCandidateLocations(this, target);
    }


    private MapLocation coordinateToLocation(Coordinate coordinate) {
        return getMap().asMapLocation(getMap().asMapCoordinate(coordinate));
    }

    private List<MapLocation> dijkstra(MapLocation target, java.util.Map<Coordinate, Set<Coordinate>> segmentPairs) {

        /* Use Dijkstra's method */
        /*
         * set all distance to infinity
         * set distance for start = 0;
         * add start to visited list
         * for every start.targetData.isDirect and targetData.target not in visited list, add targetData.target to queue with distance from start
         * find the shortest distance in the queue
         *
         * record distance and parent.
         * stop when shortest distance MapLocation = target
         */
        Set<Coordinate> seen = new HashSet<>();
        HashMap<MapLocation, PartialPath> partialPathMap = new HashMap<>();
        segmentPairs.keySet().stream().map(k -> {
                    MapLocation kLoc = coordinateToLocation(k);
                    return kLoc.sameCoordinate(this) ? new PartialPath(kLoc, 0, null) :
                            new PartialPath(kLoc, Double.POSITIVE_INFINITY, null);})
                .forEach(partialPath -> partialPathMap.put(partialPath.source, partialPath));

        List<PartialPath> nodeList = new ArrayList<>(partialPathMap.values());

        // dijkstra's algorithm
        while (!segmentPairs.isEmpty()) {
            nodeList.sort(Comparator.comparingDouble(a -> a.cost));
            PartialPath pathSegment = nodeList.stream().filter(p -> !seen.contains(p.source.getCoordinate())).findFirst().get();
            seen.add(pathSegment.source.getCoordinate());
            Set<Coordinate> adjacentNodes = segmentPairs.get(pathSegment.source.getCoordinate());
            for (Coordinate adj : adjacentNodes) {
                MapLocation adjacent = coordinateToLocation(adj);
                double cost = pathSegment.cost + pathSegment.source.getTargetData(adjacent).distance();
                PartialPath adjacentPath = partialPathMap.get(adjacent);
                if (adjacentPath.cost > cost) {
                    adjacentPath.cost = cost;
                    adjacentPath.parent = pathSegment.source;
                    if (adjacentPath.source.equals(target)) {
                        List<MapLocation> segments = new ArrayList<>();
                        segments.add(adjacentPath.source);
                        while (adjacentPath.parent != null) {
                            adjacentPath = partialPathMap.get(adjacentPath.parent);
                            segments.add(adjacentPath.source);
                        }
                        Collections.reverse(segments);
                        return segments;
                    }
                }
            }
            segmentPairs.remove(pathSegment.source.getCoordinate());
        }
        throw new IllegalStateException("No path found");
    }

    private static class PartialPath implements Comparable<PartialPath> {
        private final MapLocation source;
        private double cost;
        private MapLocation parent;

        PartialPath(MapLocation source, double cost, MapLocation parent) {
            this.source = source;
            this.cost = cost;
            this.parent = parent;
        }

        @Override
        public int compareTo(PartialPath o) {
            return Double.compare(cost, o.cost);
        }
    }
}
