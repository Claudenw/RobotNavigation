package org.xenei.robot.common.testUtils;

import org.xenei.robot.common.DistanceSensor;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.ScaleInfo;
import org.xenei.robot.common.mapping.CoordinateMap;

public interface FakeDistanceSensor extends DistanceSensor {
    
    void setPosition(Position position);
    
    CoordinateMap map();
}
