package org.xenei.robot.common.mapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
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
import org.xenei.robot.common.planning.Solution;
import org.xenei.robot.common.utils.AngleUtils;
import org.xenei.robot.common.utils.DoubleUtils;
import org.xenei.robot.common.utils.RobutContext;
import org.xenei.robot.mapper.map.RDFStorage;
import org.xenei.robot.mapper.rdf.Namespace;

public class MapTest {
    private static final ScaleInfo scaleInfo = ScaleInfo.DEFAULT;
    private static final RobutContext ctxt = new RobutContext(scaleInfo, ChassisInfoTest.DEFAULT);

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


    Solution solution;

    @BeforeAll
    public static void setupPaths() {
        for (Location e : coordinates) {
            paths.add(new Location[]{position, e});
        }
    }

    @BeforeEach
    void setup() {
        testingStorage = new TestingStorage();
        underTest = new Map(ctxt, testingStorage);
    }

//    private void setup() {
//        underTest = new Map(ctxt, new TestingStorage());
//        MapLibrary.map2(underTest);
//        solution = new Solution();
//        solution.add(underTest.asMapPosition(position));
//        MapLocation target = underTest.asMapLocation(makeLoc(-1, 1));
//        cMap = new DebugViz(1, underTest, () -> solution, () -> position, () -> target);
//
//        Arrays.stream(coordinates).forEach(coordinate -> underTest.asMapLocation(coordinate)
//                .getTargetData(target));
//    }

    @Test
    void asMapLocationTest() {
        Coordinate coordinate = new Coordinate(-1, 1);
        MapLocation mapLocation = underTest.asMapLocation(Location.asLocation(coordinate));
        assertEquals(testingStorage.locations.get(coordinate).coordinate, mapLocation.getCoordinate());
        assertEquals(testingStorage.locations.get(coordinate).geometry, mapLocation.getGeometry());
        assertTrue(underTest.hasRecordedCoordinate(coordinate));
        mapLocation = underTest.asMapLocation(makeLoc(-1, -2));
        assertTrue(underTest.hasRecordedCoordinate(coordinate));
    }

