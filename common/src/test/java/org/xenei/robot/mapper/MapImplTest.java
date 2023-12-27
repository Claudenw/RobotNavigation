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
import java.util.function.BiPredicate;

import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.arq.querybuilder.ExprFactory;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.geosparql.implementation.vocabulary.Geo;
import org.apache.jena.vocabulary.RDF;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.ScaleInfo;
import org.xenei.robot.common.planning.Step;
import org.xenei.robot.common.utils.CoordUtils;
import org.xenei.robot.common.utils.GeometryUtils;
import org.xenei.robot.mapper.rdf.Namespace;

public class MapImplTest {

    private final double buffer = 0.5;

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
        ScaleInfo scale = new ScaleInfo.Builder().build();
        underTest = new MapImpl(scale);
        underTest.addCoord(p, p.distance(t), false, true);
        for (Coordinate e : expected) {
            underTest.addCoord(e, e.distance(t), false, true);
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
    public void getBestTargetTest() {
        Optional<Step> pr = underTest.getBestStep(p, buffer);
        assertTrue(pr.isPresent());
        Coordinate p2 = new Coordinate(-1, -2);
        assertEquals(new StepImpl(p2, 4), pr.get());
    }

    /**
     * Checks that at least oneof the geometries (obsts) contains the coordinate.
     * 
     * @param obsts the list of geometries.
     * @param c he coorindate to contain.
     */
    public static void assertCoordinateInObstacles(Collection<? extends Geometry> obsts, Coordinate c) {
        boolean found = false;
        for (Geometry geom : obsts) {
            if (List.of( geom.getCoordinates()).contains(c)) {
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
        Optional<Step> pr = underTest.getStep(Location.from(p));
        assertTrue(pr.isPresent());
        assertEquals(0, CoordUtils.XYCompr.compare(p, pr.get().getCoordinate()));
        assertEquals(p.distance(t), pr.get().cost());

        pr = underTest.getStep(Location.from(t));
        assertTrue(pr.isEmpty());

        for (Coordinate e : expected) {
            pr = underTest.getStep(Location.from(e));
            assertTrue(pr.isPresent());
            assertEquals(0, CoordUtils.XYCompr.compare(e, pr.get().getCoordinate()));
            assertEquals(e.distance(t), pr.get().cost());
        }
        for (Coordinate o : obstacles) {
            pr = underTest.getStep(Location.from(o));
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
        assertTrue(new MapImpl(ScaleInfo.DEFAULT).isEmpty());
        assertFalse(underTest.isEmpty());
    }

    @Test
    public void addPathTest() {
        Location a = Location.from(expected[0]);
        Location b = Location.from(expected[1]);

        assertFalse(underTest.hasPath(a, b), () -> "Should not have path");

        Location c = Location.from(5, 5);
        underTest.addCoord(c.getCoordinate(), c.distance(a), false, false);

        underTest.addPath(a.getCoordinate(), c.getCoordinate());

        assertTrue(underTest.hasPath(a, c), () -> "Should have path");
    }

    @Test
    public void hasPathTest() {
        Location a = Location.from(p);
        Location b = Location.from(expected[0]);
        Location c = Location.from(t);
        underTest.addCoord(t, 0, false, false);

        System.out.println(MapReports.dumpModel(underTest));
        assertTrue(underTest.hasPath(a, b));
        assertFalse(underTest.hasPath(b, c));
        assertFalse(underTest.hasPath(a, c));

        underTest.addPath(b.getCoordinate(), c.getCoordinate());

        System.out.println(MapReports.dumpModel(underTest));
        assertTrue(underTest.hasPath(a, b));
        assertTrue(underTest.hasPath(b, c));
        // FIXME assertTrue(underTest.hasPath(a, c));
    }

    @Test
    public void recalculateTest() {
        Location c = Location.from(expected[0]);
        Step before = underTest.getStep(c).get();

        Location newTarget = Location.from(-2, 2);

        underTest.recalculate(newTarget.getCoordinate(), buffer);
        Step after = underTest.getStep(c).get();
        assertNotEquals(before.cost(), after.cost());
    }

    @Test
    public void updateTest() {
        Location c = Location.from(5, 4);
        // Resource r = Namespace.urlOf(c);

        // check not there, update then verify that it is.
        AskBuilder ask = new AskBuilder().from(Namespace.UnionModel.getURI())
                .addWhere(Namespace.s, Namespace.distance, 5).addWhere(Namespace.s, Namespace.x, c.getX())
                .addWhere(Namespace.s, Namespace.y, c.getY());
        assertFalse(underTest.ask(ask));

        underTest.updateCoordinate(Namespace.PlanningModel, Namespace.Coord, c.getCoordinate(), Namespace.distance, 5);
        assertFalse(underTest.ask(ask));

        underTest.addCoord(c.getCoordinate(), 0, false, false);
        assertFalse(underTest.ask(ask));
        underTest.updateCoordinate(Namespace.PlanningModel, Namespace.Coord, c.getCoordinate(), Namespace.distance, 5);
        System.out.println(MapReports.dumpModel(underTest));
        assertTrue(underTest.ask(ask));

        ExprFactory exprF = new ExprFactory();

        ask = new AskBuilder().from(Namespace.UnionModel.getURI())
                .addWhere(Namespace.s, Namespace.distance, Namespace.o).addFilter(exprF.eq(Namespace.o, 5.0));
        assertTrue(underTest.ask(ask));

        c = Location.from(expected[0]);
        assertTrue(underTest.ask(ask));

        Step before = underTest.getStep(c).get();
        underTest.updateCoordinate(Namespace.PlanningModel, Namespace.Coord, c.getCoordinate(), Namespace.distance,
                before.cost() + 5);

        SelectBuilder sb = new SelectBuilder().from(Namespace.UnionModel.getURI())
                .addWhere(Namespace.s, Namespace.distance, "?x").addWhere(Namespace.s, Namespace.x, c.getX())
                .addWhere(Namespace.s, Namespace.y, c.getY());
        int count[] = { 0 };
        underTest.exec(sb, (q) -> {
            count[0]++;
            return true;
        });
        assertEquals(1, count[0]);

        Step after = underTest.getStep(c).get();
        assertEquals(before.cost() + 5, after.cost());
    }

    @Test
    public void clearViewTest() {
        ScaleInfo scale = new ScaleInfo.Builder().build();
        underTest = new MapImpl(scale);
        
        underTest.addObstacle(new Coordinate(-3, -3));
        Coordinate a = new Coordinate(-3, -4);
        Coordinate b = new Coordinate(-3, -2);

        assertFalse(underTest.clearView(a, b, buffer));
        b = new Coordinate(-4, -4);
        assertTrue(underTest.clearView(a, b, buffer));
    }

    @Test
    public void testAddTarget() {
        ScaleInfo scale = new ScaleInfo.Builder().build();
        underTest = new MapImpl(scale);
        underTest.addCoord(p, 11, false, false);
        
        AskBuilder ask = new AskBuilder().addGraph(Namespace.PlanningModel,
                new WhereBuilder().addWhere(Namespace.s, Namespace.x, p.x).addWhere(Namespace.s, Namespace.y, p.y)
                        .addWhere(Namespace.s, Namespace.distance, 11.0)
                        .addWhere(Namespace.s, RDF.type, Namespace.Coord)
                        .addWhere(Namespace.s, Geo.AS_WKT_PROP, GraphGeomFactory.asWKT(GeometryUtils.asPoint(p))));
        assertTrue(underTest.ask(ask));

        // verify nearby shows up as -1,-3
        underTest = new MapImpl(scale);
        Coordinate c = new Coordinate(-1 + (scale.getResolution() / 2),
                -3 + (scale.getResolution() / 2)+(scale.getResolution() / 10));
        Step step = underTest.addCoord(c, 11, false, false);
        assertTrue(underTest.ask(ask));
    }

    @Test
    public void testAddPath() {
        ScaleInfo scale = new ScaleInfo.Builder().build();
        underTest = new MapImpl(scale);
        underTest.addCoord(p, p.distance(t), false, false);
        underTest.addCoord(expected[0], expected[0].distance(t), false, false);
        underTest.addPath(p, expected[0]);

        AskBuilder ask = new AskBuilder().addPrefixes(MapImpl.getPrefixMapping()).from(Namespace.PlanningModel.getURI())
                .addWhere(Namespace.s, RDF.type, Namespace.Path)
                .addWhere(Namespace.s, Geo.AS_WKT_PROP, GraphGeomFactory.asWKT(GeometryUtils.asLine(p, expected[0])))
                .addWhere(Namespace.s, "robut:point/geo:asWKT", GraphGeomFactory.asWKT(GeometryUtils.asPoint(p)))
                .addWhere(Namespace.s, "robut:point/geo:asWKT",
                        GraphGeomFactory.asWKT(GeometryUtils.asPoint(expected[0])));
        System.out.println(MapReports.dumpModel(underTest));
        assertTrue(underTest.ask(ask));
    }

}
