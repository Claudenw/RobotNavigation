package org.xenei.robot.common;

import java.util.function.Consumer;

public interface DistanceSensor {

    record DistanceReading(double theta, double range) {
    }

//    /**
//     * Performs a sensor scan and returns all Coordinates of obstacles
//     * relative to the current position.  If no object is detected an
//     * infinite location should be returned.
//     *
//     * @return an array of Coordinates of obstacles relative to the current position.
//     * @see Location#INFINITE
//     */
//    Location[] sense();

    /**
     * The maximum range the sensor can detect.
     * 
     * @return the maximum range the sensor can detect
     */
    double maxRange();

    void addListener(Consumer<DistanceReading> listener);

    void removeListener(Consumer<DistanceReading> listener);
}
