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
import org.xenei.robot.common.Coordinates;
import org.xenei.robot.common.planning.Target;
import org.xenei.robot.common.utils.PointUtils;
import org.xenei.robot.mapper.rdf.Namespace;

import mil.nga.sf.Point;

public class MapImplTest {

    MapImpl underTest;

    public static final Point[] expected = { new Point(-4, -4), new Point(-4, -3), new Point(-4, -1), new Point(-2, -4),
            new Point(-2, -2), new Point(-1, -4), new Point(-1, -2), new Point(0, -4), new Point(0, -2),
            new Point(2, -4), new Point(2, -3), new Point(2, -1) };

    public static final Point[] obstacles = { new Point(-5, -4), new Point(-5, -3), new Point(-5, -1),
            new Point(-3, -5), new Point(-3, -1), new Point(-2, -5), new Point(-2, -1), new Point(-1, -5),
            new Point(-1, -1), new Point(0, -5), new Point(0, -1), new Point(1, -5), new Point(1, -1), new Point(3, -4),
            new Point(3, -3), new Point(3, -1) };

    static List<Point[]> paths = new ArrayList<>();

    static Point p = new Point(-1, -3);

    static Point t = new Point(-1, 1);
    
    public static List<Point> obstacleList() {
        return Arrays.asList(obstacles);
    }

    @BeforeAll
    public static void setupPaths() {
        for (Point e : expected) {
            paths.add(new Point[] { p, e });
        }
    }

    @BeforeEach
    public void setup() {
        underTest = new MapImpl();
        underTest.add(new Target(Coordinates.fromXY(p), PointUtils.distance(p,t)));
        for (Point e : expected) {
            underTest.add(new Target(Coordinates.fromXY(e), PointUtils.distance(e,t)));
        }
        for (Point o : obstacles) {
            underTest.setObstacle(Coordinates.fromXY(o));
        }
        for (Point[] l : paths) {
            underTest.path(Coordinates.fromXY(l[0]), Coordinates.fromXY(l[1]));
        }
    }

    @Test
    public void isObstacalTest() {
        for (Point o : obstacles) {
            assertTrue(underTest.isObstacle(Coordinates.fromXY(o)), () -> "Missing " + o);
        }
        for (Point e : expected) {
            assertFalse(underTest.isObstacle(Coordinates.fromXY(e)), () -> "Should not have " + e);
        }
    }

    @Test
    public void cutPathTest() {
        Coordinates a = Coordinates.fromXY(expected[0]);
        Resource rA = Namespace.asRDF(a, Namespace.Coord);
        Coordinates b = Coordinates.fromXY(p);
        Resource rB = Namespace.asRDF(b, Namespace.Coord);

        AskBuilder ask = new AskBuilder().addWhere(rA, Namespace.path, rB);
        try (QueryExecution exec = QueryExecutionFactory.create(ask.build(), underTest.getModel())) {
            assertTrue(exec.execAsk());
        }
        underTest.cutPath(a, b);
        try (QueryExecution exec = QueryExecutionFactory.create(ask.build(), underTest.getModel())) {
            assertFalse(exec.execAsk());
        }
    }

    @Test
    public void getBestTest() {
        Optional<Target> pr = underTest.getBestTarget(Coordinates.fromXY(p));
        assertTrue(pr.isPresent());
        Point p2 = new Point(-1, -2);
        assertEquals(new Target(p2, PointUtils.distance(p2, t)), pr.get());
        assertFalse(Double.isNaN(pr.get().cost()));
    }

    @Test
    public void getObstaclesTest() {
        Set<Coordinates> obsts = underTest.getObstacles();
        assertEquals(obstacles.length, obsts.size());
        for (Point o : obstacles) {
            assertTrue(obsts.contains(Coordinates.fromXY(o)), () -> "Missing obstacle " + o);
        }
    }

    @Test
    public void getTargetTest() {

        Optional<Target> pr = underTest.getTarget(Coordinates.fromXY(p));
        assertTrue(pr.isPresent());
        assertEquals( 0, PointUtils.XYCompr.compare( p, pr.get()));
        assertEquals(PointUtils.distance(p, t), pr.get().cost());

        pr = underTest.getTarget(Coordinates.fromXY(t));
        assertTrue(pr.isEmpty());

        for (Point e : expected) {
            pr = underTest.getTarget(Coordinates.fromXY(e));
            assertTrue(pr.isPresent());
            assertEquals( 0, PointUtils.XYCompr.compare( e, pr.get()));
            assertEquals(PointUtils.distance(e, t), pr.get().cost());
        }
        for (Point o : obstacles) {
            pr = underTest.getTarget(Coordinates.fromXY(o));
            assertTrue(pr.isEmpty());
        }
    }

