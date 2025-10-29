package org.xenei.robot.common;

import org.locationtech.jts.geom.Coordinate;
import org.xenei.robot.common.utils.DoubleUtils;

public interface Compass {
    /**
     * Calculates the position from a point and the heading reading.
     *
     * @param location
     *            The location of the device.
     * @return A Position instance that is at the specified location and has the
     *         correct heading.
     */
    default Position getPosition(Coordinate location) {
        return Position.asPosition(location, heading());
    };

    /**
     * the current heading. This may be an averaged value over several readings and
     * may differ slightly from the current absolute heading reading.
     *
     * @return the current heading
     */
    double heading();

    /**
     * the instantaneous heading. The latest reading from the source.
     *
     * @return the current heading
     */
    double instantaneousHeading();

    /**
     * Gets the standard deviaion of the compass measurements.
     *
     * @return the standard deviation of the compass measurements
     */
    double sd();

    /**
     * Number of decimal positions of accuracy.
     *
     * @return the number of decimal digits of accuracy.
     */
    int decimalPlaces();

    /**
     * The decimal position at which changes can not be detected.
     *
     * @return the accuracy
     */
    default double accuracy() {
        return DoubleUtils.tolerance(decimalPlaces());
    }
}
