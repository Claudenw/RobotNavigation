package org.xenei.robot.common.utils;

import java.util.Comparator;

import org.locationtech.jts.geom.Coordinate;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.mapping.MapCoordinate;
import org.xenei.robot.common.mapping.ThetaAndRange;

public final class CoordUtils {
    private CoordUtils() {
    }

    public static Comparator<Coordinate> XYCompr = (one, two) -> one.compareTo(two);

    public static String toString(Coordinate p) {
        return String.format("{%f,%f}", p.getX(), p.getY());
    }

    public static String toString(Coordinate p, int precision) {
        String fmt = String.format("{%%.%sf, %%.%sf}", precision, precision);
        return String.format(fmt, p.getX(), p.getY());
    }

    public static String toString(MapCoordinate p) {
        return String.format("{%f,%f}", p.getX(), p.getY());
    }

    public static String toString(MapCoordinate p, int precision) {
        String fmt = String.format("{%%.%sf, %%.%sf}", precision, precision);
        return String.format(fmt, p.getX(), p.getY());
    }

    /**
     * Set the coordinates with angle in radians and distance from origin.
     *
     * @param theta
     *            the angle in radians from the origin.
     * @param range
     *            the range from the origin.
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
    public static Coordinate add(Coordinate a, Coordinate b) {
        return new Coordinate(a.getX() + b.getX(), a.getY() + b.getY());
    }

    /**
     * Calculates a - b;
     *
     * @param a
     * @param b
     * @return
     */
    public static Coordinate subtract(Coordinate a, Coordinate b) {
        return new Coordinate(a.getX() - b.getX(), a.getY() - b.getY());
    }

    /**
     * Caclulates the theta of a line passing through the 2 points.
     * @param a the first point
     * @param b the second point.
     * @return the angle of the line passing thorugh the points.
     */
    public static double angleBetween(Coordinate a, Coordinate b) {
        Coordinate diff = subtract(a, b);
        if (diff.getX() == 0 && diff.getY() == 0) {
            return 0;
        }
        double theta = Math.atan(diff.getY() / diff.getX());
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

    public static double calcHeading(Coordinate from, Coordinate to) {
        return AngleUtils.normalize(Math.atan2(to.getY() - from.getY(), to.getX() - from.getX()));
    }

    public static double calcHeading(MapCoordinate from, MapCoordinate to) {
        return calcHeading(from.getCoordinate(), to.getCoordinate());
    }

    /**
     * Returns true if the Coordinate represents a point of infinite distance.
     *
     * @param coord
     *            the Location to check.
     * @return true if the location is not finite, false otherwise.
     */
    public static boolean isInfinite(Coordinate coord) {
        return !(Double.isFinite(coord.getX()) && Double.isFinite(coord.getY()));
    }

    /**
     * Returns true if the Coordinate represents a point of infinite distance.
     *
     * @param coord
     *            the Location to check.
     * @return true if the location is not finite, false otherwise.
     */
    public static boolean isNaN(Coordinate coord) {
        return (Double.isNaN(coord.getX()) || Double.isNaN(coord.getY()));
    }

    public static Coordinate minus(Coordinate a, Coordinate b) {
        return new Coordinate(a.getX() - b.getX(), a.getY() - b.getY());
    }

    public static Coordinate plus(Coordinate a, Coordinate b) {
        return new Coordinate(a.getX() + b.getX(), a.getY() + b.getY());
    }

    public static Coordinate nextCoordinate(Coordinate a, double heading, double scaledRange) {
        return plus(a, CoordUtils.fromAngle(heading, scaledRange));
    }

    public static Coordinate nextCoordinate(Coordinate a, ThetaAndRange thetaAndRange) {
        return nextCoordinate(a, thetaAndRange.theta(), thetaAndRange.range());
    }
}
