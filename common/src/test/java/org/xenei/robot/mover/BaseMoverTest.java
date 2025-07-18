package org.xenei.robot.mover;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.xenei.robot.common.ChassisInfoTest;
import org.xenei.robot.common.DeadReckoning;
import org.xenei.robot.common.FrontsCoordinateTest;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.Mover;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.ScaleInfo;
import org.xenei.robot.common.sensor.bump.BumpSensorModel;
import org.xenei.robot.common.utils.AngleUtils;
import org.xenei.robot.common.utils.RobutContext;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

public class BaseMoverTest {
    protected record StepRecord(int left, int right, byte lastSensor){};

    protected BaseMover underTest;
    protected List<StepRecord> stepRecords = new ArrayList<>();
    protected RobutContext ctxt;
    protected DeadReckoning deadReckoning;
    protected BumpSensorModel bumpSensorModel;
    protected ScaleInfo scaleInfo;

    protected BaseMover getInstance() {
        scaleInfo = ScaleInfo.DEFAULT;
        ctxt = new RobutContext(scaleInfo, ChassisInfoTest.DEFAULT);
        deadReckoning = new DeadReckoning(ctxt);
        bumpSensorModel = new BumpSensorModel(ctxt, 8);
        return new BaseMover(ctxt, deadReckoning, bumpSensorModel) {
            @Override
            public Position position() {
                return deadReckoning.get();
            }

            @Override
            public void takeSteps(int left, int right, byte lastSensor) {
                stepRecords.add(new StepRecord(left, right, lastSensor));
            }
        };
    }

    @BeforeEach
    void setupBaseMoverTest() {
        underTest = getInstance();
    }

    private class TestLogicModule implements Mover.LogicModule {
        Lock lock;
        @Override
        public void setLock(Lock lock) {
            this.lock = lock;
        }
    }

    @Test
    public void LogicModuleTest() {
        TestLogicModule logicModule = new TestLogicModule();
        underTest.register(logicModule);
        assertNotNull(logicModule.lock);
        fail("What should this test");
    }

    @Test
    void getMotorStateTest() {
        assertEquals(Mover.MotorState.STOP, underTest.getMotorState());
        for (Mover.MotorState state : Mover.MotorState.values()) {
            underTest.motorState.set(state);
            assertEquals(state, underTest.getMotorState());
        }
    }

    @Test
    void moveTest() {
        underTest.move(Location.from(0, 10));
        FrontsCoordinateTest.assertEquals(Position.from(0, 10, AngleUtils.RADIANS_90), deadReckoning.get(), scaleInfo);
        assertEquals(2, stepRecords.size());
        /*
        @Override
        final public void move(FrontsCoordinate location) {
            Position currentPosition = position();
            Position nxt = currentPosition.nextPosition(location);
            setHeading(currentPosition.headingTo(nxt));
            int rangeSteps = ctxt.chassisInfo.steps(location.range());
            takeSteps(rangeSteps, rangeSteps, (byte)0);
        }
*/
        fail("not implemented yet");
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.0, AngleUtils.RADIANS_45, AngleUtils.RADIANS_90, AngleUtils.RADIANS_135, AngleUtils.RADIANS_180,
            AngleUtils.RADIANS_225, AngleUtils.RADIANS_270, AngleUtils.RADIANS_315})
    void setHeadingTest(double heading) {
        underTest.setHeading(heading);
        assertEquals(Position.from(Location.ORIGIN, heading), deadReckoning.get());
    }


    @Test
    void takeStepsTest() {
        underTest.takeSteps(1, 10, (byte) 0);
        underTest.takeSteps(10, 5, (byte) 1);
        assertEquals(2, stepRecords.size());
        assertEquals(1, stepRecords.get(0).left);
        assertEquals(10, stepRecords.get(0).right);
        assertEquals((byte)0, stepRecords.get(0).lastSensor);
        assertEquals(10, stepRecords.get(1).left);
        assertEquals(5, stepRecords.get(1).right);
        assertEquals((byte)1, stepRecords.get(1).lastSensor);
    }
}
