package org.xenei.robot.common;

public interface DistanceSensor {
    /**
     * Performs a sensor scan and returns all Coordinates of obstacles
     * relative to the current position.
     * 
     * @return an array of Coordinates of obstacles relative to the current position.
     */
    Location[] sense();

    /**
     * The maximum range the sensor can detect.
     * 
     * @return
     */
    double maxRange();
}
