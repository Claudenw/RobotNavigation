package org.xenei.robot.common;

public interface DistanceSensor {
    /**
     * Performs a sensor scan and returns all the natural Coordinates of obstacles relative
     * to the position.
     * 
     * @return an array of Coordinates of obstacles relative to the position.
     */
    Location[] sense();
    
    /**
     * The maximum range the sensor can detect.
     * @return
     */
    double maxRange();
    
    /**
     * Get the scaling info for this sensor.
     * @return the ScaleInfo for this sensor.
     */
    ScaleInfo getScale();
}
