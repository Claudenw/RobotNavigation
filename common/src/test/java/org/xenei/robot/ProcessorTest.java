package org.xenei.robot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Collection;
import java.util.function.Supplier;

import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.arq.querybuilder.ExprFactory;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.geosparql.implementation.vocabulary.Geo;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.vocabulary.RDF;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.Processor.AbortTest;
import org.xenei.robot.common.AbortedException;
import org.xenei.robot.common.ChassisInfo;
import org.xenei.robot.common.DistanceSensor;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.Mover;
import org.xenei.robot.common.NavigationSnapshot;
import org.xenei.robot.common.Position;
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
import org.xenei.robot.common.testUtils.TestChassisInfo;
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

//    private MapViz mapViz;
//    private Processor underTest;
//    private final Map map;
    private final RobutContext ctxt;
//    private Planner planner;
//    private final Mapper mapper;
//    private FakeDistanceSensor sensor;
//    private Mover mover;


    ProcessorTest() {
        ctxt = new RobutContext(ScaleInfo.DEFAULT, TestChassisInfo.DEFAULT);
//        map = new MapImpl(ctxt);
//        mapper = new MapperImpl(map);
//        // public Processor(RobutContext ctxt, Mover mover, Supplier<Position> positionSupplier, DistanceSensor sensor, Mapper.Visualization visualization) {
//        
//        underTest = new Processor(ctxt, )
    }

    
    private void doTest(Location startCoord, Location finalCoord, Mover mover, DistanceSensor sensor) throws AbortedException {
        Supplier<Position> positionSupplier = mover::position;
 
        Processor underTest = new Processor(ctxt, mover, positionSupplier, sensor);
        MapViz mapViz = new MapViz(100, underTest.map, underTest.planner::getSolution, positionSupplier);
        underTest.add(mapViz);
        underTest.moveTo(finalCoord, new StepTracker());
    }

    @Test
    public void stepTestMap2() throws AbortedException {
        Location startCoord = Location.from(-1, -3);
        Mover mover = new FakeMover(Location.from(startCoord), 1);
        DistanceSensor sensor = new FakeDistanceSensor1(MapLibrary.map2('#'), mover::position);
        Location finalCoord = Location.from(-1, 1);
        doTest(startCoord, finalCoord, mover, sensor);
    }

    @Test
    @Disabled
    public void stepTestMap3() throws AbortedException {
        Location startCoord = Location.from(-1, -3);
        Mover mover = new FakeMover(Location.from(startCoord), 1);
        DistanceSensor sensor = new FakeDistanceSensor2(MapLibrary.map3('#'), AngleUtils.RADIANS_45, mover::position);
        Location finalCoord = Location.from(-1, 1);
        doTest(startCoord, finalCoord, mover, sensor);
    }
    
    private class StepTracker implements AbortTest {
        private int stepCount = 0;
        private int maxLoops = 100;
    
        @Override
        public void check(Processor processor) throws AbortedException {
            if (maxLoops < stepCount++) {
              throw new AbortedException("Did not find solution in " + maxLoops + " steps");
            }
        }
    }
}
