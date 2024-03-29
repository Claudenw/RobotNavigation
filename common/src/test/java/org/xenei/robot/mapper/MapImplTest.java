package org.xenei.robot.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.arq.querybuilder.ExprFactory;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.geosparql.implementation.vocabulary.Geo;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.vocabulary.RDF;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.ScaleInfo;
import org.xenei.robot.common.mapping.MapCoord;
import org.xenei.robot.common.mapping.Obstacle;
import org.xenei.robot.common.planning.Solution;
import org.xenei.robot.common.planning.Step;
import org.xenei.robot.common.testUtils.CoordinateUtils;
import org.xenei.robot.common.testUtils.DebugViz;
import org.xenei.robot.common.testUtils.MapLibrary;
import org.xenei.robot.common.testUtils.TestChassisInfo;
import org.xenei.robot.common.utils.AngleUtils;
import org.xenei.robot.common.utils.CoordUtils;
import org.xenei.robot.common.utils.RobutContext;
import org.xenei.robot.mapper.rdf.Namespace;

public class MapImplTest {

    private static final Logger LOG = LoggerFactory.getLogger(MapImplTest.class);

    private static RobutContext ctxt = new RobutContext(ScaleInfo.DEFAULT, TestChassisInfo.DEFAULT);

    private MapImpl underTest;

    public static final Coordinate[] coordinates = { new Coordinate(-4, -4), new Coordinate(-4, -3),
            new Coordinate(-4, -1), new Coordinate(-2, -4), new Coordinate(-2, -2), new Coordinate(-1, -4),
            new Coordinate(-1, -2), new Coordinate(0, -4), new Coordinate(0, -2), new Coordinate(2, -4),
            new Coordinate(2, -3), new Coordinate(2, -1) };

    public static final Coordinate[] obstacles = { new Coordinate(-5, -4), new Coordinate(-5, -3),
            new Coordinate(-5, -1), new Coordinate(-3, -5), new Coordinate(-3, -1), new Coordinate(-2, -5),
            new Coordinate(-2, -1), new Coordinate(-1, -5), new Coordinate(-1, -1), new Coordinate(0, -5),
            new Coordinate(0, -1), new Coordinate(1, -5), new Coordinate(1, -1), new Coordinate(3, -4),
            new Coordinate(3, -3), new Coordinate(3, -1) };

    static List<Coordinate[]> paths = new ArrayList<>();

    static final Coordinate p = new Coordinate(-1, -3);

    static final Coordinate t = new Coordinate(-1, 1);

    DebugViz cMap;

    Solution solution;

    public static List<Coordinate> obstacleList() {
        return Arrays.asList(obstacles);
    }

    @BeforeAll
    public static void setupPaths() {
        for (Coordinate e : coordinates) {
            paths.add(new Coordinate[] { p, e });
        }
    }

    private void setup() {
        underTest = new MapImpl(ctxt);
        MapLibrary.map2(underTest);
        solution = new Solution();
        solution.add(p);
        cMap = new DebugViz(.5, underTest, () -> solution, () -> Position.from(p));

        underTest.addCoord(p, p.distance(t), false, underTest.isClearPath(p, t));
        Arrays.stream(coordinates)
                .forEach(c -> underTest.addCoord(c, c.distance(t), false, !underTest.isClearPath(c, t)));
    }

    @Test
    public void getBestTargetTest() {
        setup();
        List<Coordinate> solutions = List.of(new Coordinate(2, -1), new Coordinate(-4, -1));
        Optional<Step> pr = underTest.getBestStep(p);
        assertTrue(pr.isPresent());
        Step step = pr.get();
        assertTrue(solutions.contains(step.getCoordinate()));

        // remove the 2 possible solutions.
        solutions.forEach(c -> underTest.setVisited(t, c));

        pr = underTest.getBestStep(p);
        assertTrue(pr.isPresent());
        step = pr.get();
        assertEquals(new Coordinate(-1, -2), step.getCoordinate());

        // remove all the solutions
        underTest.getCoords().forEach(c -> underTest.setVisited(t, c.location.getCoordinate()));
        pr = underTest.getBestStep(p);
        assertFalse(pr.isPresent());
    }

