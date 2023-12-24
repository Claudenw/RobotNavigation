package org.xenei.robot.common.utils;

import java.util.Comparator;

import org.locationtech.jts.geom.Coordinate;
import org.xenei.robot.common.FrontsCoordinate;

public class CoordUtils {
    private CoordUtils() {
    }

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
     * 
     * @param theta the angle in radians from the origin.
     * @param range the range from the origin.
     * @return a new Coordinats instance.
     */
    public static final Coordinate fromAngle(double theta, double range) {
        return new Coordinate(range * Math.cos(theta), range * Math.sin(theta));
    }

    /**
     * Calculates a+b
     * 
     * @param a
     * @param b
     * @return
     */
    public static final Coordinate add(Coordinate a, Coordinate b) {
        return new Coordinate(a.getX() + b.getX(), a.getY() + b.getY());
    }

    /**
     * Calculates a - b;
     * 
     * @param a
     * @param b
     * @return
     */
    public static final Coordinate subtract(Coordinate a, Coordinate b) {
        return new Coordinate(a.getX() - b.getX(), a.getY() - b.getY());
    }

    public static double angleBetween(Coordinate a, Coordinate b) {
        Coordinate diff = subtract(a, b);
        if (diff.getX() == 0 && diff.getY() == 0) {
            return 0;
        }
        double theta = AngleUtils.normalize(Math.atan(diff.getY() / diff.getX()));
        boolean yNeg = DoubleUtils.isNeg(diff.getY());
        boolean tNeg = DoubleUtils.isNeg(theta);

        if (yNeg && !tNeg) {
            theta -= Math.PI;
        } else if (!yNeg && tNeg) {
            theta += Math.PI;
        }
        // angle will be pointing the wrong way, so reverse it.
        return AngleUtils.normalize(theta + Math.PI);
    }
}
