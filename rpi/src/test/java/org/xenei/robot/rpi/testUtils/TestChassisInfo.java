package org.xenei.robot.rpi.testUtils;

import org.xenei.robot.common.ChassisInfo;
import org.xenei.robot.common.MotorInfo;

public class TestChassisInfo {
    // wheel size and max speed are unused in unit tests.
    public static final ChassisInfo DEFAULT = ChassisInfo.builder().width(0.5).wheelSize(70)
            .motorInfo(new MotorInfo(0.1, 100)).build();
    // new ChassisInfo(0.5, 70, 60, .1);
}
