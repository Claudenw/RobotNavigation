package org.xenei.robot;

import java.util.function.Supplier;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.Processor.AbortTest;
import org.xenei.robot.common.AbortedException;
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

    private void doTest(Location startCoord, Location finalCoord, Mover mover, DistanceSensor sensor)
            throws AbortedException {
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
        Map m = new MapImpl(new RobutContext(ScaleInfo.DEFAULT, TestChassisInfo.DEFAULT));
        DistanceSensor sensor = new FakeDistanceSensor1(MapLibrary.map2(m), mover::position);
        Location finalCoord = Location.from(-1, 1);
        doTest(startCoord, finalCoord, mover, sensor);
    }

    @Test
    @Disabled
    public void stepTestMap3() throws AbortedException {
        Location startCoord = Location.from(-1, -3);
        Mover mover = new FakeMover(Location.from(startCoord), 1);
        Map m = new MapImpl(new RobutContext(ScaleInfo.DEFAULT, TestChassisInfo.DEFAULT));
        DistanceSensor sensor = new FakeDistanceSensor2(MapLibrary.map3(m), AngleUtils.RADIANS_45, mover::position);
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