    @Test
    public void getTargetsTest() {
        Collection<Point> points = new ArrayList<>();
        points.addAll(Arrays.asList(expected));
        points.add(p);
        BiPredicate<Collection<Point>, Target> contains = (c,t) -> c.stream().filter( p->PointUtils.XYCompr.compare(p, t) == 0).findFirst().isPresent();


        Collection<Target> records = underTest.getTargets();
        assertEquals(points.size(), records.size());

        for (Target pr : records) {
            assertTrue(contains.test(points, pr), () -> "Unexpected Target " + pr);
            assertFalse(Double.isNaN(pr.cost()), () -> pr.toString() + " has NaN cost");
        }
    }

    @Test
    public void isEmptyTest() {
        assertTrue(new MapImpl().isEmpty());
        assertFalse(underTest.isEmpty());
    }

    @Test
    public void pathTest() {
        Coordinates a = Coordinates.fromXY(expected[0]);
        Resource rA = Namespace.asRDF(a, Namespace.Coord);
        Coordinates b = Coordinates.fromXY(p);
        Resource rB = Namespace.asRDF(b, Namespace.Coord);

        AskBuilder ask = new AskBuilder().addWhere(rA, Namespace.path, rB);
        try (QueryExecution exec = QueryExecutionFactory.create(ask.build(), underTest.getModel())) {
            assertTrue(exec.execAsk());
        }

        Coordinates c = Coordinates.fromXY(5, 5);
        underTest.add(new Target(c, c.distanceTo(a)));
        Resource rC = Namespace.asRDF(c, Namespace.Coord);
        ask = new AskBuilder().addWhere(rA, Namespace.path, rC);
        try (QueryExecution exec = QueryExecutionFactory.create(ask.build(), underTest.getModel())) {
            assertFalse(exec.execAsk());
        }
        underTest.path(a, c);
        try (QueryExecution exec = QueryExecutionFactory.create(ask.build(), underTest.getModel())) {
            assertTrue(exec.execAsk());
        }
    }

    @Test
    public void hasPathTest() {
        Coordinates a = Coordinates.fromXY(p);
        Coordinates b = Coordinates.fromXY(expected[0]);
        Coordinates c = Coordinates.fromXY(t);

        assertTrue(underTest.hasPath(a, b));
        assertFalse(underTest.hasPath(b, c));
        assertFalse(underTest.hasPath(a, c));

        underTest.path(b, c);

        assertTrue(underTest.hasPath(a, b));
        assertTrue(underTest.hasPath(b, c));
        assertTrue(underTest.hasPath(a, c));
    }

    @Test
    public void resetTest() {
        Coordinates c = Coordinates.fromXY(expected[0]);
        Target before = underTest.getTarget(c).get();

        Coordinates newTarget = Coordinates.fromXY(2, 4);

        underTest.reset(newTarget);

        Target after = underTest.getTarget(c).get();
        assertNotEquals(before.cost(), after.cost());
    }

    @Test
    public void updateTest() {
        Coordinates c = Coordinates.fromXY(5, 5);
        Resource r = Namespace.urlOf(c);
        AskBuilder ask = new AskBuilder().addWhere(r, Namespace.distance, null);
        try (QueryExecution exec = QueryExecutionFactory.create(ask.build(), underTest.getModel())) {
            assertFalse(exec.execAsk());
        }
        underTest.update(Namespace.PlanningModel, c, Namespace.distance, 5);

        try (QueryExecution exec = QueryExecutionFactory.create(ask.build(), underTest.getModel())) {
            assertTrue(exec.execAsk());
        }
        ExprFactory exprF = new ExprFactory();

        ask = new AskBuilder().addWhere(r, Namespace.distance, "?o").addFilter(exprF.eq("?o", 5.0));
        try (QueryExecution exec = QueryExecutionFactory.create(ask.build(), underTest.getModel())) {
            assertTrue(exec.execAsk());
        }

        c = Coordinates.fromXY(expected[0]);
        try (QueryExecution exec = QueryExecutionFactory.create(ask.build(), underTest.getModel())) {
            assertTrue(exec.execAsk());
        }
        Target before = underTest.getTarget(c).get();
        underTest.update(Namespace.PlanningModel, c, Namespace.distance, before.cost() + 5);

        SelectBuilder sb = new SelectBuilder().addWhere(Namespace.urlOf(c), Namespace.distance, "?x");
        try (QueryExecution qexec = QueryExecutionFactory.create(sb.build(), underTest.getModel())) {
            int count[] = { 0 };
            Iterator<QuerySolution> results = qexec.execSelect();
            results.forEachRemaining((q) -> count[0]++);
            assertEquals(1, count[0]);
        }
        Target after = underTest.getTarget(c).get();
        assertEquals(before.cost() + 5, after.cost());
    }

}
