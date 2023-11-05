package org.xenei.robot.utils;

import org.xenei.robot.navigation.Coordinates;
import org.xenei.robot.navigation.Position;

public interface Sensor {
    // returns location of first obstruction relative to position
    Coordinates[] sense(Position position);
}
