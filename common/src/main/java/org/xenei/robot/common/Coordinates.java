package org.xenei.robot.common;

import java.util.Comparator;

import org.apache.commons.math3.util.Precision;
import org.xenei.robot.common.utils.AngleUtils;
import org.xenei.robot.common.utils.PointUtils;

import mil.nga.sf.Point;

/**
 * An immutable definition of a set of Coordinates (X, Y) or (theta, r) values.
 * Holds both representations.
 */
public class Coordinates extends Point {
    private final double theta;
    private final double range;
    private final Point quantized;
    // experimentally ascertained in PositionTest.collisiontTest.
    public final static double POINT_RADIUS = 0.57;

    /**
     * Compares Coordinates by XY positions.
     * @see PointUtils#XYCompr
     */
    public static Comparator<Coordinates> XYCompr = (one, two) -> {
        return PointUtils.XYCompr.compare(one, two);
    };

    /**
     * Compares Coordinates by angle and then range.
     */
    public static Comparator<Coordinates> ThetaCompr = (one, two) -> {
        int x = Double.compare(one.theta, two.theta);
        return x == 0 ? Double.compare(one.range, two.range) : x;
    };

    /**
     * Compares Coordinates by range and then angle.
     */
    public static Comparator<Coordinates> RangeCompr = (one, two) -> {
        int x = Double.compare(one.range, two.range);
        return x == 0 ? Double.compare(one.theta, two.theta) : x;
    };

    /**
     * Set the coordinates with angle in radians and distance from origin.
     * @param theta the angle in radians from the origin.
     * @param range the range from the origin.
     * @return a new Coordinats instance.
     */
    public static final Coordinates fromAngle(double theta,  double range) {
        return new Coordinates(theta, range, range * Math.cos(theta), range * Math.sin(theta));
    }

    /**
     * Constructs a Coordinates from the X and Y positions.
     * @param x the X position
     * @param y the Y position.
     * @return a new Coordinates instance.
     */
    public static final Coordinates fromXY(double x, double y) {
        return new Coordinates(x, y);
    }

    /**
     * Constructs a Coordinates from a Point instance.
     * @param p the point representing the X and Y positions.
     * @return a new coordinates instance.
     */
    public static final Coordinates fromXY(Point p) {
        return p instanceof Coordinates ? (Coordinates) p : fromXY(p.getX(), p.getY());
    }
    
    /**
     * Copy constructor for coordinates.
     * @param other the other coordinates to copy.
     */
    protected Coordinates(Coordinates other) {
        super(other.getX(), other.getY());
        this.theta = other.theta;
        this.range = other.range;
        this.quantized = other.quantized;
    }
    
    /**
     * Construct coordinates from a Point.
     * @param p The point representing the X and Y positions
     */
    protected Coordinates(Point p) {
        this(p.getX(), p.getY());
    }
    
    /**
     * Construct coordinates from X and Y positions.
     * @param x the X position.
     * @param y the Y position.
     */
    protected Coordinates(double x, double y) {
        this(Math.atan(y / x), Math.sqrt(x * x + y * y), x, y);
    }

    /**
     * Constructs coordinates from all the variables.
     * @param theta the angle from the origin.
     * @param range the range fro mthe origin.
     * @param x the X value.
     * @param y the Y value.
     */
    private Coordinates(double theta, double range, double x, double y) {
        super(x,y);
        double t = AngleUtils.normalize(theta);
        boolean yNeg = DoubleUtils.isNeg(y);
        boolean tNeg = DoubleUtils.isNeg(t);

        if (yNeg && !tNeg) {
            t -= Math.PI;
        } else if (!yNeg && tNeg) {
            t += Math.PI;
        }
        this.theta = t;
        this.range = range;
        long qX = Math.round(getX());
        long qY = Math.round(getY());
        quantized = (x == qX && y == qY) ? this : new Point(qX, qY);
    }

    /**
     * True if the coordinates are quantized.
     * @return true if this Coordinate is quantized
     */
    public boolean isQuantized() {
        return this.quantized == this;
    }

    /**
     * Calculate the heading from this coordinate to the point.
     * @param point the heading to calculate angle to.
     * @return the heading to the point.
     */
    public double headingTo(Point point) {
        Coordinates c = this.minus(point);
        // vector will be pointing the wrong way. So reverse it.
        return AngleUtils.normalize(c.getTheta() + Math.PI);
    }

    /**
     * Calcualte the distance to the the specified point.
     * @param point the point to calculate the distance to.
     * @return the distance to the point.
     */
    public double distanceTo(Point point) {
        return PointUtils.distance(this, point);
    }

    /**
     * Quantize these coordinates.
     * @return the quantized coordinates.
     */
    public Coordinates quantize() {
        return new Coordinates(quantized);
    }

    /**
     * Return the angle in radians from the origin.
     * @return the angle in radians from the origin to this coordinates.
     */
    public double getTheta() {
        return theta;
    }

    /**
     * Get the range to the coordinates in meters.
     * @return the range to the coordinates.
     */
    public double getRange() {
        return range;
    }

    /**
     * Get the Point representation of this location.
     * @return a Point instance of this locaiton.
     */
    public Point asPoint() {
        return this;
    }

    @Override
    public String toString() {
        return String.format("Coordinates[ %s ]", PointUtils.toString(this,4));
    }

    /**
     * Add the other coordinates to these coordinates to create a new set of coordinates.
     * This effectively adds the X and Y values of this and the point together.
     * @param point the point to add to this coordinates.
     * @return a new set of coordinates.
     * @see PointUtils#plus(Point, Point)
     */
    public final Coordinates plus(Point point) {
        return Coordinates.fromXY(PointUtils.plus(this, point));
    }

    /**
     * Subtract the other coordinates from these coordinates.
     * @param other the point to subtract from this coordinates.
     * @return the new Coordinates instance.
     */
    public final Coordinates minus(Point other) {
        return Coordinates.fromXY(PointUtils.minus(this, other));
    }

    public boolean sameAs(Point other, double delta) {
        //return Precision.equals(one.getX(), other.getX(), delta) &&
        //        Precision.equals(one.getY(), other.getY(), delta);
        return Precision.equals( this.distanceTo(other), delta);
    }

//    public final boolean overlap(Coordinates other, double range) {
//        double distance = distanceTo(other);
//        return Precision.equals(distance, 0, range + Precision.EPSILON);
//    }
}
