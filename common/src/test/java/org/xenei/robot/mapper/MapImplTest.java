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
import java.util.stream.Stream;

import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.arq.querybuilder.ExprFactory;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.geosparql.implementation.vocabulary.Geo;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.vocabulary.RDF;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.ScaleInfo;
import org.xenei.robot.common.mapping.CoordinateMap;
import org.xenei.robot.common.mapping.Obstacle;
import org.xenei.robot.common.planning.Step;
import org.xenei.robot.common.testUtils.CoordinateUtils;
import org.xenei.robot.common.testUtils.MapLibrary;
import org.xenei.robot.common.utils.AngleUtils;
import org.xenei.robot.common.utils.RobutContext;
import org.xenei.robot.common.utils.CoordUtils;
import org.xenei.robot.common.utils.DoubleUtils;
import org.xenei.robot.common.utils.GeometryUtils;
import org.xenei.robot.mapper.rdf.Namespace;

public class MapImplTest {

    private final double buffer = 0.5;
    
    private static RobutContext ctxt = new RobutContext(ScaleInfo.DEFAULT);

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
    
    CoordinateMap cMap;

    public static List<Coordinate> obstacleList() {
        return Arrays.asList(obstacles);
    }

        @BeforeAll
        public static void setupPaths() {
            for (Coordinate e : expected) {
                paths.add(new Coordinate[] { p, e });
            }
        }

    private void setup() {
        cMap = new CoordinateMap(1);
        underTest = new MapImpl(ctxt);
        underTest.addCoord(p, p.distance(t), false, true);
        cMap.enable(p, 'p');
        cMap.enable(t, 't');
        for (Coordinate e : expected) {
            underTest.addCoord(e, e.distance(t), false, true);
            cMap.enable(e, 'e');
        }
        Position pos = Position.from(p, 0);
        for (Coordinate o : obstacles) {
            underTest.addObstacle( underTest.createObstacle(pos, pos.relativeLocation(o)));
            cMap.enable(o, '#');
        }
        for (Coordinate[] l : paths) {
            underTest.addPath(l[0], l[1]);
        }
    }

    @Test
    public void getBestTargetTest() {
        setup();
        Optional<Step> pr = underTest.getBestStep(p, buffer);
        assertTrue(pr.isPresent());
        Coordinate p2 = new Coordinate(-1, -2);
        assertEquals(StepImpl.builder().setCoordinate(p2).setCost(9).setDistance(2).build(ctxt), pr.get());
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
        Optional<Step> pr = underTest.getStep(Location.from(p));
        assertTrue(pr.isPresent());
        assertEquals(0, CoordUtils.XYCompr.compare(p, pr.get().getCoordinate()));
        assertEquals(p.distance(t), pr.get().distance());
        // p can not see t so cost should be 2x distance
        assertEquals(pr.get().distance()*2, pr.get().cost());

        pr = underTest.getStep(Location.from(t));
        assertTrue(pr.isEmpty());

        for (Coordinate e : expected) {
            pr = underTest.getStep(Location.from(e));
            assertTrue(pr.isPresent());
            assertEquals(0, CoordUtils.XYCompr.compare(e, pr.get().getCoordinate()));
            assertEquals(e.distance(t), pr.get().distance());
        }
        for (Coordinate o : obstacles) {
            pr = underTest.getStep(Location.from(o));
            assertTrue(pr.isEmpty());
        }
    }

    @Test
    public void getTargetsTest() {
        setup();
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
        assertTrue(new MapImpl(ctxt).isEmpty());
    }

