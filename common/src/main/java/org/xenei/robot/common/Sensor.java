package org.xenei.robot.common;

public interface Sensor {
    /**
     * Performs a sensor scan and returns all the natural Coordinates of obstacles relative
     * to the position.
     * 
     * @param position The position we are scanning from.
     * @return an array of Coordinates of obstacles relative to the position.
     */
    Coordinates[] sense(Position position);
    
    double maxRange();
}