    @Test
    void testLocationCache() {
        Coordinate coordinate = new Coordinate(-1, 1);
        MapLocation mapLocation = underTest.asMapLocation(Location.asLocation(coordinate));
        List<Location> lst = new ArrayList<>();
        lst.add(mapLocation);
        mapLocation = null;
        assertTrue(underTest.hasRecordedCoordinate(coordinate));
        lst.clear();
        assertFalse(underTest.hasRecordedCoordinate(coordinate));
    }

//    @Test
//    void addCoordTest_complete() {
//        TestingStorage testingStorage = new TestingStorage();
//        underTest = new Map(ctxt, testingStorage);
//        MapLocation target = underTest.asMapLocation(makeLoc(-1, 1));
//
//        MapLocation mapLocation = underTest.asMapLocation(position);
//        MapTargetData targetData = mapLocation.addTarget(target).join();
//        mapLocation.setVisited();
//
//        assertEquals(4.0d, targetData.distance());
//        assertEquals(target.getCoordinate(), targetData.getTarget());
//        assertFalse(targetData.indirect());
//
//        Segment segment = targetData.asSegment(mapLocation);
//        FrontsCoordinateTest.assertEquals(target, segment, scaleInfo);
//        assertEquals(4.0d, segment.cost(), scaleInfo.getResolution());
//        assertEquals(4.0d, segment.distance(), scaleInfo.getResolution());
//
//    }

//    @Test
//    void addCoordTest_completeWithObstacle() {
//        underTest = new MapImpl(ctxt);
//        target = makeLoc(-1, 1);
//
//        List<CompletableFuture<?>> futures = new ArrayList<>();
//        futures.add(underTest.createObstacle(new Coordinate(-1, -2)));
//
//        MapLocation mapLocation = underTest.asMapCoordinate(position);
//        futures.add(mapLocation.setVisited());
//        mapLocation.addTarget(target);
//
//        CompletableFuture<Void> future = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
//
//        await().atMost(Duration.ofSeconds(5)).until(() -> future.isDone());
//
//        System.out.println(MapReports.dumpModel(underTest.getModel()));
//
//        assertTrue(result.isPresent());
//        Segment segment = result.get();
//        assertEquals(4.0d, segment.distance());
//        assertEquals(target.getCoordinate(), segment.getCoordinate());
//        assertEquals(8.0d, segment.cost());
//
//        AskBuilder askResult = new AskBuilder().addWhere(Namespace.s, RDF.type, Namespace.Coord)
//                .addWhere(Namespace.s, Namespace.x, -1d).addWhere(Namespace.s, Namespace.y, -3d)
//                .addWhere(Namespace.s, Namespace.isIndirect, true).addWhere(Namespace.s, Namespace.distance, 4.0)
//                .addWhere(Namespace.s, Namespace.visited, true);
//
//        await().atMost(Duration.ofSeconds(5)).pollInterval(Duration.ofMillis(500))
//                .untilAsserted(() -> underTest.ask(askResult));
//    }


//    @Test
//    public void getBestTargetTest() {
//        setup();
//        TreeSet<MapCoordinate> solutions = new TreeSet<>(MapCoordinate.XYCompr);
//        solutions.add(makeLoc(2, -1));
//        solutions.add(makeLoc(-4, -1));
//        TextViz tMap = new TextViz(1, underTest, () -> solution, () -> position, () -> target);
//        System.out.println(tMap.render());
//        MapViz vMap = new MapViz(100, underTest, () -> solution, () -> position, () -> target);
//        vMap.redraw();
//        Optional<Segment> pr = underTest.getBestSegment(position);
//        assertTrue(pr.isPresent());
//        Segment segment = pr.get();
//        assertTrue(solutions.contains(segment));
//
//        // remove the 2 possible solutions.
//        solutions.forEach(underTest::setVisited);
//
//        pr = underTest.getBestSegment(position);
//        assertTrue(pr.isPresent());
//        segment = pr.get();
//        assertEquals(0, makeLoc(-1, -2).compareTo(segment.getCoordinate()));
//
//        // remove all the solutions
//        underTest.getMapLocations().join().forEach(underTest::setVisited);
//        pr = underTest.getBestSegment(position);
//        assertFalse(pr.isPresent());
//    }


//    @Test
//    public void getStepTest() throws InterruptedException {
//        setup();
//        System.out.println(MapReports.dumpModel(underTest, Namespace.PlanningModel));
//
//        Optional<Segment> pr = underTest.getStep(0.0, new Location(position)).join();
//        assertTrue(pr.isPresent());
//        assertEquals(0, FrontsCoordinate.XYCompr.compare(position, pr.get()));
//        assertEquals(position.distance(target), pr.get().distance());
//        // p can not see t so cost should be 2x distance
//        assertEquals(pr.get().distance() * 2, pr.get().cost());
//
//        pr = underTest.getStep(0.0, new Location(target)).join();
//        assertTrue(pr.isEmpty());
//
//        for (FrontsCoordinate e : coordinates) {
//            pr = underTest.getStep(0.0, new Location(e)).join();
//            assertTrue(pr.isPresent());
//            assertEquals(0, FrontsCoordinate.XYCompr.compare(e, pr.get()));
//            assertEquals(e.distance(target), pr.get().distance());
//        }
//        for (FrontsCoordinate o : obstacles) {
//            pr = underTest.getStep(0.0, new Location(o)).join();
//            assertTrue(pr.isEmpty());
//        }
//    }

//    @Test
//    public void getSegmentsTest() {
////        MapLibrary.map2(underTest);
////        solution = new Solution();
////        solution.add(underTest.asMapPosition(position));
////        MapLocation target = underTest.asMapLocation(makeLoc(-1, 1));
////        cMap = new DebugViz(1, underTest, () -> solution, () -> position, () -> target);
////
////        Arrays.stream(coordinates).forEach(coordinate -> underTest.asMapLocation(coordinate)
////                .getTargetData(target));
//        MapLibrary.map2(underTest);
//        Solution solution = new Solution();
//        solution.add(underTest.asMapCoordinate(position));
//
//        // Supplier<Position> positionSupplier = () -> Position.from( p );
//        cMap.redraw();
//        // looking from the target we should only see -4,-1 and 2,-1
//        Collection<Segment> records = underTest.getSegments(position).join();
//        cMap.redraw();
//        assertEquals(12, records.size());
//
//        MapCoordinate nxt = records.iterator().next();
//        underTest.setVisited(nxt);
//        records = underTest.getSegments(position).join();
//        cMap.redraw();
//        assertEquals(11, records.size());
//    }
//
//    @Test
//    public void getMapLocationsTest() {
//        setup();
//        List<MapCoordinate> expected = new ArrayList<>(Arrays.asList(coordinates));
//        expected.add(position);
//        expected.sort(MapCoordinate.XYCompr);
//
//        List<? extends MapCoordinate> records = new ArrayList<>(underTest.getMapLocations().join());
//        records.sort(MapCoordinate.XYCompr);
//        assertEquals(expected.size(), records.size());
//
//        for (int i = 0; i < records.size(); i++) {
//            assertEquals(0, MapCoordinate.XYCompr.compare(records.get(i), expected.get(i)));
//        }
//    }
//
//    @Test
//    public void isEmptyTest() {
//        assertTrue(new MapImpl(ctxt).isEmpty());
//    }

//    @Test
//    public void addPathTest() {
//        setup();
//        Location a = new Location(coordinates[0]);
//        Location b = new Location(coordinates[1]);
//        List<CompletableFuture<?>> futures = new ArrayList<>();
//
//        assertFalse(underTest.hasPath(a, b), "Should not have path");
//
//        Location c = makeLoc(5, 5);
//       futures.add(underTest.asMapLocation(c).addTarget(a));
//
//        futures.add(underTest.addPath(a, c));
//
//        awaitFutures(futures, Duration.ofSeconds(5));
//
//        assertTrue(underTest.hasPath(a, c), "Should have path");
//    }

//    @Test
//    public void pathTests() {
//        setup();
//        for (MapCoordinate[] l : paths) {
//            underTest.addPath(l[0], l[1]);
//        }
//        Location a = new Location(position);
//        Location b = new Location(coordinates[0]);
//        Location c = new Location(target);
//        MapLocationImpl location = underTest.asMapLocation(target);
//
//        assertTrue(underTest.hasPath(a, b));
//        assertFalse(underTest.hasPath(b, c));
//        assertFalse(underTest.hasPath(a, c));
//
//        underTest.addPath(b, c);
//
//        assertTrue(underTest.hasPath(a, b));
//        assertTrue(underTest.hasPath(b, c));
//        assertTrue(underTest.hasPath(a, c));
//    }

//    @Test
//    public void recalculateTest() {
//        setup();
//        SelectBuilder select = new SelectBuilder();
//        ExprFactory exprF = select.getExprFactory();
//        select.addVar("Count(*)", "?count").from(Namespace.UnionModel.getURI()) //
//                .addWhere(Namespace.s, RDF.type, Namespace.Coord) //
//                .addOptional(Namespace.s, Namespace.isIndirect, "?indFlg") //
//                .addBind(exprF.cond(exprF.bound("?indFlg"), exprF.asExpr("?indFlg"), exprF.asExpr(false)),
//                        "?isIndirect")
//                .addFilter(exprF.not("?isIndirect"));
//
//        SelectBuilder report = new SelectBuilder();
//        if (LOG.isDebugEnabled()) {
//            report.addVar("?wkt").from(Namespace.UnionModel.getURI()) //
//                    .addWhere(Namespace.s, RDF.type, Namespace.Coord) //
//                    .addWhere(Namespace.s, Geo.AS_WKT_PROP, "?wkt")
//                    .addOptional(Namespace.s, Namespace.isIndirect, "?indFlg") //
//                    .addBind(exprF.cond(exprF.bound("?indFlg"), exprF.asExpr("?indFlg"), exprF.asExpr(false)),
//                            "?isIndirect")
//                    .addFilter(exprF.not("?isIndirect"));
//
//            LOG.debug("\n{}", MapReports.dumpQuery(underTest, report));
//        }
//
//        int count = underTest.exec(select).thenApply(resultSet -> resultSet.next().getLiteral("count").getInt()).join();
//        ctxt.awaitQuiescence(5, SECONDS);
//        cMap.redraw();
//
//        // assertEquals(3, count, "Should have 3 direct points");
//
//        MapPosition mapPosition = underTest.asMapPosition(position);
//
//        cMap.redraw();
//
//        Map.TargetData targetData = mapPosition.addTarget(target).join();
//
//        Location c = new Location(coordinates[0]);
//
//        Segment before = underTest.getStep(0.0, c).join().orElseThrow();
//        target = makeLoc(-4, 1);
//        underTest.recalculate(target);
//
//        solution.add(target);
//        if (LOG.isDebugEnabled()) {
//            LOG.debug("\n{}", MapReports.dumpQuery(underTest, report));
//        }
//        cMap.redraw();
//
//        Segment after = underTest.getStep(0.0, c).join().orElseThrow();
//        assertNotEquals(before.cost(), after.cost());
//
//        System.out.println(MapReports.dumpModel(underTest));
//        cMap.redraw();
//
//        System.out.println(position);
//
//        // await().atMost(5, SECONDS).untilAsserted(() -> assertEquals(5,
//        // underTest.exec(select).thenApply(resultSet -> {
//        // return resultSet.next().getLiteral("count").getInt();
//        // }).join(), () -> "Should have 5 direct points"));
//    }

