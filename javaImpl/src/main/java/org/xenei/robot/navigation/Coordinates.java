package org.xenei.robot.navigation;

import java.util.Comparator;

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

    protected Coordinates(Coordinates other) {
        this.theta = other.theta;
        this.range = other.range;
        this.x = other.x;
        this.y = other.y;
    }

    // bitmasking negative number check
    private static boolean isNeg(double d) {
        return (Double.doubleToLongBits(d) & 0x8000000000000000L) != 0;
    }

    protected Coordinates(double theta, double range, double x, double y) {
        double t = normalize(theta);
        boolean yNeg = isNeg(y);
        boolean tNeg = isNeg(t);

        if (yNeg && !tNeg) {
            t -= Math.PI;
        } else if (!yNeg && tNeg) {
            t += Math.PI;
        }
        this.theta = t;
        this.range = range;
        this.x = x;
        this.y = y;
    }

    public double angleTo(Coordinates other) {
        // angle to
        Coordinates c = this.minus(other);
        // angle will be pointing the wrong way. So reverse it.
        double angle = normalize(c.getThetaRadians() + Math.PI);
        return angle;
    }

    public double distanceTo(Coordinates other) {
        double newX = this.x - other.x;
        double newY = this.y - other.y;
        return Math.sqrt(newX * newX + newY * newY);
    }

    private Coordinates quantized() {
        long qX = Math.round(this.x);
        long qY = Math.round(this.y);
        if (x == qX && y == qY) {
            return this;
        }
        return Coordinates.fromXY(qX, qY);
    }

    @Override
    public int hashCode() {
        Integer result = hashCode;
        if (result == null) {
            result = hashCode = Double.hashCode(quantized().range);
        }
        return result.intValue();
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Coordinates) {
            Coordinates c = ((Coordinates) other).quantized();
            Coordinates q = this.quantized();
            return q.hashCode() == c.hashCode() && q.x == c.x && q.y == c.y;
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
        return String.format("Coordinates[x:%.4f,y:%.4f r:%.4f theta:%.4f (%.4f)]", x, y, range, theta,
                getThetaDegrees());
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
