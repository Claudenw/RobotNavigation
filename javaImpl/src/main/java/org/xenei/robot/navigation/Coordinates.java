package org.xenei.robot.navigation;

import java.util.Comparator;

import org.apache.commons.math3.util.Precision;

public class Coordinates {

    private Integer hashCode = null;
    private final double theta;
    private final double range;
    private final double x;
    private final double y;

    public static Comparator<Coordinates> XYCompr = (one, two) -> {
        int x = Double.compare(one.x, two.x);
        return x == 0 ? Double.compare(one.y, two.y) : x;
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
        return Math.atan2(Math.sin(angle), Math.cos(angle));
    }

    protected static final double normalAtan(double x, double y) {
        double theta = Math.atan(y / x);
        if (Double.isNaN(theta)) {
            return 0.0;
        }
        return theta;
    }

    public static final Coordinates fromDegrees(double theta, double r) {
        return fromRadians(Math.toRadians(theta), r);
    }

    public static final Coordinates fromRadians(double theta, double r) {
        return new Coordinates(normalize(theta), r, r * Math.cos(theta), r * Math.sin(theta));
    }

    public static final Coordinates fromXY(double x, double y) {
        return new Coordinates(normalAtan(x, y), Math.sqrt(x * x + y * y), x, y);
    }

    protected Coordinates(Coordinates other) {
        this.theta = other.theta;
        this.range = other.range;
        this.x = other.x;
        this.y = other.y;
    }

    protected Coordinates(double theta, double range, double x, double y) {
        this.theta = Double.isNaN(theta) ? 0.0 : theta;
        this.range = range;
        this.x = x;
        this.y = y;
    }

    public double angleTo(Coordinates other) {
        return normalAtan(this.x - other.x, this.y - other.y);
    }

    public double distanceTo(Coordinates other) {
        double newX = this.x - other.x;
        double newY = this.y - other.y;
        return Math.sqrt(newX * newX + newY * newY);
    }

    @Override
    public int hashCode() {
        Integer result = hashCode;
        if (result == null) {
            result = hashCode = Double.hashCode(range);
        }
        return result.intValue();
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Coordinates) {
            Coordinates c = (Coordinates) other;
            return Precision.equals(Double.hashCode(range), Double.hashCode(c.range), 0.5)
                    && Precision.equals(distanceTo(c), 0.0, 0.5);
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

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    @Override
    public String toString() {
        return String.format("Coordinates[x:%s,y:%s r:%s theta:%s (%s)]", x, y, range, theta, getThetaDegrees());
    }

    public final Coordinates plus(Coordinates other) {
        double x = this.x + other.x;
        double y = this.y + other.y;
        return Coordinates.fromXY(x, y);
    }

    public final Coordinates minus(Coordinates other) {
        double x = this.x - other.x;
        double y = this.y - other.y;
        return Coordinates.fromXY(x, y);
    }
}
