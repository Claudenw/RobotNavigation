package org.xenei.robot.common.mapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.nats.client.Options;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.xenei.robot.common.ChassisInfoTest;
import org.xenei.robot.common.GeometricObject;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.Obstacle;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.ScaleInfo;
import org.xenei.robot.common.TestingConfiguration;
import org.xenei.robot.common.utils.AngleUtils;
import org.xenei.robot.common.utils.DoubleUtils;
import org.xenei.robot.common.utils.RobutContext;
import org.xenei.robot.mapper.map.RDFStorage;
import org.xenei.robot.mapper.rdf.Namespace;

public class MapTest {
    private static final ScaleInfo scaleInfo = ScaleInfo.DEFAULT;
    private Map underTest;
    private TestingStorage testingStorage;

    private static Location makeLoc(double x, double y) {
        return Location.asLocation(new Coordinate(x, y));
    }

    public static final Location[] coordinates = {makeLoc(-4, -4), makeLoc(-4, -3), makeLoc(-4, -1), makeLoc(-2, -4),
            makeLoc(-2, -2), makeLoc(-1, -4), makeLoc(-1, -2), makeLoc(0, -4), makeLoc(0, -2), makeLoc(2, -4),
            makeLoc(2, -3), makeLoc(2, -1)};

    static List<Location[]> paths = new ArrayList<>();

    static final Position position = Position.asPosition(new Coordinate(-1, -3), 0);

    @BeforeAll
    public static void setupPaths() {
        for (Location e : coordinates) {
            paths.add(new Location[]{position, e});
        }
    }

    @BeforeEach
    void setup() {
        testingStorage = new TestingStorage();
        RobutContext.Builder builder = TestingConfiguration.getContextBuilder("MapTest");
        underTest = new Map(builder.build(), testingStorage);
    }

    @AfterEach
    void teardown() {
        underTest.getContext().close();
    }

    @Test
    void asMapLocationTest() {
        Coordinate coordinate = new Coordinate(-1, 1);
        MapLocation mapLocation = underTest.asMapLocation(Location.asLocation(coordinate));
        assertEquals(testingStorage.locations.get(coordinate).coordinate, mapLocation.getCoordinate());
        assertEquals(testingStorage.locations.get(coordinate).geometry, mapLocation.getGeometry());
        assertTrue(underTest.hasRecordedCoordinate(coordinate));


        Location location = makeLoc(-1, -2);
        // hold MapLocation to ensure garbage collection does not remove it.
        mapLocation = underTest.asMapLocation(location);
        assertTrue(underTest.hasRecordedCoordinate(location.getCoordinate()));
    }

    @Test
    void testLocationCache() {
        Coordinate coordinate = new Coordinate(-1, 1);
        MapLocation mapLocation = underTest.asMapLocation(Location.asLocation(coordinate));
        List<Location> lst = new ArrayList<>();
        lst.add(mapLocation);
        // make sure mapLocation is garbage
        mapLocation = null;
        assertTrue(underTest.hasRecordedCoordinate(coordinate));
        lst.clear();
        System.gc();
        await().atMost(Duration.ofSeconds(1))
                .untilAsserted(() -> assertThat(underTest.hasRecordedCoordinate(coordinate)).isFalse());
    }

    @Test
    void testObstacleClearOnGC() {
        Coordinate obst = new Coordinate(1, 1);
        underTest.createObstacle(obst);
        underTest.getObstacleHandler().cache.clear();
        System.gc();
        Geometry geom = underTest.asMapCoordinate(obst).getGeometry();
        await().atMost(Duration.ofSeconds(1))
                .untilAsserted(() -> assertThat(underTest.getObstacleHandler().matchingCache(geom)).isEmpty());
    }

    @Test
    void clearTest() {
        Coordinate obst = new Coordinate(1, 1);
        Coordinate loc = new Coordinate(3, 3);
        underTest.createObstacle(obst);
        underTest.asMapLocation(Location.asLocation(loc));
        assertThat(underTest.isObstacle(underTest.asMapCoordinate(obst))).isTrue();
        assertThat(underTest.getLocations().join().collect(Collectors.toList())).size().isEqualTo(1);

        underTest.clear();
        testingStorage.clear();
        System.gc();
        await().atMost(Duration.ofSeconds(2))
                .untilAsserted(() -> assertThat(underTest.isObstacle(underTest.asMapCoordinate(obst))).isFalse());
        assertThat(underTest.getLocations().join().collect(Collectors.toList())).isEmpty();
    }

