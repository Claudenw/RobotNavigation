package org.xenei.robot.rpi.testUtils;

import org.xenei.robot.common.ChassisInfo;

public class TestChassisInfo  {
    // wheel size and max speed are unused in unit tests.
    public static final ChassisInfo DEFAULT = new ChassisInfo(0.5, 70, 60);
}
