package org.xenei.robot.navigation;

public class Coordinates {

    private Integer hashCode = null;
    private final double theta;
    private final double range;
    private final double x;
    private final double y;

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
            Coordinates otherC = (Coordinates) other;
            return x == otherC.x && y == otherC.y;
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
        return String.format( "Coordinates[x:%s,y:%s r:%s theta:%s (%s)]", x, y, range, theta, getThetaDegrees());
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