    // @Test
    // public void updateTest() {
    // setup();
    // Location c = makeLoc(5, 4);
    // // Resource r = Namespace.urlOf(c);
    //
    // // check not there, update then verify that it is.
    // AskBuilder ask = new AskBuilder().from(Namespace.UnionModel.getURI()) //
    // .addWhere(Namespace.s, Namespace.x, c.getX()) //
    // .addWhere(Namespace.s, Namespace.y, c.getY());
    // assertFalse(underTest.ask(ask));
    //
    // assertFalse(underTest.addCoord(c).join().isPresent());
    // assertTrue(underTest.ask(ask));
    // MapLocation mapLocation = underTest.asMapPosition(c);
    // // add the coordinate with a distance of 1.
    // MapLocation target = underTest.asMapLocation(new Position(c.getCoordinate(),
    // 0).relativeLocation(makeLoc(0, 1)));
    // mapLocation.addTarget(target);
    //
    // underTest.addCoord(c, target);
    // assertTrue(underTest.ask(ask));
    // // now update it to 5 and verify that it is there.
    // underTest.updateCoordinate(Namespace.PlanningModel, c, Namespace.distance,
    // 5).join();
    // assertTrue(underTest.ask(ask));
    //
    // ExprFactory exprF = new ExprFactory();
    //
    // ask = new AskBuilder().from(Namespace.UnionModel.getURI()) //
    // .addWhere(Namespace.s, Namespace.distance, Namespace.o) //
    // .addFilter(exprF.eq(Namespace.o, 5.0));
    // assertTrue(underTest.ask(ask));
    //
    // c = new Location(coordinates[0]);
    // assertTrue(underTest.ask(ask));
    //
    // Segment before = underTest.getStep(0.0, c).join().orElseThrow();
    // underTest.updateCoordinate(Namespace.PlanningModel, c, Namespace.distance,
    // before.distance() + 5).join();
    //
    // SelectBuilder sb = new SelectBuilder().from(Namespace.UnionModel.getURI()) //
    // .addWhere(Namespace.s, Namespace.distance, before.distance() + 5) //
    // .addWhere(Namespace.s, Namespace.x, c.getX()) //
    // .addWhere(Namespace.s, Namespace.y, c.getY());
    // int count = underTest.exec(sb).thenApply(resultSet -> {
    // int cnt = 0;
    // while (resultSet.hasNext()) {
    // cnt++;
    // resultSet.next();
    // }
    // return cnt;
    // }).join();
    // assertEquals(1, count);
    //
    // Segment after = underTest.getStep(0.0, c).join().orElseThrow();
    // assertEquals(before.distance() + 5, after.distance());
    // }

//    @Test
//    public void clearViewTest() {
//        underTest = new MapImpl(ctxt);
//        underTest.createObstacle(position, position.relativeLocation(makeLoc(-3, -3))).join();
//        MapCoordinate a = makeLoc(-3, -4);
//        MapCoordinate b = makeLoc(-3, -2);
//
//        assertFalse(underTest.isClearPath(new Location(a), new Location(b)), "Did not expect clear path");
//        b = makeLoc(-4, -4);
//        assertTrue(underTest.isClearPath(new Location(a), new Location(b)), "Expected clear path");
//    }

//    @Test
//    public void scaleCoordinatesTest() {
//        // verify inserting a node near map coord shows up at map coord
//        underTest = new MapImpl(ctxt);
//        Coordinate expected = position.getCoordinate();
//        assertEquals(expected, underTest.scaleCoordinate(position.getCoordinate()));
//
//        double incr = ctxt.scaleInfo.getResolution() / 2;
//        MapCoordinate p2 = makeLoc(position.getX() + incr, position.getY() + incr);
//        assertNotEquals(position, p2);
//        assertEquals(expected, underTest.scaleCoordinate(p2.getCoordinate()));
//    }

//    @Test
//    public void verifySingleMapLocationInstance() {
//        // verify inserting a node near map coord shows up at map coord
//        underTest = new MapImpl(ctxt);
//        MapLocationImpl location = underTest.asMapLocation(position);
//        double incr = ctxt.scaleInfo.getResolution() / 2;
//        MapCoordinate p2 = makeLoc(position.getX() + incr, position.getY() + incr);
//        assertNotEquals(position, p2);
//        assertEquals(underTest.scaleCoordinate(position.getCoordinate()), underTest.scaleCoordinate(p2.getCoordinate()));
//
//        MapLocationImpl location2 = underTest.asMapLocation(p2);
//        assertSame(location, location2);
//    }

//    @Test
//    public void createObstacleTest() {
//        underTest = new MapImpl(ctxt);
//        Location relative = makeLoc(1, 0);
//        MapObstacleImpl obst = underTest.createObstacle(position, relative).join();
//        Coordinate[] lst = obst.getGeometry().getCoordinates();
//        assertEquals(1, lst.length);
//        CoordinateUtils.assertEquivalent(makeLoc(0, -3), lst[0], ctxt.scaleInfo.getResolution());
//        assertEquals(ctxt.graphGeomFactory.asWKT(obst.getGeometry()), obst.wkt());
//    }

//    @Test
//    public void addObstacleTest() {
//        underTest = new MapImpl(ctxt);
//        Location relative = makeLoc(ctxt.scaleInfo.getResolution(), 0);
//        MapObstacleImpl obst = underTest.createObstacle(position, relative).join();
//        assertTrue(underTest.isObstacle(position.nextPosition(relative)));
//        relative = makeLoc(0, ctxt.scaleInfo.getResolution());
//        MapObstacleImpl obst2 = underTest.createObstacle(position, relative).join();
//        double halfResolution = ctxt.scaleInfo.getResolution() / 2;
//        relative = makeLoc(halfResolution, halfResolution);
//        MapObstacleImpl obst3 = underTest.createObstacle(position, relative).join();
//        assertEquals(1, underTest.getObstacles().join().size());
//    }


