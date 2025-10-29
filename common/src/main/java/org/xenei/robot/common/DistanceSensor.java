package org.xenei.robot.common;

import org.xenei.robot.common.mapping.ThetaAndRange;

import java.util.Collection;

public interface DistanceSensor extends Runnable {

    /**
     * The origin and readings from that origin to objects.
     *
     * @param origin
     *            the origin position.
     * @param readings
     *            the collection of sensor readings from the origin.
     */
    record Readings(Location origin, Collection<Location> readings) {
    }

    /**
     * The maximum range the sensor can detect.
     *
     * @return the maximum range the sensor can detect
     */
    double maxRange();
}