    @Test
    void isClearPathTest() {
        MapCoordinate source = underTest.asMapCoordinate(new Coordinate(0, 1));
        MapCoordinate dest = underTest.asMapCoordinate(new Coordinate(0, -1));
        assertThat(underTest.isClearPath(source, dest)).isTrue();

        MapObstacle originObstacle = underTest.createObstacle(Location.ORIGIN.getCoordinate());
        assertThat(underTest.isClearPath(source, dest)).isFalse();
    }

    @Test
    void asMapCoordinateThetaAndRangeTest() {
        ThetaAndRange thetaAndRange = new ThetaAndRange(AngleUtils.RADIANS_45, DoubleUtils.SQRT2);
        MapCoordinate mapCoordinate = underTest.asMapCoordinate(thetaAndRange);
        assertThat(underTest.getContext().scaleInfo.compare(ScaleInfo.OP.EQ, mapCoordinate.getCoordinate(), thetaAndRange.getCoordinate()));
        assertThat(mapCoordinate.getMap()).isEqualTo(underTest);
    }

    @Test
    void asMapCoordinateCoordinateTest() {
        MapCoordinate mapCoordinate = underTest.asMapCoordinate(new Coordinate(3, 3));
        assertThat(mapCoordinate.getCoordinate()).isEqualTo(new Coordinate(3, 3));
        assertThat(mapCoordinate.getMap()).isEqualTo(underTest);
    }

    @Test
    void asMapCoordinateLocationTest() {
        Location location = Location.asLocation(new Coordinate(3, 3));
        MapCoordinate mapCoordinate = underTest.asMapCoordinate(location);
        assertThat(mapCoordinate.getCoordinate()).isEqualTo(new Coordinate(3, 3));
        assertThat(mapCoordinate.getMap()).isEqualTo(underTest);
    }

    @Test
    void asMapLocationCoordinateTest() {
        MapLocation location = underTest.asMapLocation(new Coordinate(1, 2));
        assertThat(location.getCoordinate()).isEqualTo(new Coordinate(1, 2));
        assertThat(location.getMap()).isEqualTo(underTest);
    }

    @Test
    void MapLocationMapCoordinateTest() {
        MapCoordinate mapCoordinate = underTest.asMapCoordinate(new Coordinate(1, 2));
        MapLocation location = underTest.asMapLocation(mapCoordinate);
        assertThat(location.getCoordinate()).isEqualTo(new Coordinate(1, 2));
        assertThat(location.getMap()).isEqualTo(underTest);
    }


    @Test
    void asMapLocationLocationTest() {
        MapLocation location = underTest.asMapLocation(Location.asLocation(new Coordinate(1, 2)));
        assertThat(location.getCoordinate()).isEqualTo(new Coordinate(1, 2));
        assertThat(location.getMap()).isEqualTo(underTest);
    }

    @Test
    void asMapPositionPositionTest() {
        MapPosition mapPosition = underTest.asMapPosition(Position.asPosition(new Coordinate(1, 1), AngleUtils.RADIANS_45));
        assertThat(mapPosition.getCoordinate()).isEqualTo(new Coordinate(1, 1));
        assertThat(mapPosition.heading()).isEqualTo(AngleUtils.RADIANS_45);
        assertThat(mapPosition.getMap()).isEqualTo(underTest);
    }

    @Test
    void asMapPositionMapCoordinateHeadingTest() {
        MapPosition mapPosition = underTest.asMapPosition(underTest.asMapCoordinate(new Coordinate(1, 1)), AngleUtils.RADIANS_45);
        assertThat(mapPosition.getCoordinate()).isEqualTo(new Coordinate(1, 1));
        assertThat(mapPosition.heading()).isEqualTo(AngleUtils.RADIANS_45);
        assertThat(mapPosition.getMap()).isEqualTo(underTest);
    }