    @Test
    void clearTest() {
        Coordinate obst = new Coordinate(1, 1);
        Coordinate loc = new Coordinate(3, 3);
        underTest.createObstacle(obst);
        underTest.asMapLocation(Location.asLocation(loc));
        assertThat(underTest.isObstacle(underTest.asMapCoordinate(obst))).isTrue();
        assertThat(underTest.getLocations().join().collect(Collectors.toList())).size().isEqualTo(1);

        underTest.clear();
        assertThat(underTest.isObstacle(underTest.asMapCoordinate(obst))).isFalse();
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
        assertThat(mapCoordinate.getCoordinate()).isEqualTo(thetaAndRange.getCoordinate());
        assertThat(mapCoordinate.getMap()).isEqualTo(underTest);
    }
    @Test
    void asMapCoordinateCoordinateTest() {
        MapCoordinate mapCoordinate = underTest.asMapCoordinate(new Coordinate(3,3));
        assertThat(mapCoordinate.getCoordinate()).isEqualTo(new Coordinate(3,3));
        assertThat(mapCoordinate.getMap()).isEqualTo(underTest);
    }

    @Test
    void asMapCoordinateLocationTest() {
        Location location = Location.asLocation(new Coordinate(3,3));
        MapCoordinate mapCoordinate = underTest.asMapCoordinate(location);
        assertThat(mapCoordinate.getCoordinate()).isEqualTo(new Coordinate(3,3));
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
        MapPosition mapPosition = underTest.asMapPosition(Position.asPosition(new Coordinate(1,1), AngleUtils.RADIANS_45));
        assertThat(mapPosition.getCoordinate()).isEqualTo(new Coordinate(1, 1));
        assertThat(mapPosition.getHeading()).isEqualTo(AngleUtils.RADIANS_45);
        assertThat(mapPosition.getMap()).isEqualTo(underTest);
    }

