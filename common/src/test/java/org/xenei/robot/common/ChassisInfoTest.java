package org.xenei.robot.common;

import org.junit.jupiter.api.Test;
import org.xenei.robot.common.utils.AngleUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ChassisInfoTest {

    @Test
    void metersPerStepTest() {
        // setup 4 steps to rotate the wheel with a 1M circumstance.
        double wheelDiameter = 1 / Math.PI * 100;
        double stepAngle = 0.25 * AngleUtils.PI_x_2;
        ChassisInfo chassisInfo  = ChassisInfo.builder().motorInfo(new MotorInfo(stepAngle, 100))
                        .wheelSize(wheelDiameter).build();
        assertEquals(0.25, chassisInfo.metersPerStep);
    }

    @Test
    void stepsPerRotationTest() {
        double stepAngle = 0.25 * AngleUtils.PI_x_2;
        ChassisInfo chassisInfo  = ChassisInfo.builder().motorInfo(new MotorInfo(stepAngle, 100))
                .build();
        assertEquals(4, chassisInfo.stepsPerRotation, 0.001);
    }
}
