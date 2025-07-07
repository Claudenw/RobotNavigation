package org.xenei.robot.common.testUtils;

import org.xenei.robot.common.ChassisInfo;

public class TestChassisInfo  {
    // wheel size and max speed are unused in unit tests.
    public static final ChassisInfo DEFAULT = ChassisInfo.builder()
            .width(0.5).wheelSize(7).stepAngle(0.1).motorFreq(100).build();
}
