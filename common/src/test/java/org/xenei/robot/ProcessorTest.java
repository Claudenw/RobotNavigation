package org.xenei.robot;

import static org.junit.jupiter.api.Assertions.fail;

import java.util.Collection;

import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.arq.querybuilder.ExprFactory;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.geosparql.implementation.vocabulary.Geo;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.vocabulary.RDF;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.Mover;
import org.xenei.robot.common.ScaleInfo;
import org.xenei.robot.common.mapping.Map;
import org.xenei.robot.common.mapping.Mapper;
import org.xenei.robot.common.planning.Planner;
import org.xenei.robot.common.planning.Planner.Diff;
import org.xenei.robot.common.planning.Solution;
import org.xenei.robot.common.planning.Step;
import org.xenei.robot.common.testUtils.CoordinateUtils;
import org.xenei.robot.common.testUtils.FakeDistanceSensor;
import org.xenei.robot.common.testUtils.FakeDistanceSensor1;
import org.xenei.robot.common.testUtils.FakeDistanceSensor2;
import org.xenei.robot.common.testUtils.FakeMover;
import org.xenei.robot.common.testUtils.MapLibrary;
import org.xenei.robot.common.utils.AngleUtils;
import org.xenei.robot.common.utils.DoubleUtils;
import org.xenei.robot.mapper.GraphGeomFactory;
import org.xenei.robot.mapper.MapImpl;
import org.xenei.robot.mapper.MapReports;
import org.xenei.robot.mapper.MapperImpl;
import org.xenei.robot.mapper.rdf.Namespace;
import org.xenei.robot.mapper.visualization.MapViz;
import org.xenei.robot.planner.PlannerImpl;

public class ProcessorTest {
    private static final Logger LOG = LoggerFactory.getLogger(ProcessorTest.class);

    private final MapViz mapViz;
    private final Map map;
    private Planner planner;
    private final Mapper mapper;
    private FakeDistanceSensor sensor;
    private final Mover mover;
    private final double buffer;

    ProcessorTest() {
        map = new MapImpl(ScaleInfo.DEFAULT);
        mapViz = new MapViz(100, map, () -> planner.getSolution());
        mapper = new MapperImpl(map);
        mover = new FakeMover(new Location(-1, -3), 1);
        buffer = 0.25;
        planner = new PlannerImpl(map, mover.position(), buffer);
        sensor = new FakeDistanceSensor2(MapLibrary.map2('#'), AngleUtils.RADIANS_45);
        planner.addListener(() -> sensor.setPosition(planner.getCurrentPosition()));
    }

    private Collection<Step> processSensor() {
        sensor.setPosition(planner.getCurrentPosition());
        return mapper.processSensorData(planner.getCurrentPosition(), buffer, planner.getRootTarget(), sensor.sense());
    }

    public boolean checkTarget() {
        if (!mapper.equivalent(planner.getCurrentPosition(), planner.getRootTarget())) {
            // if we can see the final target go that way.
            if (mapper.clearView(planner.getCurrentPosition(), planner.getRootTarget(), buffer)) {
                double newHeading = planner.getCurrentPosition().headingTo(planner.getRootTarget());
                boolean cont = DoubleUtils.eq(newHeading, planner.getCurrentPosition().getHeading());
                if (!cont) {
                    // heading is different so reset the heading, scan, and check again.
                    mover.setHeading(planner.getCurrentPosition().headingTo(planner.getRootTarget()));
                    processSensor();
                    cont = mapper.clearView(planner.getCurrentPosition(), planner.getRootTarget(), buffer);
                    if (!cont) {
                        // can't see the position really so reset the heading.
                        mover.setHeading(planner.getCurrentPosition().getHeading());
                    }
                }
                if (cont) {
                    // we can really see the final position.
                    LOG.info("can see {} from {}", planner.getRootTarget(), planner.getCurrentPosition());
                    Literal pathWkt = GraphGeomFactory.asWKTString(planner.getRootTarget(), 
                            planner.getCurrentPosition().getCoordinate());
                    Var wkt = Var.alloc("wkt");

                    ExprFactory exprF = new ExprFactory();
                    System.out.println(MapReports.dumpQuery((MapImpl) map, new SelectBuilder() //
                            .from(Namespace.UnionModel.getURI()) //
                            .addWhere(Namespace.s, RDF.type, Namespace.Obst) //
                            .addWhere(Namespace.s, Geo.AS_WKT_PROP, wkt)
                            .addBind(GraphGeomFactory.checkCollision(exprF, pathWkt, wkt, buffer), "?colission")
                            .addBind(GraphGeomFactory.calcDistance(exprF, pathWkt, wkt), "?dist")
                            .addBind(exprF.le(GraphGeomFactory.calcDistance(exprF, pathWkt, wkt), buffer), "?le")
                            ));

                    AskBuilder ask = new AskBuilder().from(Namespace.UnionModel.getURI()) //
                            .addWhere(Namespace.s, RDF.type, Namespace.Obst) //
                            .addWhere(Namespace.s, Geo.AS_WKT_PROP, wkt)
                            .addFilter(GraphGeomFactory.checkCollision(new ExprFactory(), pathWkt, wkt, buffer));
                    System.out.println( ((MapImpl)map).ask(ask));
                    System.out.println(MapReports.dumpQuery((MapImpl) map,
                            new SelectBuilder().from(Namespace.UnionModel.getURI())
                                    .addWhere(Namespace.s, Namespace.p, Namespace.o)
                                    .addWhere(Namespace.s, RDF.type, Namespace.Obst)));
                    planner.replaceTarget(planner.getRootTarget());
                    planner.notifyListeners();
                    return true;
                }
            }
        }
        // if we can not see the target replan.
        return mapper.clearView(planner.getCurrentPosition(), planner.getTarget(), buffer);
    }