    @Test
    public void addPathTest() {
        setup();
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
        setup();
        Location a = Location.from(p);
        Location b = Location.from(expected[0]);
        Location c = Location.from(t);
        underTest.addCoord(t, 1, false, false);

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
        setup();
        AskBuilder ask = new AskBuilder();
        ExprFactory exprF = ask.getExprFactory();
        ask.from(Namespace.UnionModel.getURI()).addWhere(Namespace.s, RDF.type, Namespace.Coord)
        .addOptional(Namespace.s, Namespace.isIndirect, Namespace.o)
                .addFilter( exprF.not( exprF.bound(Namespace.o)));
        
        Location c = Location.from(expected[0]);
        Step before = underTest.getStep(c).get();
        assertFalse( underTest.ask(ask), () ->"Should not have any direct points" );

        Location newTarget = Location.from(-4, 1);
        underTest.recalculate(newTarget.getCoordinate(), buffer);
        
        Step after = underTest.getStep(c).get();
        assertNotEquals(before.cost(), after.cost());
        assertTrue(underTest.ask(ask), ()->"Should have some indirect points");
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
        underTest.updateCoordinate(Namespace.PlanningModel, Namespace.Coord, c.getCoordinate(), Namespace.distance, 5);
        assertFalse(underTest.ask(ask));

        // add the coordinate with a distance of 1.
        underTest.addCoord(c.getCoordinate(), 1, false, false);
        assertFalse(underTest.ask(ask));
        // now update it to 5 and verify that it is there.
        underTest.updateCoordinate(Namespace.PlanningModel, Namespace.Coord, c.getCoordinate(), Namespace.distance, 5);
        assertTrue(underTest.ask(ask));

        ExprFactory exprF = new ExprFactory();

        ask = new AskBuilder().from(Namespace.UnionModel.getURI()) //
                .addWhere(Namespace.s, Namespace.distance, Namespace.o) //
                .addFilter(exprF.eq(Namespace.o, 5.0));
        assertTrue(underTest.ask(ask));

        
        c = Location.from(expected[0]);
        assertTrue(underTest.ask(ask));

        Step before = underTest.getStep(c).get();
        underTest.updateCoordinate(Namespace.PlanningModel, Namespace.Coord, c.getCoordinate(), Namespace.distance,
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

        Step after = underTest.getStep(c).get();
        assertEquals(before.distance() + 5, after.distance());
    }

    @Test
    public void clearViewTest() {
        underTest = new MapImpl(ctxt);
        Position pos = Position.from(p, 0);

        underTest.addObstacle(underTest.createObstacle(pos, pos.relativeLocation(new  Coordinate(-3, -3))));
        Coordinate a = new Coordinate(-3, -4);
        Coordinate b = new Coordinate(-3, -2);

        assertFalse(underTest.isClearPath(a, b, buffer));
        b = new Coordinate(-4, -4);
        assertTrue(underTest.isClearPath(a, b, buffer));
    }

    @Test
    public void testAddTarget() {
        underTest = new MapImpl(ctxt);
        Step step = underTest.addCoord(p, 11, false, false).get();
        assertEquals( 11, step.cost());
        
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
        Coordinate c = new Coordinate( p.getX()+incr, p.getY()+incr );
                //-3 + (scale.getResolution() / 2) + (scale.getResolution() / 10));
        step = underTest.addCoord(c, 11, false, false).get();
        assertTrue(underTest.ask(ask));
        assertEquals( 11, step.cost());
    }

    @Test
    public void testAddPath() {
        underTest = new MapImpl(ctxt);
        assertTrue( underTest.addCoord(p, p.distance(t), false, false).isPresent());
        assertTrue(underTest.addCoord(expected[0], expected[0].distance(t), false, false).isPresent());
        Coordinate[] path = underTest.addPath(p, expected[0]);

        ExprFactory exprF = new ExprFactory(MapImpl.getPrefixMapping());
        Var wkt = Var.alloc("wkt");
        Literal pathWkt = ctxt.graphGeomFactory.asWKTString(path);
        AskBuilder ask = new AskBuilder().addPrefixes(MapImpl.getPrefixMapping()).from(Namespace.PlanningModel.getURI())
                .addWhere(Namespace.s, RDF.type, Namespace.Coord)
                .addWhere(Namespace.s, Geo.AS_WKT_PROP, wkt)
                .addFilter(ctxt.graphGeomFactory.isNearby( exprF, wkt, pathWkt, ctxt.scaleInfo.getResolution()));

        assertTrue(underTest.ask(ask));
    }
    
    @Test
    public void createObstacleTest() {
        underTest = new MapImpl(ctxt);
        Position pos = Position.from(p, 0);
        Location relative = Location.from(1,0);
        Obstacle obst = underTest.createObstacle(pos, relative);
        Coordinate[] lst = obst.geom().getCoordinates();
        assertEquals( 1, lst.length);
        CoordinateUtils.assertEquivalent(new Coordinate(0,-3), lst[0], ctxt.scaleInfo.getResolution());
        assertEquals( ctxt.graphGeomFactory.asWKT(obst.geom()), obst.wkt());
    }
    
    @Test
    public void addObstacleTest() {
        underTest = new MapImpl(ctxt);
        Position pos = Position.from(p, 0);
        Location relative = Location.from(ctxt.scaleInfo.getResolution(),0);
        Obstacle obst = underTest.createObstacle(pos, relative);
        Set<Obstacle> result = underTest.addObstacle(obst);
        assertEquals(1, result.size());
        assertEquals(obst, result.iterator().next());
        System.out.println(MapReports.dumpModel(underTest));
        relative = Location.from(0, ctxt.scaleInfo.getResolution());
        Obstacle obst2 = underTest.createObstacle(pos, relative);
        result = underTest.addObstacle(obst2);
        relative = Location.from(ctxt.scaleInfo.getHalfResolution(),ctxt.scaleInfo.getHalfResolution());
        Obstacle obst3 = underTest.createObstacle(pos, relative);
        result = underTest.addObstacle(obst3);
        System.out.println(MapReports.dumpModel(underTest));
        System.out.println( obst2.wkt());
        assertEquals(1, underTest.getObstacles().size());
    }
    
    @Test
    public void isObstacleTest() {
        underTest = new MapImpl(ctxt);
        Position pos = Position.from(p, 0);
        Location relative = Location.from(1,0);
        Obstacle obst = underTest.createObstacle(pos, relative);
        underTest.addObstacle(obst);
        relative = Location.from(1, 1);
        Obstacle obst2 = underTest.createObstacle(pos, relative);
        underTest.addObstacle(obst2);
        relative = Location.from(CoordUtils.fromAngle(AngleUtils.RADIANS_45/2, 1));
        Obstacle obst3 = underTest.createObstacle(pos, relative);
        underTest.addObstacle(obst3);
        for (Coordinate c : obst.geom().getCoordinates())
        assertTrue(underTest.isObstacle(c), ()-> "Did not find c");
        for (Coordinate c : obst2.geom().getCoordinates())
            assertTrue(underTest.isObstacle(c), ()-> "Did not find c");
        for (Coordinate c : obst3.geom().getCoordinates())
            assertTrue(underTest.isObstacle(c), ()-> "Did not find c");
    }
//    
//    @ParameterizedTest(name = "{index} {1}")
//    @MethodSource("loadObstaclesTestParameters")
//    public void loadObstaclesTest(MapImpl underTest, Coordinate c) {
//        assertTrue( underTest.isObstacle(c));
//    }
//    
//    private static void listFail(Geometry geom, Coordinate c) {
//        if (!geom.intersects(ctxt.geometryUtils.asPoint(c))) {
//            System.out.format( "%s %s failed\n", geom, c);
//        }
//    }
//    private static Stream<Arguments> loadObstaclesTestParameters() {
//        CoordinateMap cMap =  MapLibrary.map2('#');
//        MapImpl underTest= new MapImpl(ScaleInfo.DEFAULT);
//        List<Coordinate> lst = new ArrayList<>();
//        
//        cMap.getObstacles().forEach( g -> {
//            Point p = g.getCentroid();
//            Coordinate c = new Coordinate( p.getX(), p.getY());
//            lst.add(c);
//            underTest.addObstacle(c);
//            listFail(g, c );
//            Arrays.stream(g.getCoordinates()).forEach( c2 -> {lst.add(c2);
//            listFail(g,c2);
//          
//            });  
//        });
//        
//        return lst.stream().map( c -> Arguments.of( underTest, c));
//    }

}
