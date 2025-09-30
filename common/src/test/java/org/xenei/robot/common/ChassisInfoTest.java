package org.xenei.robot.common;

import org.junit.jupiter.api.Test;
import org.xenei.robot.common.utils.AngleUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ChassisInfoTest {

    // wheel size and max speed are unused in unit tests.
    public static final ChassisInfo DEFAULT = ChassisInfo.builder().width(0.5).wheelSize(7)
            .motorInfo(new MotorInfo(0.1, 100)).build();

    /**
     * setup 4 steps to rotate the wheel with a 1M circumstance.
     */
    public static final ChassisInfo ONE_METER = ChassisInfo.builder().width(0.5).wheelSize(1 / Math.PI * 100)
            .motorInfo(new MotorInfo(0.25 * AngleUtils.PI_x_2, 100)).build();

    @Test
    void metersPerStepTest() {
        assertEquals(0.25, ONE_METER.metersPerStep);
    }

    @Test
    void stepsPerRotationTest() {
        assertEquals(4, ONE_METER.stepsPerRotation, 0.001);
    }
}