    @Test
    void asPathTest() {
        MapLocation a = underTest.asMapLocation(underTest.asMapCoordinate(new Coordinate(1, 1)));
        MapLocation b = underTest.asMapLocation(underTest.asMapCoordinate(new Coordinate(1, 5)));
        MapLocation c = underTest.asMapLocation(underTest.asMapCoordinate(new Coordinate(5, 5)));
        MapPath path = underTest.asPath(Arrays.asList(a, b, c));

        assertTrue(underTest.hasPath(a, b));
        assertTrue(underTest.hasPath(b, c));
        assertTrue(underTest.hasPath(a, c));

        assertThat(path.getGeometry().distance(a.getGeometry())).isEqualTo(0);
        assertThat(path.getGeometry().distance(b.getGeometry())).isEqualTo(0);
        assertThat(path.getGeometry().distance(c.getGeometry())).isEqualTo(0);
    }

    @Test
    void asMapObstacleTest() {
        Geometry geometry = underTest.getContext().geometryUtils.asPolygon(Location.ORIGIN, DoubleUtils.SQRT2, 10);
        Obstacle obstacle = Obstacle.asObstacle(UUID.randomUUID(), geometry);
        MapObstacle mapObstacle = underTest.asMapObstacle(obstacle);
        assertThat(mapObstacle.uuid()).isEqualTo(obstacle.uuid());
        assertThat(mapObstacle.getGeometry()).isEqualTo(obstacle.getGeometry());
    }

    @Test
    void createObstacleCoordinateTest() {
        Coordinate coord = new Coordinate(1, 1);
        MapObstacle mapObstacle = underTest.createObstacle(coord);
        assertThat(underTest.isObstacle(underTest.asMapCoordinate(coord))).isTrue();
    }

    @Test
    void createObstacleGeometricObjectTest() {
        GeometricObject geometricObject = GeometricObject.of(underTest.getContext().geometryUtils.asPolygon(Location.ORIGIN, DoubleUtils.SQRT2, 10));
        MapObstacle obstacle = underTest.createObstacle(geometricObject);
        for (int x = -2; x <= 2; x++) {
            for (int y = -2; y <= 2; y++) {
                MapCoordinate mc =underTest.asMapCoordinate(new Coordinate(x / 2.0, y / 2.0));
                assertThat(underTest.isObstacle(mc)).as(mc.toString()).isTrue();
            }
        }
    }


    @Test
    void createObstacleInBackgroundGeometricObjectTest() {
        GeometricObject geometricObject = GeometricObject.of(underTest.getContext().geometryUtils.asPolygon(Location.ORIGIN, DoubleUtils.SQRT2, 10));
        MapObstacle obstacle = underTest.createObstacleInBackground(geometricObject).join();
        int lowerLimit = -3;
        int upperLimit = 3;
        // the expected values are mirrored around the origin.
        Coordinate[] y15 = {new Coordinate(-.5, 1.5), new Coordinate(0, 1.5), new Coordinate(.5, 1.5)};
        Coordinate[] y1 = {new Coordinate(-1, 1), new Coordinate( -.5, 1), new Coordinate(0, 1), new Coordinate(.5, 1), new Coordinate(1, 1)};
        Coordinate[] y5 = {new Coordinate(-1, .5), new Coordinate( -.5, .5), new Coordinate(0, .5), new Coordinate(.5, .5), new Coordinate(1, .5)};
        Coordinate[] y0 = {new Coordinate(-1.5, 0), new Coordinate(-1, 0), new Coordinate( -.5, 0), new Coordinate(0, 0), new Coordinate(.5, 0), new Coordinate(1, 0), new Coordinate(1.5, 0)};
        Coordinate[] y_5 = {new Coordinate(-1, -.5), new Coordinate( -.5, -.5), new Coordinate(0, -.5), new Coordinate(.5, -.5), new Coordinate(1, -.5)};
        Coordinate[] y_1 = {new Coordinate(-1, -1), new Coordinate( -.5, -1), new Coordinate(0, -1), new Coordinate(.5, -1), new Coordinate(1, -1)};
        Coordinate[] y_15 = {new Coordinate(-.5, -1.5), new Coordinate(0, -1.5), new Coordinate(.5, -1.5)};

        List<Coordinate> inside = new ArrayList<>();
        inside.addAll(Arrays.asList(y15));
        inside.addAll(Arrays.asList(y1));
        inside.addAll(Arrays.asList(y5));
        inside.addAll(Arrays.asList(y0));
        inside.addAll(Arrays.asList(y_15));
        inside.addAll(Arrays.asList(y_1));
        inside.addAll(Arrays.asList(y_5));

        for (int x = lowerLimit; x <= upperLimit; x++) {
            for (int y = lowerLimit; y <= upperLimit; y++) {
                boolean expected = Math.abs(x) != upperLimit || Math.abs(y) != upperLimit;
                Coordinate coordinate = new Coordinate(x / 2.0, y / 2.0);
                MapCoordinate mapCoordinate = underTest.asMapCoordinate(coordinate);
                assertThat(underTest.isObstacle(mapCoordinate)).describedAs(mapCoordinate.toString()).isEqualTo(inside.contains(mapCoordinate.getCoordinate()));
            }
        }
    }

