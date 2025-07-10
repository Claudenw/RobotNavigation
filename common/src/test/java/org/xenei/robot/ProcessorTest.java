package org.xenei.robot;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.common.DistanceSensor;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.Mover;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.ScaleInfo;
import org.xenei.robot.common.mapping.Map;
import org.xenei.robot.common.testUtils.FakeDistanceSensor1;
import org.xenei.robot.common.testUtils.FakeDistanceSensor2;
import org.xenei.robot.common.testUtils.FakeMover;
import org.xenei.robot.common.testUtils.MapLibrary;
import org.xenei.robot.common.testUtils.TestChassisInfo;
import org.xenei.robot.common.utils.AngleUtils;
import org.xenei.robot.common.utils.RobutContext;
import org.xenei.robot.mapper.MapImpl;
import org.xenei.robot.mapper.visualization.MapViz;

public class ProcessorTest {
    private static final Logger LOG = LoggerFactory.getLogger(ProcessorTest.class);
    private final RobutContext ctxt;

    ProcessorTest() {
        ctxt = new RobutContext(ScaleInfo.DEFAULT, TestChassisInfo.DEFAULT);
    }

    private void doTest(Location startCoord, Location finalCoord, FakeMover mover, DistanceSensor sensor) {
        Supplier<Position> positionSupplier = mover::position;
        MapImpl map = new MapImpl(ctxt);
        Processor underTest = new Processor(mover, positionSupplier, map);
        MapViz mapViz = new MapViz(100, underTest.map, underTest.planner::getSolution, positionSupplier, () -> null);
        ctxt.visualizations.register(mapViz);
        SegmentTracker segmentTracer = new SegmentTracker(ctxt);
        try {
            underTest.moveTo(finalCoord);
            Coordinate target;
            while ((target = underTest.getPlanner().getTarget()) != null) {
                int stepsToTarget = ctxt.chassisInfo.steps(positionSupplier.get().distance(target));
                mover.sleep(stepsToTarget);
            }
        } finally {
            ctxt.bus.moveTo.unregister(segmentTracer);
        }
    }

    @Test
    public void stepTestMap2() {
        Location startCoord = Location.from(-1, -3);
        FakeMover mover = new FakeMover(ctxt, startCoord.getCoordinate());
        Map m = new MapImpl(new RobutContext(ScaleInfo.DEFAULT, TestChassisInfo.DEFAULT));
        DistanceSensor sensor = new FakeDistanceSensor1(MapLibrary.map2(m), mover::position);
        Location finalCoord = Location.from(-1, 1);
        doTest(startCoord, finalCoord, mover, sensor);
    }

    @Test
    public void stepTestMap3() {
        Location startCoord = Location.from(-1, -3);
        FakeMover mover = new FakeMover(ctxt, startCoord.getCoordinate());
        Map m = new MapImpl(new RobutContext(ScaleInfo.DEFAULT, TestChassisInfo.DEFAULT));
        DistanceSensor sensor = new FakeDistanceSensor2(MapLibrary.map3(m), AngleUtils.RADIANS_45, mover::position);
        Location finalCoord = Location.from(-1, 1);
        doTest(startCoord, finalCoord, mover, sensor);
    }
    
    @Test
    public void stepTestEmptyMap() {
        Location startCoord = Location.from(-1, -3);
        FakeMover mover = new FakeMover(ctxt, startCoord.getCoordinate());
        Map m = new MapImpl(new RobutContext(ScaleInfo.DEFAULT, TestChassisInfo.DEFAULT));
        DistanceSensor sensor = new FakeDistanceSensor1(m, mover::position);
        Location finalCoord = Location.from(-1, 1);
        doTest(startCoord, finalCoord, mover, sensor);
    }

    private class SegmentTracker implements Consumer<Mover.MoveTo> {
        private int maxSegments = 100;
        private int totalSegments = 0;
        private RobutContext ctxt;


        SegmentTracker(RobutContext ctxt) {
            ctxt.bus.moveTo.register(this);
            this.ctxt = ctxt;
        }

        @Override
        public void accept(Mover.MoveTo moveTo) {
            if (++totalSegments > maxSegments) {
                ctxt.bus.motor.send(Mover.MotorState.STOP);
                throw new RuntimeException("Did not find solution in " + maxSegments + " steps");
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        RobutContext ctxt = new RobutContext(ScaleInfo.DEFAULT, TestChassisInfo.DEFAULT);
        Location startCoord = Location.from(-1, -3);
        FakeMover mover = new FakeMover(ctxt, startCoord.getCoordinate());
        Map m = new MapImpl(new RobutContext(ScaleInfo.DEFAULT, TestChassisInfo.DEFAULT));
        DistanceSensor sensor = new FakeDistanceSensor1(MapLibrary.map2(m), mover::position);
        Location finalCoord = Location.from(-1, 1);
        Supplier<Position> positionSupplier = mover::position;
        MapImpl map = new MapImpl(ctxt);
        MapLibrary.map2(map);
        Processor underTest = new Processor(mover, positionSupplier, map);
        MapViz mapViz = new MapViz(100, underTest.map, underTest.planner::getSolution, positionSupplier, () -> null);
        ctxt.visualizations.register(mapViz);
        ctxt.visualizations.redraw();

        while (true) {
            Thread.sleep(1000);
        }
    }
}
