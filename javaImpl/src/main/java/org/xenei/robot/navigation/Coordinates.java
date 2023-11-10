package org.xenei.robot.navigation;

import java.util.Comparator;

import org.apache.jena.rdf.model.Resource;

public class Coordinates {
    private Integer hashCode = null;
    private final double theta;
    private final double range;
    private final double x;
    private final double y;
    private final boolean quantized;

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

    protected Coordinates(Coordinates other) {
        this.theta = other.theta;
        this.range = other.range;
        this.x = other.x;
        this.y = other.y;
        this.quantized = other.quantized;
    }

    // bitmasking negative number check
    private static boolean isNeg(double d) {
        return (Double.doubleToLongBits(d) & 0x8000000000000000L) != 0;
    }

    private Coordinates(double theta, double range, double x, double y) {
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
        this.quantized = this.x == Math.round(this.x) && this.y == Math.round(this.y);
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
        double newX = this.x - other.x;
        double newY = this.y - other.y;
        return Math.sqrt(newX * newX + newY * newY);
    }

    public Coordinates quantize() {
        if (quantized) {
            return this;
        }
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
            result = hashCode = Double.hashCode(quantize().range);
        }
        return result.intValue();
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Coordinates) {
            Coordinates c = ((Coordinates) other).quantize();
            Coordinates q = this.quantize();
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
