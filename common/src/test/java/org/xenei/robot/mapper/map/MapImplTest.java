package org.xenei.robot.mapper.map;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.arq.querybuilder.ExprFactory;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.geosparql.implementation.vocabulary.Geo;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.vocabulary.RDF;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.common.ChassisInfoTest;
import org.xenei.robot.common.FrontsCoordinate;
import org.xenei.robot.common.FrontsCoordinateTest;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.Obstacle;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.ScaleInfo;
import org.xenei.robot.common.planning.Solution;
import org.xenei.robot.common.planning.Segment;
import org.xenei.robot.common.testUtils.CoordinateUtils;
import org.xenei.robot.common.testUtils.DebugViz;
import org.xenei.robot.common.testUtils.MapLibrary;
import org.xenei.robot.common.utils.AngleUtils;
import org.xenei.robot.common.utils.CoordUtils;
import org.xenei.robot.common.utils.RobutContext;
import org.xenei.robot.mapper.MapReports;
import org.xenei.robot.mapper.rdf.Namespace;
import org.xenei.robot.mapper.visualization.MapViz;

public class MapImplTest {

    private static final Logger LOG = LoggerFactory.getLogger(MapImplTest.class);
    private static final ScaleInfo scaleInfo = ScaleInfo.DEFAULT;
    private static final RobutContext ctxt = new RobutContext(scaleInfo, ChassisInfoTest.DEFAULT);

    private MapImpl underTest;

    private static Location makeLoc(double x, double y) {
        return new Location(new Coordinate(x, y));
    }

    public static final Location[] coordinates = {makeLoc(-4, -4), makeLoc(-4, -3), makeLoc(-4, -1), makeLoc(-2, -4),
            makeLoc(-2, -2), makeLoc(-1, -4), makeLoc(-1, -2), makeLoc(0, -4), makeLoc(0, -2), makeLoc(2, -4),
            makeLoc(2, -3), makeLoc(2, -1)};

    public static final Location[] obstacles = {makeLoc(-5, -4), makeLoc(-5, -3), makeLoc(-5, -1), makeLoc(-3, -5),
            makeLoc(-3, -1), makeLoc(-2, -5), makeLoc(-2, -1), makeLoc(-1, -5), makeLoc(-1, -1), makeLoc(0, -5),
            makeLoc(0, -1), makeLoc(1, -5), makeLoc(1, -1), makeLoc(3, -4), makeLoc(3, -3), makeLoc(3, -1)};

    static List<FrontsCoordinate[]> paths = new ArrayList<>();

    static final Position position = new Position(new Coordinate(-1, -3), 0);

    static Location target = makeLoc(-1, 1);

    DebugViz cMap;

    Solution solution;

    @BeforeAll
    public static void setupPaths() {
        for (FrontsCoordinate e : coordinates) {
            paths.add(new FrontsCoordinate[]{position, e});
        }
    }

    private void setup() {
        underTest = new MapImpl(ctxt);
        MapLibrary.map2(underTest);
        solution = new Solution();
        solution.add(position);
        target = makeLoc(-1, 1);
        cMap = new DebugViz(1, underTest, () -> solution, () -> position, () -> target);

        List<CompletableFuture<?>> futures = new ArrayList<>();
        underTest.addCoord(position, target, false);
        Arrays.stream(coordinates).forEach(coordinate -> underTest.addCoord(coordinate, target, false));
        for (CompletableFuture<?> f : futures) {
            f.join();
        }
        ctxt.awaitQuiescence(1, SECONDS);
    }

