package org.xenei.robot.common;

import org.apache.commons.math3.util.Precision;
import org.xenei.robot.common.utils.PointUtils;

import mil.nga.sf.Point;

public class Position extends Coordinates {
    public static final double DELTA = 0.0000000001;

    private double heading;

    public Position() {
        this(0.0, 0.0);
    }

    public Position(Point point) {
        this(point, 0.0);
    }

    public Position(Point point, double heading) {
        super(point);
        this.heading = heading;
    }

    public Position(double x, double y) {
        this(x, y, 0.0);
    }

    public Position(double x, double y, double heading) {
        super(x, y);
        this.heading = heading;
    }

    @Override
    public Position quantize() {
        return isQuantized() ? this : new Position(super.quantize(), heading);
    }

    public double getHeading(AngleUnits units) {
        return units == AngleUnits.RADIANS ? heading : Math.toDegrees(heading);
    }

    public void setHeading(double radians) {
        this.heading = radians;
    }

    public void setHeading(Point heading) {
        this.heading = angleTo(heading);
    }

    /**
     * Calculates the next position.
     * 
     * @param cmd The relative coordinates to move to.
     * @return the new Position with a heading the same as the cmd.
     */
    public Position nextPosition(Coordinates cmd) {
        if (cmd.getRange() == 0) {
            return new Position(getX(), getY(), cmd.getTheta(AngleUnits.RADIANS));
        }
        return new Position(PointUtils.plus(this, cmd), cmd.getTheta(AngleUnits.RADIANS));
    }

    /**
     * Checks if this postion will strike the obstacle within the specified
     * distance.
     * 
     * @param obstacle the obstacle to check.
     * @param radius the size of the obstacle.
     * @param distance the maximum distance to check.
     * @return True if the obstacle will be struck fasle otherwise.
     */
    public boolean checkCollision(Point obstacle, double radius, double distance) {

        Coordinates m = Coordinates.fromXY(PointUtils.minus( obstacle, this ));

        if (distance < m.getRange()) {
            return false;
        }
        if (m.getRange() < radius) {
            return true;
        }

        double sin = Math.sin(heading);
        double cos = Math.cos(heading);
        double d = Math.abs(cos * (getY() - obstacle.getY()) - sin * (getX() - obstacle.getX()));

        if (d < radius) {
            // ensure that it is along our heading
            return rightDirection(cos, m.getX()) && rightDirection(sin, m.getY());
        }
        return false;
    }

    /**
     * Returns true if the target is not obstructed by the obstacle.
     * 
     * @param target
     * @param obstacle
     * @param maxDist
     * @return
     */
    public boolean hasClearView(Point target, Point obstacle) {
        double maxDist = distanceTo(target);
        boolean td = (distanceTo(obstacle) - Coordinates.POINT_RADIUS) < (maxDist + Coordinates.POINT_RADIUS);

        if (td) {
            // in range to check
            return !checkCollision(obstacle, Coordinates.POINT_RADIUS, maxDist);
        }
        return true;
    }

    private boolean rightDirection(double trig, double delta) {
        if (Precision.equals(trig, 0, 2 * Precision.EPSILON)) {
            return true;
        }
        return (trig < 0) ? delta <= 0 : delta >= 0;
    }

    @Override
    public String toString() {
        return String.format("Position[ %s heading:%.4f ]", PointUtils.toString(this, 4), Math.toDegrees(heading));
    }

}
