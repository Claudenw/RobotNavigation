package org.xenei.robot.common.testUtils;

import org.xenei.robot.common.DistanceSensor;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.mapping.Map;
import org.xenei.robot.mapper.visualization.TextViz;

public interface FakeDistanceSensor extends DistanceSensor {
    Map map();
}
