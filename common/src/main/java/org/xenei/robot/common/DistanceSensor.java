package org.xenei.robot.common;

import org.locationtech.jts.geom.Coordinate;
import org.xenei.robot.common.utils.CoordUtils;

import java.util.function.Consumer;

public interface DistanceSensor extends Runnable {

    record DistanceReading(double theta, double range) {
        public Location getLocation() {return Location.from(CoordUtils.fromAngle(theta, range));}
        static DistanceReading from(Location location) {
            return new DistanceReading(location.theta(), location.range());
        }
        public static DistanceReading from(Coordinate coordinate) {
            return from(Location.from(coordinate));
        }
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
