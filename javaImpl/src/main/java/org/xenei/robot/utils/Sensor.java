package org.xenei.robot.utils;

import org.xenei.robot.navigation.Coordinates;
import org.xenei.robot.navigation.Position;

public interface Sensor {
    /**
     * Performs a sensor scan and returns all the natural Coordinates of obstacles relative
     * to the position.
     * 
     * @param position The position we are scanning from.
     * @return an array of Coordinates of obstacles relative to the position.
     */
    Coordinates[] sense(Position position);
}
