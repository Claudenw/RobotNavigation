package org.xenei.robot.common;

import java.util.Comparator;

import org.apache.commons.math3.util.Precision;
import org.xenei.robot.common.utils.PointUtils;

import mil.nga.sf.Point;


public class Coordinates extends Point {
    private final double theta;
    private final double range;
    private final Point quantized;
    // experimentally ascertained in PositionTest.collisiontTest.
    public final static double POINT_RADIUS = 0.57;

    public static Comparator<Coordinates> XYCompr = (one, two) -> {
        return PointUtils.XYCompr.compare(one, two);
    };

    public static Comparator<Coordinates> ThetaCompr = (one, two) -> {
        int x = Double.compare(one.theta, two.theta);
        return x == 0 ? Double.compare(one.range, two.range) : x;
    };

    public static Comparator<Coordinates> RangeCompr = (one, two) -> {
        int x = Double.compare(one.range, two.range);
        return x == 0 ? Double.compare(one.theta, two.theta) : x;
    };

    public static final Coordinates fromAngle(double theta, AngleUnits units, double r) {
        if (units == AngleUnits.DEGREES) {
            theta = Math.toRadians(theta);
        }
        return new Coordinates(theta, r, r * Math.cos(theta), r * Math.sin(theta));
    }
    
//    public static final Coordinates fromDegrees(double theta, double r) {
//        return fromRadians(Math.toRadians(theta), r);
//    }
//
//    public static final Coordinates fromRadians(double theta, double r) {
//        return new Coordinates(theta, r, r * Math.cos(theta), r * Math.sin(theta));
//    }

    public static final Coordinates fromXY(double x, double y) {
        return new Coordinates(x, y);
    }

    public static final Coordinates fromXY(Point p) {
        return p instanceof Coordinates ? (Coordinates) p : fromXY(p.getX(), p.getY());
    }
    
    protected Coordinates(Coordinates other) {
        super(other.getX(), other.getY());
        this.theta = other.theta;
        this.range = other.range;
        this.quantized = other.quantized;
    }
    
    protected Coordinates(Point p) {
        this(p.getX(), p.getY());
    }
    
    protected Coordinates(double x, double y) {
        this(Math.atan(y / x), Math.sqrt(x * x + y * y), x, y);
    }

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

    public boolean isQuantized() {
        return this.quantized == this;
    }

    public double angleTo(Point p) {
        Coordinates c = this.minus(p);
        // vector will be pointing the wrong way. So reverse it.
        return AngleUtils.normalize(c.getTheta(AngleUnits.RADIANS) + Math.PI);
    }

    public double distanceTo(Point other) {
        return PointUtils.distance(this, other);
    }

    public Coordinates quantize() {
        return new Coordinates(quantized);
    }

    public double getTheta(AngleUnits units) {
        return (units == AngleUnits.DEGREES) ?
            Math.toDegrees(theta) : theta;
    }

//    public double getThetaRadians() {
//        return theta;
//    }
//
//    public double getThetaDegrees() {
//        return Math.toDegrees(theta);
//    }

    public double getRange() {
        return range;
    }

    public Point asPoint() {
        return (Point)this;
    }
    
//    public double getX() {
//        return point.x;
//    }
//
//    public double getY() {
//        return point.y;
//    }

    @Override
    public String toString() {
        return String.format("Coordinates[ %s ]", PointUtils.toString(this,4));
    }

    public final Coordinates plus(Point other) {
        return Coordinates.fromXY(PointUtils.plus(this, other));
    }

    public final Coordinates minus(Point other) {
        return Coordinates.fromXY(PointUtils.minus(this, other));
    }

    public final boolean overlap(Coordinates other, double range) {
        double distance = distanceTo(other);
        return Precision.equals(distance, 0, range + Precision.EPSILON);
    }
}
