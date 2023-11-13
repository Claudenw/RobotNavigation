package org.xenei.robot.navigation;

import org.apache.commons.math3.util.Precision;
import org.xenei.robot.utils.DoubleUtils;

public class Position {
    public static final double DELTA = 0.0000000001;

    private double heading;
    private final Coordinates coordinates;

    public Position() {
        this(0.0, 0.0);
    }

    public Position(Coordinates coord) {
        this(coord, 0.0);
    }

    public Position(Coordinates coord, double heading) {
        this.coordinates = coord;
        this.heading = heading;
    }

    public Position(double x, double y) {
        this(x, y, 0.0);
    }

    public Position(double x, double y, double heading) {
        this.coordinates = Coordinates.fromXY(x, y);
        this.heading = heading;
    }

    public Position quantize() {
        return coordinates.isQuantized() ? this :
            new Position( coordinates.quantize(), heading );
    }
    
    public Coordinates coordinates() {
        return coordinates;
    }

    public double getHeadingRadians() {
        return heading;
    }

    public double getHeadingDegrees() {
        return Math.toDegrees(heading);
    }

    public void setHeading(double radians) {
        this.heading = radians;
    }

    /**
     * Calculates the next position.
     * @param cmd The relative coordinates to move to.
     * @return the new Position with a heading the same as the cmd.
     */
    public Position nextPosition(Coordinates cmd) {
        if (cmd.getRange() == 0) {
            return new Position(this.coordinates.getX(), this.coordinates.getY(), cmd.getThetaRadians());
        }
        Coordinates nextCoord = this.coordinates.plus(cmd);
        return new Position(nextCoord, cmd.getThetaRadians());
    }

    @Override
    public String toString() {
        return String.format("Position[ %s heading:%.4f ]", coordinates.toString(), Math.toDegrees(heading));
    }
    
    /**
     * Checks if this postion will srike the obstacle within the specified distance.
     * @param obstacle the obstacle to check.
     * @param radius the size of the obstacle.
     * @param distance the maximum distance to check.
     * @return True if the obstacle will be struck fasle otherwise.
     */
    public boolean checkCollision(Coordinates obstacle, double radius, double distance) {
        
        Coordinates m = obstacle.minus(coordinates);
        double sin = Math.sin(heading);
        double cos = Math.cos(heading);
        
        if (distance < m.getRange()) {
            return false;
        }
        if (m.getRange() < radius) {
            return true;
        }
        double d = Math.abs( cos*(coordinates.getY()-obstacle.getY())
        - sin*(coordinates.getX()-obstacle.getX()));
        
        if (d < radius) {
            // ensure that it is along our heading
            return rightDirection(cos, m.getX()) &&
                    rightDirection(sin, m.getY());
        }
        return false;
    }
    
    private boolean rightDirection(double trig, double delta) {
        if (Precision.equals(trig, 0, 2*Precision.EPSILON)) {
            return true;
        }
        return (trig < 0) ? delta <= 0 : delta >= 0;
    }
}