    @Test
    void addCoordTest_buildup() {
        underTest = new MapImpl(ctxt);
        target = makeLoc(-1, 1);

        Optional<Segment> result = underTest.addCoord(position, null, false);
        assertFalse(result.isPresent());

        AskBuilder askResult = new AskBuilder().addWhere(Namespace.s, RDF.type, Namespace.Coord)
                .addWhere(Namespace.s, Namespace.x, -1d).addWhere(Namespace.s, Namespace.y, -3d);
        assertFalse(underTest.ask(askResult));

        AskBuilder askFalse = askResult.clone();
        askFalse.addWhere(Namespace.s, Namespace.isIndirect, Namespace.o);
        assertFalse(underTest.ask(askFalse));

        askFalse = askResult.clone();
        askFalse.addWhere(Namespace.s, Namespace.visited, Namespace.o);
        assertFalse(underTest.ask(askFalse));

        askFalse = askResult.clone();
        askFalse.addWhere(Namespace.s, Namespace.distance, Namespace.o);
        assertFalse(underTest.ask(askFalse));

        result = underTest.addCoord(position, target, false);

        assertTrue(result.isPresent());
        Segment segment = result.get();
        assertEquals(4.0d, segment.distance());
        FrontsCoordinateTest.assertEquals(target, segment, scaleInfo);
        assertEquals(4.0d, segment.cost());

        askResult.addWhere(Namespace.s, RDF.type, Namespace.Coord).addWhere(Namespace.s, Namespace.x, -1d)
                .addWhere(Namespace.s, Namespace.y, -3d).addWhere(Namespace.s, Namespace.isIndirect, false)
                .addWhere(Namespace.s, Namespace.distance, 4.0);

        await().atMost(Duration.ofSeconds(5)).pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> underTest.ask(askResult));

        askFalse = askResult.clone();
        askFalse.addWhere(Namespace.s, Namespace.visited, Namespace.o);
        assertFalse(underTest.ask(askFalse));

        result = underTest.addCoord(position, null, true);
        assertFalse(result.isPresent());