    @Test
    void asMapPositionMapCoordinateHeadingTest() {
        MapPosition mapPosition = underTest.asMapPosition(underTest.asMapCoordinate(new Coordinate(1,1)), AngleUtils.RADIANS_45);
        assertThat(mapPosition.getCoordinate()).isEqualTo(new Coordinate(1, 1));
        assertThat(mapPosition.getHeading()).isEqualTo(AngleUtils.RADIANS_45);
        assertThat(mapPosition.getMap()).isEqualTo(underTest);
    }

    @Test
    void asPathTest() {
        fail("Not implemented");

//    public MapPath asPath(Collection<? extends MapLocation> points) {
//        return new MapPath(this, points.stream().map(this::asMapLocation));
    }

    @Test
    void asMapObstacleTest() {
        Geometry geometry = ctxt.geometryUtils.asPolygon(Location.ORIGIN, DoubleUtils.SQRT2, 10);
        Obstacle obstacle = Obstacle.asObstacle(UUID.randomUUID(), geometry);
        MapObstacle mapObstacle = underTest.asMapObstacle(obstacle);
        assertThat(mapObstacle.uuid()).isEqualTo(obstacle.uuid());
        assertThat(mapObstacle.getGeometry()).isEqualTo(obstacle.getGeometry());
    }