    private void doTest(Location startCoord, Location finalCoord) {
        planner = new PlannerImpl(map, startCoord, buffer, finalCoord);
        mover.setHeading(planner.getCurrentPosition().getHeading());
        planner.addListener(() -> sensor.setPosition(planner.getCurrentPosition()));
        planner.addListener(() -> mapViz.redraw(planner.getTarget()));

        processSensor();

        int stepCount = 0;
        int maxLoops = 100;
        while (planner.getTarget() != null) {
            Diff diff = planner.selectTarget();
            if (planner.getTarget() != null) {
                if (diff.didChange()) {
                    // look where we are heading.
                    processSensor();
                    planner.notifyListeners();
                }
                // can we still see the target
                if (checkTarget()) {
                    // move
                    Location relativeLoc = planner.getCurrentPosition().relativeLocation(planner.getTarget());
                    planner.changeCurrentPosition(mover.move(relativeLoc));
                    processSensor();
                }
                if (maxLoops < stepCount++) {
                    fail("Did not find solution in " + maxLoops + " steps");
                }
                planner.notifyListeners();
                System.out.println( MapReports.dumpDistance((MapImpl)map) );
                diff.reset();
            }
        }
        planner.notifyListeners();
        Solution solution = planner.getSolution();
        CoordinateUtils.assertEquivalent(startCoord, solution.start(), buffer);
        CoordinateUtils.assertEquivalent(startCoord, solution.start(), buffer);
        System.out.println( MapReports.dumpDistance((MapImpl)map) );
        System.out.println("Solution");
        solution.stream().forEach(System.out::println);
    }

    @Test
    public void stepTestMap2() {
//        Coordinate[] expectedSolution = { new Coordinate(-1.0, -3.0), new Coordinate(-0.9999999999999999, -2.0),
//                new Coordinate(-0.4472135954999578, -1.8944271909999157),
//                new Coordinate(0.2928932188134528, -1.7071067811865475),
//                new Coordinate(-1.5527864045000421, -1.8944271909999157),
//                new Coordinate(-4.000468632496236, -1.9693888030933675),
//                new Coordinate(-4.547420028549094, -0.8917238190389993), new Coordinate(-1.0, 1.0) };
        sensor = new FakeDistanceSensor1(MapLibrary.map2('#'));
        map.clear(Namespace.UnionModel.getURI());

        Location finalCoord = new Location(-1, 1);
        Location startCoord = new Location(-1, -3);
        doTest(startCoord, finalCoord);
//
//        assertEquals(SolutionTest.expectedSolution.length - 1, planner.getSolution().stepCount());
//        List<Coordinate> solution = planner.getSolution().stream().collect(Collectors.toList());
//        assertEquals(SolutionTest.expectedSolution.length, solution.size());
//        for (int i = 0; i < solution.size(); i++) {
//            final int j = i;
//            assertTrue(expectedSolution[i].equals2D(solution.get(i), 0.00001),
//                    () -> String.format("failed at %s: %s == %s +/- 0.00001", j, SolutionTest.expectedSolution[j],
//                            solution.get(j)));
//        }
    }

    @Test
    public void stepTestMap3() {
//        Coordinate[] expectedSimpleSolution = { new Coordinate(-1, -3), new Coordinate(-3, -2), new Coordinate(-4, -1),
//                new Coordinate(-4, 0), new Coordinate(-1, 1) };
        sensor = new FakeDistanceSensor2(MapLibrary.map3('#'), AngleUtils.RADIANS_45);
        map.clear(Namespace.UnionModel.getURI());

        Location finalCoord = new Location(-1, 1);
        Location startCoord = new Location(-1, -3);
        doTest(startCoord, finalCoord);
//
//        assertEquals(25, underTest.getSolution().stepCount());
//        assertEquals(33.29126786466034, underTest.getSolution().cost());
//
//        underTest.getSolution().simplify(map::clearView);
//        assertEquals(7.812559200041265, underTest.getSolution().cost());
//        assertTrue(startCoord.equals2D(underTest.getSolution().start()));
//        assertTrue(finalCoord.equals2D(underTest.getSolution().end()));
//        List<Coordinate> solution = underTest.getSolution().stream().collect(Collectors.toList());
//        assertEquals(expectedSimpleSolution.length, solution.size());
//        for (int i = 0; i < solution.size(); i++) {
//            assertTrue(expectedSimpleSolution[i].equals2D(solution.get(i)));
//        }
    }
}