        // System.out.println(MapReports.dumpModel(underTest, Namespace.PlanningModel));
        askResult.addWhere(Namespace.s, Namespace.visited, Namespace.o);
        await().atMost(Duration.ofSeconds(5)).pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> underTest.ask(askResult));
    }

    @Test
    void addCoordTest_complete() {
        underTest = new MapImpl(ctxt);
        target = makeLoc(-1, 1);

        Optional<Segment> result = underTest.addCoord(position, target, true);
        assertTrue(result.isPresent());
        Segment segment = result.get();
        assertEquals(4.0d, segment.distance());
        FrontsCoordinateTest.assertEquals(target, segment, scaleInfo);
        assertEquals(4.0d, segment.cost());

        AskBuilder askResult = new AskBuilder().addWhere(Namespace.s, RDF.type, Namespace.Coord)
                .addWhere(Namespace.s, Namespace.x, -1d).addWhere(Namespace.s, Namespace.y, -3d)
                .addWhere(Namespace.s, Namespace.isIndirect, false).addWhere(Namespace.s, Namespace.distance, 4.0)
                .addWhere(Namespace.s, Namespace.visited, true);

        await().atMost(Duration.ofSeconds(5)).pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> underTest.ask(askResult));
    }

    @Test
    void addCoordTest_completeWithObstacle() {
        underTest = new MapImpl(ctxt);
        target = makeLoc(-1, 1);

        underTest.createObstacle(new Coordinate(-1, -2)).join();

        Optional<Segment> result = underTest.addCoord(position, target, true);
        ctxt.awaitQuiescence(5, SECONDS);

        System.out.println(MapReports.dumpModel(underTest.getModel()));
        assertTrue(result.isPresent());
        Segment segment = result.get();
        assertEquals(4.0d, segment.distance());
        assertEquals(target.getCoordinate(), segment.getCoordinate());
        assertEquals(8.0d, segment.cost());

        AskBuilder askResult = new AskBuilder().addWhere(Namespace.s, RDF.type, Namespace.Coord)
                .addWhere(Namespace.s, Namespace.x, -1d).addWhere(Namespace.s, Namespace.y, -3d)
                .addWhere(Namespace.s, Namespace.isIndirect, true).addWhere(Namespace.s, Namespace.distance, 4.0)
                .addWhere(Namespace.s, Namespace.visited, true);

        await().atMost(Duration.ofSeconds(5)).pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> underTest.ask(askResult));
    }

    @Test
    public void getBestTargetTest() {
        setup();
        TreeSet<FrontsCoordinate> solutions = new TreeSet<>(FrontsCoordinate.XYCompr);
        solutions.add(makeLoc(2, -1));
        solutions.add(makeLoc(-4, -1));
        MapViz vMap = new MapViz(100, underTest, () -> solution, () -> position, () -> target);
        vMap.redraw();
        Optional<Segment> pr = underTest.getBestSegment(position);
        assertTrue(pr.isPresent());
        Segment segment = pr.get();
        assertTrue(solutions.contains(segment));

        // remove the 2 possible solutions.
        solutions.forEach(underTest::setVisited);

        pr = underTest.getBestSegment(position);
        assertTrue(pr.isPresent());
        segment = pr.get();
        assertEquals(0, makeLoc(-1, -2).compareTo(segment.getCoordinate()));

        // remove all the solutions
        underTest.getCoords().join().forEach(underTest::setVisited);
        pr = underTest.getBestSegment(position);
        assertFalse(pr.isPresent());
    }

    /**
     * Checks that at least one of the geometries (obsts) contains the coordinate.
     *
     * @param obsts
     *            the list of geometries.
     * @param c
     *            he coorindate to contain.
     */
    static void assertCoordinateInObstacles(Collection<? extends Geometry> obsts, Coordinate c) {
        boolean found = false;
        Geometry cGeom = ctxt.geometryUtils.asPoint(c);
        for (Geometry geom : obsts) {
            if (geom.intersects(cGeom)) {
                found = true;
                break;
            }
        }
        assertTrue(found, () -> "Missing coordinate " + c);
    }

    @Test
    public void getStepTest() throws InterruptedException {
        setup();
        System.out.println(MapReports.dumpModel(underTest, Namespace.PlanningModel));

        Optional<Segment> pr = underTest.getStep(0.0, new Location(position)).join();
        assertTrue(pr.isPresent());
        assertEquals(0, FrontsCoordinate.XYCompr.compare(position, pr.get()));
        assertEquals(position.distance(target), pr.get().distance());
        // p can not see t so cost should be 2x distance
        assertEquals(pr.get().distance() * 2, pr.get().cost());

        pr = underTest.getStep(0.0, new Location(target)).join();
        assertTrue(pr.isEmpty());

        for (FrontsCoordinate e : coordinates) {
            pr = underTest.getStep(0.0, new Location(e)).join();
            assertTrue(pr.isPresent());
            assertEquals(0, FrontsCoordinate.XYCompr.compare(e, pr.get()));
            assertEquals(e.distance(target), pr.get().distance());
        }
        for (FrontsCoordinate o : obstacles) {
            pr = underTest.getStep(0.0, new Location(o)).join();
            assertTrue(pr.isEmpty());
        }
    }

    @Test
    public void getStepsTest() {
        setup();

        Solution solution = new Solution();
        solution.add(position);
        // Supplier<Position> positionSupplier = () -> Position.from( p );
        cMap.redraw();
        // looking from the target we should only see -4,-1 and 2,-1
        Collection<Segment> records = underTest.getSegments(position);
        cMap.redraw();
        assertEquals(12, records.size());

        FrontsCoordinate nxt = records.iterator().next();
        underTest.setVisited(nxt);
        records = underTest.getSegments(position);
        cMap.redraw();
        assertEquals(11, records.size());
    }

    @Test
    public void getCoordsTest() {
        setup();
        List<FrontsCoordinate> expected = new ArrayList<>(Arrays.asList(coordinates));
        expected.add(position);
        expected.sort(FrontsCoordinate.XYCompr);

        List<? extends FrontsCoordinate> records = new ArrayList<>(underTest.getCoords().join());
        records.sort(FrontsCoordinate.XYCompr);
        assertEquals(expected.size(), records.size());

        for (int i = 0; i < records.size(); i++) {
            assertEquals(0, FrontsCoordinate.XYCompr.compare(records.get(i), expected.get(i)));
        }
    }

    @Test
    public void isEmptyTest() {
        assertTrue(new MapImpl(ctxt).isEmpty());
    }

    @Test
    public void addPathTest() {
        setup();
        Location a = new Location(coordinates[0]);
        Location b = new Location(coordinates[1]);

        assertFalse(underTest.hasPath(a, b), "Should not have path");

        Location c = makeLoc(5, 5);
        underTest.addCoord(c, a, false);

        underTest.addPath(a, c);

        ctxt.awaitQuiescence(5, SECONDS);
        assertTrue(underTest.hasPath(a, c), "Should have path");
    }

    @Test
    public void hasPathTest() {
        setup();
        for (FrontsCoordinate[] l : paths) {
            underTest.addPath(l[0], l[1]);
        }
        Location a = new Location(position);
        Location b = new Location(coordinates[0]);
        Location c = new Location(target);
        underTest.addCoord(target, null, false);

        assertTrue(underTest.hasPath(a, b));
        assertFalse(underTest.hasPath(b, c));
        assertFalse(underTest.hasPath(a, c));

        underTest.addPath(b, c);

        assertTrue(underTest.hasPath(a, b));
        assertTrue(underTest.hasPath(b, c));
        // assertTrue(underTest.hasPath(a, c));// does this one actually work
    }

    @Test
    public void recalculateTest() {
        setup();
        SelectBuilder select = new SelectBuilder();
        ExprFactory exprF = select.getExprFactory();
        select.addVar("Count(*)", "?count").from(Namespace.UnionModel.getURI()) //
                .addWhere(Namespace.s, RDF.type, Namespace.Coord) //
                .addOptional(Namespace.s, Namespace.isIndirect, "?indFlg") //
                .addBind(exprF.cond(exprF.bound("?indFlg"), exprF.asExpr("?indFlg"), exprF.asExpr(false)),
                        "?isIndirect")
                .addFilter(exprF.not("?isIndirect"));

        SelectBuilder report = new SelectBuilder();
        if (LOG.isDebugEnabled()) {
            report.addVar("?wkt").from(Namespace.UnionModel.getURI()) //
                    .addWhere(Namespace.s, RDF.type, Namespace.Coord) //
                    .addWhere(Namespace.s, Geo.AS_WKT_PROP, "?wkt")
                    .addOptional(Namespace.s, Namespace.isIndirect, "?indFlg") //
                    .addBind(exprF.cond(exprF.bound("?indFlg"), exprF.asExpr("?indFlg"), exprF.asExpr(false)),
                            "?isIndirect")
                    .addFilter(exprF.not("?isIndirect"));

            LOG.debug("\n{}", MapReports.dumpQuery(underTest, report));
        }

        int count = underTest.exec(select).thenApply(resultSet -> resultSet.next().getLiteral("count").getInt()).join();
        ctxt.awaitQuiescence(5, SECONDS);
        cMap.redraw();

        // assertEquals(3, count, "Should have 3 direct points");

        cMap.redraw();

        Location c = new Location(coordinates[0]);

        Segment before = underTest.getStep(0.0, c).join().orElseThrow();
        target = makeLoc(-4, 1);
        underTest.recalculate(target);

        solution.add(target);
        if (LOG.isDebugEnabled()) {
            LOG.debug("\n{}", MapReports.dumpQuery(underTest, report));
        }
        cMap.redraw();

        Segment after = underTest.getStep(0.0, c).join().orElseThrow();
        assertNotEquals(before.cost(), after.cost());

        System.out.println(MapReports.dumpModel(underTest));
        cMap.redraw();

        System.out.println(position);

        // await().atMost(5, SECONDS).untilAsserted(() -> assertEquals(5,
        // underTest.exec(select).thenApply(resultSet -> {
        // return resultSet.next().getLiteral("count").getInt();
        // }).join(), () -> "Should have 5 direct points"));
    }

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

    @Test
    public void clearViewTest() {
        underTest = new MapImpl(ctxt);

        underTest.createObstacle(position, position.relativeLocation(makeLoc(-3, -3))).join();
        FrontsCoordinate a = makeLoc(-3, -4);
        FrontsCoordinate b = makeLoc(-3, -2);

        assertFalse(underTest.isClearPath(new Location(a), new Location(b)), "Did not expect clear path");
        b = makeLoc(-4, -4);
        assertTrue(underTest.isClearPath(new Location(a), new Location(b)), "Expected clear path");
    }

    @Test
    public void verifyMapRounding() {
        // verify inserting a node near map coord shows up at map coord
        underTest = new MapImpl(ctxt);
        Location target = position.nextPosition(makeLoc(0, 11));
        Segment step = underTest.addCoord(position, target, false).orElseThrow();
        assertEquals(11, step.cost());
        String result = MapReports.dumpModel(underTest, Namespace.PlanningModel);
        AskBuilder ask = new AskBuilder().addGraph(Namespace.PlanningModel, new WhereBuilder() //
                .addWhere(Namespace.s, Namespace.x, position.getX()) //
                .addWhere(Namespace.s, Namespace.y, position.getY()) //
                .addWhere(Namespace.s, Namespace.distance, 11.0) //
                .addWhere(Namespace.s, RDF.type, Namespace.Coord) //
                .addWhere(Namespace.s, Geo.AS_WKT_PROP,
                        ctxt.graphGeomFactory.asWKT(ctxt.geometryUtils.asPoint(position))));
        assertTrue(underTest.ask(ask));

        // create a point not "p" and within p +/- the resolution of the map.
        double incr = ctxt.scaleInfo.getResolution() / 2;
        FrontsCoordinate p2 = makeLoc(position.getX() + incr, position.getY() + incr);
        assertNotEquals(position, p2);
        step = underTest.addCoord(p2, target, false).orElseThrow();
        assertTrue(underTest.ask(ask));
        assertEquals(11, step.cost());
        SelectBuilder sb = new SelectBuilder().from(Namespace.PlanningModel).addVar("count(*)", "?x")
                .addWhere(Namespace.s, RDF.type, Namespace.Coord);
        int count = underTest.exec(sb).thenApply(resultSet -> resultSet.next().getLiteral("x").getInt()).join();
        assertEquals(1, count);
    }

    @Test
    public void testAddPath() {
        underTest = new MapImpl(ctxt);
        assertTrue(underTest.addCoord(position, target, false).isPresent());
        assertTrue(underTest.addCoord(coordinates[0], target, false).isPresent());
        underTest.addPath(position, coordinates[0]);
        ctxt.awaitQuiescence(30, TimeUnit.SECONDS);

        ExprFactory exprF = new ExprFactory(MapImpl.getPrefixMapping());
        Var wkt = Var.alloc("wkt");
        Literal pathWkt = ctxt.graphGeomFactory.asWKTString(position.getCoordinate(), coordinates[0].getCoordinate());
        AskBuilder ask = new AskBuilder().addPrefixes(MapImpl.getPrefixMapping()).from(Namespace.PlanningModel.getURI())
                .addWhere(Namespace.s, RDF.type, Namespace.Coord).addWhere(Namespace.s, Geo.AS_WKT_PROP, wkt)
                .addFilter(ctxt.graphGeomFactory.isNearby(exprF, wkt, pathWkt, ctxt.scaleInfo.getResolution()));

        assertTrue(underTest.ask(ask));
    }

    @Test
    public void createObstacleTest() {
        underTest = new MapImpl(ctxt);
        Location relative = makeLoc(1, 0);
        MapObstacle obst = underTest.createObstacle(position, relative).join();
        Coordinate[] lst = obst.getGeometry().getCoordinates();
        assertEquals(1, lst.length);
        CoordinateUtils.assertEquivalent(makeLoc(0, -3), lst[0], ctxt.scaleInfo.getResolution());
        assertEquals(ctxt.graphGeomFactory.asWKT(obst.getGeometry()), obst.wkt());
    }

    @Test
    public void addObstacleTest() {
        underTest = new MapImpl(ctxt);
        Location relative = makeLoc(ctxt.scaleInfo.getResolution(), 0);
        MapObstacle obst = underTest.createObstacle(position, relative).join();
        assertTrue(underTest.isObstacle(position.nextPosition(relative)));
        relative = makeLoc(0, ctxt.scaleInfo.getResolution());
        MapObstacle obst2 = underTest.createObstacle(position, relative).join();
        double halfResolution = ctxt.scaleInfo.getResolution() / 2;
        relative = makeLoc(halfResolution, halfResolution);
        MapObstacle obst3 = underTest.createObstacle(position, relative).join();
        assertEquals(1, underTest.getObstacles().join().size());
    }

    @Test
    public void isObstacleTest() {
        underTest = new MapImpl(ctxt);
        Location relative = makeLoc(1, 0);
        Position pos1 = position.nextPosition(relative);
        Obstacle obst = underTest.createObstacle(position, relative).join();
        assertTrue(underTest.isObstacle(pos1), "Did not find 1 " + pos1);

        relative = makeLoc(1, 1);
        Position pos2 = position.nextPosition(relative);
        Obstacle obst2 = underTest.createObstacle(position, relative).join();
        assertTrue(underTest.isObstacle(pos2), "Did not find 2 " + pos2);

        relative = new Location(CoordUtils.fromAngle(AngleUtils.RADIANS_45 / 2, 1));
        Position pos3 = position.nextPosition(relative);
        Obstacle obst3 = underTest.createObstacle(position, relative).join();
        assertTrue(underTest.isObstacle(pos3), "Did not find 3 " + pos3);

        for (Coordinate c : obst.getGeometry().getCoordinates())
            assertTrue(underTest.isObstacle(new Location(c)), "Did not find 1 coordinate " + c);
        for (Coordinate c : obst2.getGeometry().getCoordinates())
            assertTrue(underTest.isObstacle(new Location(c)), "Did not find 2 coordinate " + c);
        System.out.println(MapReports.dumpModel(underTest.getModel()));
        for (Coordinate c : obst3.getGeometry().getCoordinates())
            assertTrue(underTest.isObstacle(new Location(c)), "Did not find 3 coordinate " + c);
    }

    @Test
    public void isClearPathTest() {
        setup();
        cMap.redraw();

        assertFalse(underTest.isClearPath(new Location(position), new Location(target)));
        assertFalse(underTest.isClearPath(makeLoc(-2, -2), new Location(target)));
        assertTrue(underTest.isClearPath(makeLoc(-2, -2), new Location(position)));
        assertFalse(underTest.isClearPath(makeLoc(2, -1), makeLoc(-4, -1)));
    }

    @Test
    public void lookTest() {
        double delta = 0.0001;
        setup();
        cMap.redraw();

        Optional<FrontsCoordinate> result = underTest.look(position, 0, 250).join();
        assertTrue(result.isPresent());
        FrontsCoordinate loc = result.get();
        assertEquals(4, loc.getX(), delta);

        assertEquals(0, loc.getY(), delta);
        cMap.redraw();
        result = underTest.look(position, AngleUtils.RADIANS_45, 250).join();
        assertTrue(result.isPresent());
        loc = result.get();
        assertEquals(4, loc.getX(), delta);
        assertEquals(4, loc.getY(), delta);

        result = underTest.look(position, AngleUtils.RADIANS_90, 250).join();
        assertTrue(result.isPresent());
        loc = result.get();
        assertEquals(0, loc.getX(), delta);
        assertEquals(2, loc.getY(), delta);

        result = underTest.look(position, AngleUtils.RADIANS_135, 250).join();
        assertTrue(result.isPresent());
        loc = result.get();
        assertEquals(-2, loc.getX(), delta);
        assertEquals(2, loc.getY(), delta);

        result = underTest.look(position, AngleUtils.RADIANS_180, 250).join();
        assertTrue(result.isPresent());
        loc = result.get();
        assertEquals(-4, loc.getX(), delta);
        assertEquals(0, loc.getY(), delta);

        result = underTest.look(position, AngleUtils.RADIANS_225, 250).join();
        assertTrue(result.isPresent());
        loc = result.get();
        assertEquals(-2, loc.getX(), delta);
        assertEquals(-2, loc.getY(), delta);

        result = underTest.look(position, AngleUtils.RADIANS_270, 250).join();
        assertTrue(result.isPresent());
        loc = result.get();
        assertEquals(0, loc.getX(), delta);
        assertEquals(-2, loc.getY(), delta);

        result = underTest.look(position, AngleUtils.RADIANS_315, 250).join();
        assertTrue(result.isPresent());
        loc = result.get();
        System.out.println(loc);
        assertEquals(2, loc.getX(), delta);
        assertEquals(-2, loc.getY(), delta);
    }
}