    @Test
    void createObstacleCoordinateTest() {
        Coordinate coord = new Coordinate(1,1);
        MapObstacle mapObstacle = underTest.createObstacle(coord);
        assertThat(underTest.isObstacle(underTest.asMapCoordinate(coord))).isTrue();
    }

    @Test
    void createObstacleGeometricObjectTest() {
        GeometricObject geometricObject = GeometricObject.of(ctxt.geometryUtils.asPolygon(Location.ORIGIN, DoubleUtils.SQRT2, 10));
        MapObstacle obstacle = underTest.createObstacle(geometricObject);
        for (int x = -1; x < 1; x++) {
            for (int y = -1; y < 1; y++) {
                assertThat(underTest.isObstacle(underTest.asMapCoordinate(new Coordinate(x, y)))).isTrue();
            }
        }
    }


    @Test
    void createObstacleInBackgroundGeometricObjectTest() {
        GeometricObject geometricObject = GeometricObject.of(ctxt.geometryUtils.asPolygon(Location.ORIGIN, DoubleUtils.SQRT2, 10));
        MapObstacle obstacle = underTest.createObstacleInBackground(geometricObject).join();
        for (int x = -1; x < 1; x++) {
            for (int y = -1; y < 1; y++) {
                assertThat(underTest.isObstacle(underTest.asMapCoordinate(new Coordinate(x, y)))).isTrue();
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
            assertThat(underTest.isObstacle(coord)).describedAs(coord.toString()).isFalse();
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
        List<Coordinate> coords = Arrays.asList(new Coordinate(1,1), new Coordinate(10,10));
        coords.forEach(underTest::createObstacle);

        coords.forEach(coord -> assertThat(underTest.isObstacle(underTest.asMapCoordinate(coord)))
                .describedAs("Coordinate " + coord).isTrue());

        assertThat(underTest.isObstacle(underTest.asMapCoordinate(new Coordinate(5,5)))).isFalse();
    }

    @Test
    void getObstaclesTest() {
        List<Coordinate> coords = Arrays.asList(new Coordinate(1,1), new Coordinate(10,10));
        coords.forEach(underTest::createObstacle);
        List<Geometry> expected = coords.stream().map(ctxt.geometryUtils::asPoint).collect(Collectors.toList());

        List<Geometry> actual = underTest.getObstacles().join().map(Obstacle::getGeometry).collect(Collectors.toList());
        assertThat(actual).containsExactlyElementsOf(expected);
    }


    @Test
    void getLocationsTest() {
        List<MapLocation> expected = Arrays.stream(coordinates).map(underTest::asMapLocation).collect(Collectors.toList());
        List<MapLocation> actual = underTest.getLocations().join().collect(Collectors.toList());
        assertThat(actual).containsExactlyElementsOf(expected);

        // release all the old data.
        expected.clear();
        actual.clear();

        // verify read from storage.
        List<Location> actual2 = underTest.getLocations().join().map(mapLocation -> Location.asLocation(mapLocation.getCoordinate())).toList();
        assertThat(actual2).containsExactlyElementsOf(Arrays.asList(coordinates));

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
        public  MapStorage.Reports<String> getReports() {
            throw new UnsupportedOperationException();
        }

        @Override
        public CompletableFuture<MapLocation> saveLocation(MapLocation mapLocation) {
            locations.put(mapLocation.getCoordinate(), new TestingData(mapLocation.getCoordinate(), mapLocation.getGeometry(), mapLocation.wasVisited()));
            return CompletableFuture.completedFuture(mapLocation);
        }

        @Override
        public CompletableFuture<Stream<Coordinate>> getLocations(Geometry boundingBox) {
            return CompletableFuture.completedFuture(locations.values().stream().filter(testingData -> boundingBox.covers(testingData.geometry))
                    .map(TestingData::coordinate));
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
                    geom.getGeometry().covers(geometricObject.getGeometry()) ||
                            geom.getGeometry().intersects(geometricObject.getGeometry())
            ));
        }

        @Override
        public CompletableFuture<?> removeObstacles(Stream<UUID> obstacleIds) {
            obstacleIds.forEach(obstacles::remove);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<?> addObstacle(MapObstacle mapObstacle) {
            obstacles.put(mapObstacle.uuid(), mapObstacle);
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
                    .forEach( testingData -> locations.remove(testingData.coordinate()));
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
