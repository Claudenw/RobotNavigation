package org.xenei.robot.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;

import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.arq.querybuilder.ExprFactory;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.Resource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.planning.Step;
import org.xenei.robot.common.utils.CoordUtils;
import org.xenei.robot.common.utils.GeometryUtils;
import org.xenei.robot.mapper.rdf.Namespace;

public class MapImplTest {

    private MapImpl underTest;

    public static final Coordinate[] expected = { new Coordinate(-4, -4), new Coordinate(-4, -3),
            new Coordinate(-4, -1), new Coordinate(-2, -4), new Coordinate(-2, -2), new Coordinate(-1, -4),
            new Coordinate(-1, -2), new Coordinate(0, -4), new Coordinate(0, -2), new Coordinate(2, -4),
            new Coordinate(2, -3), new Coordinate(2, -1) };

    public static final Coordinate[] obstacles = { new Coordinate(-5, -4), new Coordinate(-5, -3),
            new Coordinate(-5, -1), new Coordinate(-3, -5), new Coordinate(-3, -1), new Coordinate(-2, -5),
            new Coordinate(-2, -1), new Coordinate(-1, -5), new Coordinate(-1, -1), new Coordinate(0, -5),
            new Coordinate(0, -1), new Coordinate(1, -5), new Coordinate(1, -1), new Coordinate(3, -4),
            new Coordinate(3, -3), new Coordinate(3, -1) };

    static List<Coordinate[]> paths = new ArrayList<>();

    static Coordinate p = new Coordinate(-1, -3);

    static Coordinate t = new Coordinate(-1, 1);

    public static List<Coordinate> obstacleList() {
        return Arrays.asList(obstacles);
    }

    @BeforeAll
    public static void setupPaths() {
        for (Coordinate e : expected) {
            paths.add(new Coordinate[] { p, e });
        }
    }

    @BeforeEach
    public void setup() {
        underTest = new MapImpl(.1);
        underTest.addTarget(new Step(p, p.distance(t), null));
        for (Coordinate e : expected) {
            underTest.addTarget(new Step(e, e.distance(t), null));
        }
        for (Coordinate o : obstacles) {
            underTest.addObstacle(o);
        }
        for (Coordinate[] l : paths) {
            underTest.addPath(l[0], l[1]);
        }
    }

    @Test
    public void isObstacleTest() {
        for (Coordinate o : obstacles) {
            assertTrue(underTest.isObstacle(o), () -> "Missing " + o);
        }
        for (Coordinate e : expected) {
            assertFalse(underTest.isObstacle(e), () -> "Should not have " + e);
        }
    }

    @Test
    public void getBestTest() {
        Optional<Step> pr = underTest.getBestTarget(p);
        assertTrue(pr.isPresent());
        Coordinate p2 = new Coordinate(-1, -2);
        assertEquals(new Step(p2, 4, null), pr.get());
    }
    
    /**
     * Checks that at least oneof the geometries (obsts) contains the coordinate.
     * @param obsts the list of geometries.
     * @param c he coorindate to contain.
     */
    public static void assertCoordinateInObstacles(Collection<? extends Geometry> obsts, Coordinate c) {
        boolean found = false;
        Point p = GeometryUtils.asPoint(c);
        for (Geometry geom : obsts) {
            
            if (geom.contains(p)) {
                found = true;
                break;
            }
        }
        assertTrue(found, () -> "Missing coordinate " + c);
    }

    @Test
    public void getObstaclesTest() {

        Set<Geometry> obsts = underTest.getObstacles();
        assertEquals(obstacles.length, obsts.size());
        for (Coordinate o : obstacles) {
            assertCoordinateInObstacles(obsts, o);
        }
    }

    @Test
    public void getStepTest() {
        Optional<Step> pr = underTest.getStep(new Location(p));
        assertTrue(pr.isPresent());
        assertEquals(0, CoordUtils.XYCompr.compare(p, pr.get().getCoordinate()));
        assertEquals(p.distance(t), pr.get().cost());

        pr = underTest.getStep(new Location(t));
        assertTrue(pr.isEmpty());

        for (Coordinate e : expected) {
            pr = underTest.getStep(new Location(e));
            assertTrue(pr.isPresent());
            assertEquals(0, CoordUtils.XYCompr.compare(e, pr.get().getCoordinate()));
            assertEquals(e.distance(t), pr.get().cost());
        }
        for (Coordinate o : obstacles) {
            pr = underTest.getStep(new Location(o));
            assertTrue(pr.isEmpty());
        }
    }

