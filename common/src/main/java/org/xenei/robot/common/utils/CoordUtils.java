package org.xenei.robot.common.utils;

import java.util.Comparator;

import org.apache.commons.math3.util.Precision;
import org.locationtech.jts.geom.Coordinate;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.DoubleUtils;
import org.xenei.robot.common.FrontsCoordinate;

public class CoordUtils {
    private CoordUtils() {}
    
    public static Comparator<Coordinate> XYCompr = (one, two) -> {
        int x = Double.compare(one.getX(), two.getX());
        return x == 0 ? Double.compare(one.getY(), two.getY()) : x;
    };
    
    public static String toString(Coordinate p) {
        return String.format("{%f,%f}", p.getX(), p.getY());
    }

    public static String toString(Coordinate p, int precision) {
        String fmt = String.format("{%%.%sf, %%.%sf}", precision, precision);
        return String.format(fmt, p.getX(), p.getY());
    }

    public static String toString(FrontsCoordinate p) {
        return String.format("{%f,%f}", p.getX(), p.getY());
    }

    public static String toString(FrontsCoordinate p, int precision) {
        String fmt = String.format("{%%.%sf, %%.%sf}", precision, precision);
        return String.format(fmt, p.getX(), p.getY());
    }
    
    /**
     * Set the coordinates with angle in radians and distance from origin.
     * @param theta the angle in radians from the origin.
     * @param range the range from the origin.
     * @return a new Coordinats instance.
     */
    public static final Coordinate fromAngle(double theta,  double range) {
        return new Coordinate(range * Math.cos(theta), range * Math.sin(theta));
    }
}