    @Test
    void createObstacleInBackgroundStartEndTest() {
        MapCoordinate start = underTest.asMapCoordinate(new Coordinate(1, 1));
        MapCoordinate end = underTest.asMapCoordinate(new Coordinate(10, 10));
        MapObstacle mapObstacle = underTest.createObstacleInBackground(start, end).join();
        assertThat(underTest.getObstacles().join().count()).isEqualTo(1);

        for (int i = 1; i <= 10; i++) {
            MapCoordinate coord = underTest.asMapCoordinate(new Coordinate(i, i));
            assertThat(underTest.isObstacle(coord)).describedAs(coord.toString()).isTrue();
            coord = underTest.asMapCoordinate(new Coordinate(i + 1, i));
            assertThat(underTest.isObstacle(coord)).describedAs(coord.toString()).isFalse();
        }
    }

    @Test
    void createObstacleStartEndTest() {
        MapCoordinate start = underTest.asMapCoordinate(new Coordinate(1, 1));
        MapCoordinate end = underTest.asMapCoordinate(new Coordinate(10, 10));
        MapObstacle mapObstacle = underTest.createObstacle(start, end);
        assertThat(underTest.getObstacles().join().count()).isEqualTo(1);

        for (int i = 1; i <= 10; i++) {
            MapCoordinate coord = underTest.asMapCoordinate(new Coordinate(i, i));
            assertThat(underTest.isObstacle(coord)).describedAs(coord.toString()).isTrue();
            coord = underTest.asMapCoordinate(new Coordinate(i + 1, i));
            assertThat(underTest.isObstacle(coord)).describedAs(coord.toString() + " in " + mapObstacle.toString()).isFalse();
        }
    }

    @Test
    void hasPathTest() {
        MapLocation a = underTest.asMapLocation(new Coordinate(1, 1));
        MapLocation b = underTest.asMapLocation(new Coordinate(5, 5));
        MapLocation c = underTest.asMapLocation(new Coordinate(1, 10));

        underTest.addPath(Stream.of(a, b));
        underTest.addPath(Stream.of(b, c));

        assertThat(underTest.hasPath(a, b)).isTrue();
        assertThat(underTest.hasPath(b, c)).isTrue();
        assertThat(underTest.hasPath(a, c)).isTrue();
    }

    @Test
    void isObstacleTest() {
        List<Coordinate> coords = Arrays.asList(new Coordinate(1, 1), new Coordinate(10, 10));
        coords.forEach(underTest::createObstacle);

        coords.forEach(coord -> assertThat(underTest.isObstacle(underTest.asMapCoordinate(coord)))
                .describedAs("Coordinate " + coord).isTrue());

        assertThat(underTest.isObstacle(underTest.asMapCoordinate(new Coordinate(5, 5)))).isFalse();
    }

    @Test
    void getObstaclesTest() {
        List<Coordinate> coords = Arrays.asList(new Coordinate(1, 1), new Coordinate(10, 10));
        coords.forEach(underTest::createObstacle);
        List<Geometry> expected = coords.stream().map(underTest.getContext().geometryUtils::asPoint).collect(Collectors.toList());

        List<Geometry> actual = underTest.getObstacles().join().map(Obstacle::getGeometry).collect(Collectors.toList());
        assertThat(actual).containsExactlyInAnyOrderElementsOf(expected);
    }


