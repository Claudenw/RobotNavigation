package org.xenei.robot.common;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xenei.robot.common.testUtils.FakeStepMonitor;
import org.xenei.robot.common.utils.RobutContext;

public class DeadReckoningTest {
    private RobutContext ctxt;
    private ScaleInfo scaleInfo;

    @BeforeEach
    void beforeDeadReckoningTest() {
        ctxt = new RobutContext(ScaleInfo.DEFAULT, ChassisInfoTest.ONE_METER);
        scaleInfo = ctxt.scaleInfo;
    }

    @Test
    void constructorTest() {
        DeadReckoning deadReckoning = new DeadReckoning(ctxt);
        FrontsCoordinateTest.assertEquals(Position.ORIGIN, deadReckoning.get(), scaleInfo);
        Assertions.assertEquals(0.0, deadReckoning.heading(), scaleInfo.getResolution());
        Assertions.assertEquals(2, deadReckoning.decimalPlaces());

        Position position = Position.from( 5.5, 3.1, 7.8);
        deadReckoning = new DeadReckoning(ctxt, position);
        FrontsCoordinateTest.assertEquals(position, deadReckoning.get(), scaleInfo);
        Assertions.assertEquals(7.8, deadReckoning.heading(), scaleInfo.getResolution());
        Assertions.assertEquals(2, deadReckoning.decimalPlaces());
    }


    @Test
    void instantaneousHeadingTest() {
        Position position = Position.ORIGIN;
        FakeStepMonitor fakeStepMonitor = new FakeStepMonitor(ctxt, 5, 5);
        DeadReckoning deadReckoning = new DeadReckoning(ctxt, position);
        deadReckoning.track(fakeStepMonitor);
        fakeStepMonitor.takeStep();
        FrontsCoordinateTest.assertEquals(Position.ORIGIN, deadReckoning.get(), scaleInfo);
        fakeStepMonitor.takeStep();
        FrontsCoordinateTest.assertEquals(Position.from(1,0, 0), deadReckoning.get(), scaleInfo);
    }
}
