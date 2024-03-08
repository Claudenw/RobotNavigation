package org.xenei.robot.common.testUtils;

import org.xenei.robot.common.DistanceSensor;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.mapping.CoordinateMap;

public interface FakeDistanceSensor extends DistanceSensor {
    CoordinateMap map();
}
