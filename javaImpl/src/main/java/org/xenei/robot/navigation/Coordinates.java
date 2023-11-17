package org.xenei.robot.navigation;

import java.util.Comparator;

import org.apache.commons.math3.util.Precision;
import org.xenei.robot.utils.DoubleUtils;

public class Coordinates {
    private Integer hashCode = null;
    private final double theta;
    private final double range;
    private final Point point;
    private final boolean quantized;
    // experimentally ascertained in PositionTest.collisiontTest.
    public final static double POINT_RADIUS = 0.57;

    public static Comparator<Coordinates> XYCompr = (one, two) -> {
        int x = Double.compare(one.point.x, two.point.x);
        return x == 0 ? Double.compare(one.point.y, two.point.y) : x;
    };

    public static Comparator<Coordinates> ThetaCompr = (one, two) -> {
        int x = Double.compare(one.theta, two.theta);
        return x == 0 ? Double.compare(one.range, two.range) : x;
    };

    public static Comparator<Coordinates> RangeCompr = (one, two) -> {
        int x = Double.compare(one.range, two.range);
        return x == 0 ? Double.compare(one.theta, two.theta) : x;
    };

    public static final double normalize(double angle) {
        if (Double.isNaN(angle)) {
            return 0.0;
        }
        // should this account for NaN?
        return Math.atan2(Math.sin(angle), Math.cos(angle));
    }

    public static final Coordinates fromDegrees(double theta, double r) {
        return fromRadians(Math.toRadians(theta), r);
    }

    public static final Coordinates fromRadians(double theta, double r) {
        return new Coordinates(theta, r, r * Math.cos(theta), r * Math.sin(theta));
    }

    public static final Coordinates fromXY(double x, double y) {
        return new Coordinates(Math.atan(y / x), Math.sqrt(x * x + y * y), x, y);
    }

    public static final Coordinates fromXY(Point p) {
        return fromXY(p.x, p.y);
    }
    
    protected Coordinates(Coordinates other) {
        this.theta = other.theta;
        this.range = other.range;
        this.point = other.point;
        this.quantized = other.quantized;
    }

    private Coordinates(double theta, double range, double x, double y) {
        double t = normalize(theta);
        boolean yNeg = DoubleUtils.isNeg(y);
        boolean tNeg = DoubleUtils.isNeg(t);

        if (yNeg && !tNeg) {
            t -= Math.PI;
        } else if (!yNeg && tNeg) {
            t += Math.PI;
        }
        this.theta = t;
        this.range = range;
        this.point = new Point(x,y);
        this.quantized = this.point.x == Math.round(this.point.x) && this.point.y == Math.round(this.point.y);
    }

    public boolean isQuantized() {
        return this.quantized;
    }

    public double angleTo(Coordinates other) {
        Coordinates c = this.minus(other);
        // vector will be pointing the wrong way. So reverse it.
        return normalize(c.getThetaRadians() + Math.PI);
    }

    public double distanceTo(Coordinates other) {
        double newX = this.point.x - other.point.x;
        double newY = this.point.y - other.point.y;
        return Math.sqrt(newX * newX + newY * newY);
    }

    public Coordinates quantize() {
        if (quantized) {
            return this;
        }
        long qX = Math.round(point.x);
        long qY = Math.round(point.y);
        if (point.x == qX && point.y == qY) {
            return this;
        }
        return Coordinates.fromXY(qX, qY);
    }

    @Override
    public int hashCode() {
        Integer result = hashCode;
        if (result == null) {
            result = hashCode = Double.hashCode(quantize().range);
        }
        return result.intValue();
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Coordinates) {
            Coordinates c = ((Coordinates) other).quantize();
            Coordinates q = this.quantize();
            return q.hashCode() == c.hashCode() && q.point.equals(c.point);
        }
        return false;
    }

    public double getThetaRadians() {
        return theta;
    }

    public double getThetaDegrees() {
        return Math.toDegrees(theta);
    }

    public double getRange() {
        return range;
    }

    public Point getPoint() {
        return point;
    }
    
    public double getX() {
        return point.x;
    }

    public double getY() {
        return point.y;
    }

    @Override
    public String toString() {
        return String.format("Coordinates[ %s ]", point.toString(4));
    }

    public final Coordinates plus(Coordinates other) {
        return Coordinates.fromXY(this.point.plus(other.point));
    }

    public final Coordinates minus(Coordinates other) {
        return Coordinates.fromXY(this.point.minus(other.point));
    }

    public final boolean overlap(Coordinates other, double range) {
        double distance = distanceTo(other);
        return Precision.equals(distance, 0, range + Precision.EPSILON);
    }
}