    @Test
    public void getTargetsTest() {
        Collection<Coordinate> points = new ArrayList<>();
        points.addAll(Arrays.asList(expected));
        points.add(p);
        BiPredicate<Collection<Coordinate>, Step> contains = (c, t) -> c.stream()
                .filter(p -> CoordUtils.XYCompr.compare(p, t.getCoordinate()) == 0).findFirst().isPresent();

        Collection<Step> records = underTest.getTargets();
        assertEquals(points.size(), records.size());

        for (Step pr : records) {
            assertTrue(contains.test(points, pr), () -> "Unexpected Target " + pr);
            assertFalse(Double.isNaN(pr.cost()), () -> pr.toString() + " has NaN cost");
        }
    }

    @Test
    public void isEmptyTest() {
        assertTrue(new MapImpl(1).isEmpty());
        assertFalse(underTest.isEmpty());
    }

    @Test
    public void addPathTest() {
        Location a = new Location(expected[0]);
        Location b = new Location(expected[1]);
        
        assertFalse(underTest.hasPath(a, b), () -> "Should not have path");

        Location c = new Location(5, 5);
        underTest.addTarget(new Step(c, c.distance(a), null));

        underTest.addPath(a.getCoordinate(), c.getCoordinate());

        assertTrue(underTest.hasPath(a, c), () -> "Should have path");
    }

    @Test
    public void hasPathTest() {
        Location a = new Location(p);
        Location b = new Location(expected[0]);
        Location c = new Location(t);

        assertTrue(underTest.hasPath(a, b));
        assertFalse(underTest.hasPath(b, c));
        assertFalse(underTest.hasPath(a, c));

        underTest.addPath(b.getCoordinate(), c.getCoordinate());

        assertTrue(underTest.hasPath(a, b));
        assertTrue(underTest.hasPath(b, c));
        // FIXME assertTrue(underTest.hasPath(a, c));
    }

    @Test
    public void recalculateTest() {
        Location c = new Location(expected[0]);
        Step before = underTest.getStep(c).get();

        Location newTarget = new Location(-2, 2);

        underTest.recalculate(newTarget.getCoordinate());
        Step after = underTest.getStep(c).get();
        assertNotEquals(before.cost(), after.cost());
    }

    @Test
    public void updateTest() {
        Location c = new Location(5, 5);
        Resource r = Namespace.urlOf(c);

        // check not there, update then verify that it is.
        AskBuilder ask = new AskBuilder().from(Namespace.UnionModel.getURI()).addWhere(r, Namespace.distance, null);
        assertFalse(underTest.ask(ask));
  
        underTest.update(Namespace.PlanningModel, c.getCoordinate(), Namespace.distance, 5);

        assertTrue(underTest.ask(ask));

        ExprFactory exprF = new ExprFactory();

        ask = new AskBuilder().from(Namespace.UnionModel.getURI()).addWhere(r, Namespace.distance, "?o")
                .addFilter(exprF.eq("?o", 5.0));
        assertTrue(underTest.ask(ask));


        c = new Location(expected[0]);
        assertTrue(underTest.ask(ask));

        Step before = underTest.getStep(c).get();
        underTest.update(Namespace.PlanningModel, c.getCoordinate(), Namespace.distance, before.cost() + 5);

        SelectBuilder sb = new SelectBuilder().from(Namespace.UnionModel.getURI()).addWhere(Namespace.urlOf(c),
                Namespace.distance, "?x");
        int count[] = { 0 };
        underTest.exec(sb, (q) -> count[0]++);
        assertEquals(1, count[0]);
        
        Step after = underTest.getStep(c).get();
        assertEquals(before.cost() + 5, after.cost());
    }

    @Test
    public void clearViewTest() {
        underTest.addObstacle(new Coordinate(-3, -3));
        Coordinate a = new Coordinate(-3, -4);
        Coordinate b = new Coordinate(-3, -2);

        assertFalse(underTest.clearView(a, b));
        b = new Coordinate(-4, -4);
        assertTrue(underTest.clearView(a, b));
    }
}
