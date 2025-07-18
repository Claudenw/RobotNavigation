package org.xenei.robot.common;

import org.locationtech.jts.geom.Coordinate;
import org.xenei.robot.common.utils.CoordUtils;

import java.util.Collection;

public interface DistanceSensor extends Runnable {

    record DistanceReading(double theta, double range) {
        public Location getLocation() {return Location.from(CoordUtils.fromAngle(theta, range));}
        public static DistanceReading from(FrontsCoordinate location) {
            return new DistanceReading(location.theta(), location.range());
        }
        public static DistanceReading from(Coordinate coordinate) {
            return from(Location.from(coordinate));
        }
        public static DistanceReading from(double x, double y) {
            return from(Location.from(x, y));
        }
    }

    record Readings(Position origin, Collection<DistanceReading> readings){}

    /**
     * The maximum range the sensor can detect.
     * 
     * @return the maximum range the sensor can detect
     */
    double maxRange();
}
