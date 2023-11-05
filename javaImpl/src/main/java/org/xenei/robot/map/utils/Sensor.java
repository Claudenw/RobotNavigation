package org.xenei.robot.map.utils;

import org.xenei.robot.map.ActiveMap;
import org.xenei.robot.navigation.Coordinates;
import org.xenei.robot.navigation.Position;

public interface Sensor {
    Coordinates[] sense(Position position);
}