    @Test
    void getLocationsTest() {
        List<MapLocation> expected = Arrays.stream(coordinates).map(underTest::asMapLocation).collect(Collectors.toList());
        List<MapLocation> actual = underTest.getLocations().join().collect(Collectors.toList());
        assertThat(actual).containsExactlyInAnyOrderElementsOf(expected);

        // release all the old data.
        expected.clear();
        actual.clear();

        // verify read from storage.
        List<Location> actual2 = underTest.getLocations().join().map(mapLocation -> Location.asLocation(mapLocation.getCoordinate())).toList();
        assertThat(actual2).containsExactlyInAnyOrderElementsOf(Arrays.asList(coordinates));

        testingStorage.clearLocations();
        assertThat(underTest.getLocations().join().findAny()).isEmpty();
    }


    @Test
    void getContextTest() {
        assertNotNull(underTest.getContext());
    }

    /**
     * A testing storage that stores data in memory but does not
     * keep references to the arguments.
     */
    public static class TestingStorage implements MapStorage {

        record TestingData(Coordinate coordinate, Geometry geometry, boolean visited) {
            Location location() {
                return Location.asLocation(coordinate);
            }
        }

        private final ConcurrentMap<UUID, Obstacle> obstacles = new ConcurrentHashMap<>();
        private final ConcurrentMap<Coordinate, TestingData> locations = new ConcurrentHashMap<>();
        private final Model model = ModelFactory.createDefaultModel();

        void clearLocations() {
            locations.clear();
        }

        @Override
        public MapStorage.Reports<String> getReports() {
            throw new UnsupportedOperationException();
        }

        @Override
        public CompletableFuture<MapLocation> saveLocation(MapLocation mapLocation) {
            locations.put(mapLocation.getCoordinate(), new TestingData(mapLocation.getCoordinate(), mapLocation.getGeometry(), mapLocation.wasVisited()));
            return CompletableFuture.completedFuture(mapLocation);
        }

        @Override
        public CompletableFuture<Stream<Coordinate>> getLocations(Geometry boundingBox) {
            Stream<TestingData> testingDataStream = locations.values().stream();
            if (boundingBox != null) {
                testingDataStream = testingDataStream.filter(testingData -> boundingBox.covers(testingData.geometry));
            }
            return CompletableFuture.completedFuture(testingDataStream.map(TestingData::coordinate));
        }

        @Override
        public CompletableFuture<java.util.Map<Coordinate, Set<Coordinate>>> getPath(MapLocation start, MapLocation end) {
            throw new NotImplementedException();
        }

        @Override
        public void clear() {
            obstacles.clear();
            locations.clear();
            model.removeAll();
        }

        @Override
        public CompletableFuture<?> addPath(Collection<MapLocation> locations) {
            Iterator<MapLocation> iter = locations.iterator();
            MapLocation start = iter.next();
            MapLocation end;
            while (iter.hasNext()) {
                end = iter.next();
                model.add(RDFStorage.asResource(start), Namespace.path, RDFStorage.asResource(end));
                start = end;
            }
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<Stream<Obstacle>> findTouchingObstacles(final GeometricObject geometricObject) {
            return CompletableFuture.completedFuture(obstacles.values().stream().filter(geom ->
                    scaleInfo.compare(ScaleInfo.OP.EQ, geom.getGeometry().distance(geometricObject.getGeometry()), 0)
            ));
        }

        @Override
        public CompletableFuture<?> removeObstacles(Stream<UUID> obstacleIds) {
            obstacleIds.forEach(obstacles::remove);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<?> addObstacle(MapObstacle mapObstacle) {
            // make a deep copy to allow map obstacleHandler.activeObstacles to release the reference.
            obstacles.put(mapObstacle.uuid(), Obstacle.asObstacle(mapObstacle.uuid(), mapObstacle.getGeometry().copy()));
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<Stream<Obstacle>> getObstacles() {
            return CompletableFuture.completedFuture(obstacles.values().stream());
        }

        @Override
        public CompletableFuture<?> removeCoordinates(GeometricObject boundingBox) {
            locations.values().stream().filter(
                            testingData -> boundingBox.getGeometry().covers(testingData.geometry()))
                    .forEach(testingData -> locations.remove(testingData.coordinate()));
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<Boolean> hasPath(MapLocation a, MapLocation b) {
            boolean result = QueryExecutionFactory.create(new AskBuilder().addWhere(
                            RDFStorage.asResource(a), RDFStorage.PATH_QUERY_PREDICATE, RDFStorage.asResource(b)).build(), model)
                    .execAsk();
            return CompletableFuture.completedFuture(result);
        }
    }
}
