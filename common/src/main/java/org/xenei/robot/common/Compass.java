package org.xenei.robot.common;

import org.xenei.robot.common.utils.DoubleUtils;

public interface Compass {

    /**
     * the current heading. This may be an averaged value over several readings and
     * may differ slightly from the current absolute heading reading.
     *
     * @return the current heading
     */
    double heading();

    /**
     * Number of decimal positions of accuracy.
     *
     * @return the number of decimal digits of accuracy.
     */
    default int headingAccuracy() {
        return DoubleUtils.decimalPlaces(accuracy());
    }

    /**
     * The decimal position at which changes can not be detected.
     *
     * @return the accuracy
     */
    default double accuracy() {
        return DoubleUtils.DEFAULT_TOLERANCE;
    }
}
