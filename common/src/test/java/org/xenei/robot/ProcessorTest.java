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
import org.xenei.robot.common.utils.RobutContext;
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
    private final RobutContext ctxt;
    private Planner planner;
    private final Mapper mapper;
    private FakeDistanceSensor sensor;
    private final Mover mover;
    private final double buffer;

    ProcessorTest() {
        ctxt = new RobutContext(ScaleInfo.DEFAULT);
        map = new MapImpl(ctxt);
        //map = new MapImpl(ScaleInfo.builder().setResolution(.3).build());
        mapViz = new MapViz(100, map, () -> planner.getSolution());
        mapper = new MapperImpl(map);
        mover = new FakeMover(Location.from(-1, -3), 1);
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
            if (mapper.isClearPath(planner.getCurrentPosition(), planner.getRootTarget(), buffer)) {
                double newHeading = planner.getCurrentPosition().headingTo(planner.getRootTarget());
                boolean cont = DoubleUtils.eq(newHeading, planner.getCurrentPosition().getHeading());
                if (!cont) {
                    // heading is different so reset the heading, scan, and check again.
                    mover.setHeading(planner.getCurrentPosition().headingTo(planner.getRootTarget()));
                    processSensor();
                    cont = mapper.isClearPath(planner.getCurrentPosition(), planner.getRootTarget(), buffer);
                    if (!cont) {
                        // can't see the position really so reset the heading.
                        mover.setHeading(planner.getCurrentPosition().getHeading());
                    }
                }
                if (cont) {
                    // we can really see the final position.
                    LOG.info("can see {} from {}", planner.getRootTarget(), planner.getCurrentPosition());
                    Literal pathWkt = ctxt.graphGeomFactory.asWKTPath(buffer, planner.getRootTarget(), 
                            planner.getCurrentPosition().getCoordinate());
                    Var wkt = Var.alloc("wkt");

                    ExprFactory exprF = new ExprFactory();
                    System.out.println(MapReports.dumpQuery((MapImpl) map, new SelectBuilder() //
                            .from(Namespace.UnionModel.getURI()) //
                            .addWhere(Namespace.s, RDF.type, Namespace.Obst) //
                            .addWhere(Namespace.s, Geo.AS_WKT_PROP, wkt)
                            .addBind(ctxt.graphGeomFactory.calcDistance(exprF, pathWkt, wkt), "?dist")
                            .addBind(exprF.eq(ctxt.graphGeomFactory.calcDistance(exprF, pathWkt, wkt), 0), "?le")
                            ));

                    AskBuilder ask = new AskBuilder().from(Namespace.UnionModel.getURI()) //
                            .addWhere(Namespace.s, RDF.type, Namespace.Obst) //
                            .addWhere(Namespace.s, Geo.AS_WKT_PROP, wkt)
                            .addFilter(exprF.eq(ctxt.graphGeomFactory.calcDistance(exprF, pathWkt, wkt), 0));
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
        return mapper.isClearPath(planner.getCurrentPosition(), planner.getTarget(), buffer);
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
                System.out.println( MapReports.dumpObstacles((MapImpl)map));
                System.out.println( MapReports.dumpObstacleDistance((MapImpl)map));
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
        System.out.println( "SUCCESS");
        solution.simplify( (a,b) -> map.isClearPath(a, b, buffer));
        System.out.println("Solution 2");
        solution.stream().forEach(System.out::println);
        
    }

    @Test
    public void stepTestMap2() {
        sensor = new FakeDistanceSensor1(MapLibrary.map2('#'));
        map.clear(Namespace.UnionModel.getURI());

        Location finalCoord = Location.from(-1, 1);
        Location startCoord = Location.from(-1, -3);
        doTest(startCoord, finalCoord);
    }

    @Test
    public void stepTestMap3() {
        sensor = new FakeDistanceSensor2(MapLibrary.map3('#'), AngleUtils.RADIANS_45);
        map.clear(Namespace.UnionModel.getURI());

        Location finalCoord = Location.from(-1, 1);
        Location startCoord = Location.from(-1, -3);
        doTest(startCoord, finalCoord);
    }
}