    /**
     * Checks that at least oneof the geometries (obsts) contains the coordinate.
     * 
     * @param obsts the list of geometries.
     * @param c he coorindate to contain.
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
    public void getStepTest() {
        setup();
        Optional<Step> pr = underTest.getStep(0.0, Location.from(p));
        assertTrue(pr.isPresent());
        assertEquals(0, CoordUtils.XYCompr.compare(p, pr.get().getCoordinate()));
        assertEquals(p.distance(t), pr.get().distance());
        // p can not see t so cost should be 2x distance
        assertEquals(pr.get().distance() * 2, pr.get().cost());

        pr = underTest.getStep(0.0, Location.from(t));
        assertTrue(pr.isEmpty());

        for (Coordinate e : coordinates) {
            pr = underTest.getStep(0.0, Location.from(e));
            assertTrue(pr.isPresent());
            assertEquals(0, CoordUtils.XYCompr.compare(e, pr.get().getCoordinate()));
            assertEquals(e.distance(t), pr.get().distance());
        }
        for (Coordinate o : obstacles) {
            pr = underTest.getStep(0.0, Location.from(o));
            assertTrue(pr.isEmpty());
        }
    }

    @Test
    public void getStepsTest() {
        setup();

        Solution solution = new Solution();
        solution.add(p);
        // Supplier<Position> positionSupplier = () -> Position.from( p );
        cMap.redraw(t);
        // looking from the target we should only see -4,-1 and 2,-1
        Collection<Step> records = underTest.getSteps(p);
        cMap.redraw(t);
        assertEquals(12, records.size());

        Coordinate nxt = records.iterator().next().getCoordinate();
        underTest.setVisited(t, nxt);
        records = underTest.getSteps(p);
        cMap.redraw(t);
        assertEquals(11, records.size());
    }

    @Test
    public void getCoordsTest() {
        setup();
        Collection<Coordinate> expected = new ArrayList<>();
        expected.addAll(Arrays.asList(coordinates));
        expected.add(p);

        Collection<MapCoord> records = underTest.getCoords();
        assertEquals(expected.size(), records.size());

        for (MapCoord pr : records) {
            assertTrue(expected.contains(pr.location.getCoordinate()), () -> "Unexpected Target " + pr);
        }
    }

    @Test
    public void isEmptyTest() {
        assertTrue(new MapImpl(ctxt).isEmpty());
    }

    @Test
    public void addPathTest() {
        setup();
        Location a = Location.from(coordinates[0]);
        Location b = Location.from(coordinates[1]);

        assertFalse(underTest.hasPath(a, b), () -> "Should not have path");

        Location c = Location.from(5, 5);
        underTest.addCoord(c.getCoordinate(), c.distance(a), false, false);

        underTest.addPath(a.getCoordinate(), c.getCoordinate());

        assertTrue(underTest.hasPath(a, c), () -> "Should have path");
    }

    @Test
    public void hasPathTest() {
        setup();
        for (Coordinate[] l : paths) {
            underTest.addPath(l[0], l[1]);
        }
        Location a = Location.from(p);
        Location b = Location.from(coordinates[0]);
        Location c = Location.from(t);
        underTest.addCoord(t, 0.0, false, false);

        assertTrue(underTest.hasPath(a, b));
        assertFalse(underTest.hasPath(b, c));
        assertFalse(underTest.hasPath(a, c));

        underTest.addPath(b.getCoordinate(), c.getCoordinate());

        assertTrue(underTest.hasPath(a, b));
        assertTrue(underTest.hasPath(b, c));
        // assertTrue(underTest.hasPath(a, c));
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

        int count[] = { 0 };
        Predicate<QuerySolution> pred = soln -> {
            count[0] = soln.getLiteral("count").getInt();
            return false;
        };
        underTest.exec(select, pred);
        assertEquals(3, count[0], () -> "Should have 3 direct points");

        cMap.redraw(t);

        Location c = Location.from(coordinates[0]);

        Step before = underTest.getStep(0.0, c).get();
        Location newTarget = Location.from(-4, 1);
        underTest.recalculate(newTarget.getCoordinate());

        solution.add(newTarget);
        if (LOG.isDebugEnabled()) {
            LOG.debug("\n{}", MapReports.dumpQuery(underTest, report));
        }
        cMap.redraw(newTarget.getCoordinate());

        Step after = underTest.getStep(0.0, c).get();
        assertNotEquals(before.cost(), after.cost());

        count[0] = 0;
        underTest.exec(select, pred);

        assertEquals(5, count[0], () -> "Should have 5 direct points");
    }

    @Test
    public void updateTest() {
        setup();
        Location c = Location.from(5, 4);
        // Resource r = Namespace.urlOf(c);

        // check not there, update then verify that it is.
        AskBuilder ask = new AskBuilder().from(Namespace.UnionModel.getURI()) //
                .addWhere(Namespace.s, Namespace.distance, 5) //
                .addWhere(Namespace.s, Namespace.x, c.getX()) //
                .addWhere(Namespace.s, Namespace.y, c.getY());
        assertFalse(underTest.ask(ask));

        // no coordinate so update should not do anything.
        underTest.updateCoordinate(Namespace.PlanningModel, c.getCoordinate(), Namespace.distance, 5);
        assertFalse(underTest.ask(ask));

        // add the coordinate with a distance of 1.
        underTest.addCoord(c.getCoordinate(), 1.0, false, false);
        assertFalse(underTest.ask(ask));
        // now update it to 5 and verify that it is there.
        underTest.updateCoordinate(Namespace.PlanningModel, c.getCoordinate(), Namespace.distance, 5);
        assertTrue(underTest.ask(ask));

        ExprFactory exprF = new ExprFactory();

        ask = new AskBuilder().from(Namespace.UnionModel.getURI()) //
                .addWhere(Namespace.s, Namespace.distance, Namespace.o) //
                .addFilter(exprF.eq(Namespace.o, 5.0));
        assertTrue(underTest.ask(ask));

        c = Location.from(coordinates[0]);
        assertTrue(underTest.ask(ask));

        Step before = underTest.getStep(0.0, c).get();
        underTest.updateCoordinate(Namespace.PlanningModel, c.getCoordinate(), Namespace.distance,
                before.distance() + 5);

        SelectBuilder sb = new SelectBuilder().from(Namespace.UnionModel.getURI()) //
                .addWhere(Namespace.s, Namespace.distance, before.distance() + 5) //
                .addWhere(Namespace.s, Namespace.x, c.getX()) //
                .addWhere(Namespace.s, Namespace.y, c.getY());
        int count[] = { 0 };
        underTest.exec(sb, (q) -> {
            count[0]++;
            return true;
        });
        assertEquals(1, count[0]);

        Step after = underTest.getStep(0.0, c).get();
        assertEquals(before.distance() + 5, after.distance());
    }

    @Test
    public void clearViewTest() {
        underTest = new MapImpl(ctxt);
        Position pos = Position.from(p, 0);

        underTest.addObstacle(underTest.createObstacle(pos, pos.relativeLocation(new Coordinate(-3, -3))));
        Coordinate a = new Coordinate(-3, -4);
        Coordinate b = new Coordinate(-3, -2);

        assertFalse(underTest.isClearPath(a, b));
        b = new Coordinate(-4, -4);
        assertTrue(underTest.isClearPath(a, b));
    }

    @Test
    public void testAddTarget() {
        underTest = new MapImpl(ctxt);
        Step step = underTest.addCoord(p, 11.0, false, false).get();
        assertEquals(11, step.cost());

        AskBuilder ask = new AskBuilder().addGraph(Namespace.PlanningModel, new WhereBuilder() //
                .addWhere(Namespace.s, Namespace.x, step.getX()) //
                .addWhere(Namespace.s, Namespace.y, step.getY()) //
                .addWhere(Namespace.s, Namespace.distance, 11.0) //
                .addWhere(Namespace.s, RDF.type, Namespace.Coord) //
                .addWhere(Namespace.s, Geo.AS_WKT_PROP, ctxt.graphGeomFactory.asWKT(ctxt.geometryUtils.asPoint(p))));
        assertTrue(underTest.ask(ask));

        // verify inserting a node near map coord shows up at map coord
        underTest = new MapImpl(ctxt);
        double incr = ctxt.scaleInfo.getHalfResolution();
        Coordinate c = new Coordinate(p.getX() + incr, p.getY() + incr);
        // -3 + (scale.getResolution() / 2) + (scale.getResolution() / 10));
        step = underTest.addCoord(c, 11.0, false, false).get();
        assertTrue(underTest.ask(ask));
        assertEquals(11, step.cost());
    }

    @Test
    public void testAddPath() {
        underTest = new MapImpl(ctxt);
        assertTrue(underTest.addCoord(p, p.distance(t), false, false).isPresent());
        assertTrue(underTest.addCoord(coordinates[0], coordinates[0].distance(t), false, false).isPresent());
        Coordinate[] path = underTest.addPath(p, coordinates[0]);

        ExprFactory exprF = new ExprFactory(MapImpl.getPrefixMapping());
        Var wkt = Var.alloc("wkt");
        Literal pathWkt = ctxt.graphGeomFactory.asWKTString(path);
        AskBuilder ask = new AskBuilder().addPrefixes(MapImpl.getPrefixMapping()).from(Namespace.PlanningModel.getURI())
                .addWhere(Namespace.s, RDF.type, Namespace.Coord).addWhere(Namespace.s, Geo.AS_WKT_PROP, wkt)
                .addFilter(ctxt.graphGeomFactory.isNearby(exprF, wkt, pathWkt, ctxt.scaleInfo.getResolution()));

        assertTrue(underTest.ask(ask));
    }

    @Test
    public void createObstacleTest() {
        underTest = new MapImpl(ctxt);
        Position pos = Position.from(p, 0);
        Location relative = Location.from(1, 0);
        Obstacle obst = underTest.createObstacle(pos, relative);
        Coordinate[] lst = obst.geom().getCoordinates();
        assertEquals(1, lst.length);
        CoordinateUtils.assertEquivalent(new Coordinate(0, -3), lst[0], ctxt.scaleInfo.getResolution());
        assertEquals(ctxt.graphGeomFactory.asWKT(obst.geom()), obst.wkt());
    }

    @Test
    public void addObstacleTest() {
        underTest = new MapImpl(ctxt);
        Position pos = Position.from(p, 0);
        Location relative = Location.from(ctxt.scaleInfo.getResolution(), 0);
        Obstacle obst = underTest.createObstacle(pos, relative);
        Set<Obstacle> result = underTest.addObstacle(obst);
        assertEquals(1, result.size());
        assertEquals(obst, result.iterator().next());

        relative = Location.from(0, ctxt.scaleInfo.getResolution());
        Obstacle obst2 = underTest.createObstacle(pos, relative);
        result = underTest.addObstacle(obst2);
        relative = Location.from(ctxt.scaleInfo.getHalfResolution(), ctxt.scaleInfo.getHalfResolution());
        Obstacle obst3 = underTest.createObstacle(pos, relative);
        result = underTest.addObstacle(obst3);

        assertEquals(1, underTest.getObstacles().size());
    }

    @Test
    public void isObstacleTest() {
        underTest = new MapImpl(ctxt);
        Position pos = Position.from(p, 0);
        Location relative = Location.from(1, 0);
        Obstacle obst = underTest.createObstacle(pos, relative);
        underTest.addObstacle(obst);
        relative = Location.from(1, 1);
        Obstacle obst2 = underTest.createObstacle(pos, relative);
        underTest.addObstacle(obst2);
        relative = Location.from(CoordUtils.fromAngle(AngleUtils.RADIANS_45 / 2, 1));
        Obstacle obst3 = underTest.createObstacle(pos, relative);
        underTest.addObstacle(obst3);
        for (Coordinate c : obst.geom().getCoordinates())
            assertTrue(underTest.isObstacle(c), () -> "Did not find c");
        for (Coordinate c : obst2.geom().getCoordinates())
            assertTrue(underTest.isObstacle(c), () -> "Did not find c");
        for (Coordinate c : obst3.geom().getCoordinates())
            assertTrue(underTest.isObstacle(c), () -> "Did not find c");
    }

    @Test
    public void isClearPathTest() {
        setup();
        cMap.redraw(t);

        assertFalse(underTest.isClearPath(p, t));
        assertFalse(underTest.isClearPath(new Coordinate(-2, -2), t));
        assertTrue(underTest.isClearPath(new Coordinate(-2, -2), p));
        assertFalse(underTest.isClearPath(new Coordinate(2, -1), new Coordinate(-4, -1)));
    }

    @Test
    public void lookTest() {
        double delta = 0.0001;
        setup();
        cMap.redraw(t);
        Position pos = Position.from(p);

        Optional<Location> result = underTest.look(pos, 0, 250);
        assertTrue(result.isPresent());
        Location loc = result.get();
        assertEquals(4, loc.getX(), delta);

        assertEquals(0, loc.getY(), delta);
        cMap.redraw(t);
        result = underTest.look(pos, AngleUtils.RADIANS_45, 250);
        assertTrue(result.isPresent());
        loc = result.get();
        assertEquals(4, loc.getX(), delta);
        assertEquals(4, loc.getY(), delta);

        result = underTest.look(pos, AngleUtils.RADIANS_90, 250);
        assertTrue(result.isPresent());
        loc = result.get();
        assertEquals(0, loc.getX(), delta);
        assertEquals(2, loc.getY(), delta);

        result = underTest.look(pos, AngleUtils.RADIANS_135, 250);
        assertTrue(result.isPresent());
        loc = result.get();
        assertEquals(-2, loc.getX(), delta);
        assertEquals(2, loc.getY(), delta);

        result = underTest.look(pos, AngleUtils.RADIANS_180, 250);
        assertTrue(result.isPresent());
        loc = result.get();
        assertEquals(-4, loc.getX(), delta);
        assertEquals(0, loc.getY(), delta);

        result = underTest.look(pos, AngleUtils.RADIANS_225, 250);
        assertTrue(result.isPresent());
        loc = result.get();
        assertEquals(-2, loc.getX(), delta);
        assertEquals(-2, loc.getY(), delta);

        result = underTest.look(pos, AngleUtils.RADIANS_270, 250);
        assertTrue(result.isPresent());
        loc = result.get();
        assertEquals(0, loc.getX(), delta);
        assertEquals(-2, loc.getY(), delta);

        result = underTest.look(pos, AngleUtils.RADIANS_315, 250);
        assertTrue(result.isPresent());
        loc = result.get();
        System.out.println(loc);
        assertEquals(2, loc.getX(), delta);
        assertEquals(-2, loc.getY(), delta);
    }
}
