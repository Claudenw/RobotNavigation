package org.xenei.robot.common.testUtils;

import org.xenei.robot.common.DistanceSensor;
import org.xenei.robot.common.mapping.Map;

public interface FakeDistanceSensor extends DistanceSensor {
    Map map();
}
