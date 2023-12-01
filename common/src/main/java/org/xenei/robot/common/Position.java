package org.xenei.robot.common;

import org.apache.commons.math3.util.Precision;
import org.xenei.robot.common.utils.PointUtils;

import mil.nga.sf.Point;

/**
 * A combination of Coordinates and a heading. The coordinates are immutable but
 * the heading may be changed.
 */
public class Position extends Coordinates {
    public static final double DELTA = 0.0000000001;

    /**
     * The heading in radians of this position.
     */
    private double heading;

    /**
     * Constructs a position at the origin with ha heading of 0.0
     */
    public Position() {
        this(0.0, 0.0);
    }

    /**
     * Constructs a position from a point with a heading of 0.0.
     * 
     * @param point the point to to center the position on.
     */
    public Position(Point point) {
        this(point, 0.0);
    }

    /**
     * Constructs a position from a point an a heading.
     * 
     * @param point the point ot center the position on.
     * @param heading the heading in radians.
     */
    public Position(Point point, double heading) {
        super(point);
        this.heading = heading;
    }

    /**
     * Constructs a position from an X and Y coordinates with a heading of 0.0
     * 
     * @param x the x position.
     * @param y the y position.
     */
    public Position(double x, double y) {
        this(x, y, 0.0);
    }

    /**
     * Constructs a position from an X and Y coordinates with the specified heading.
     * 
     * @param x the x position.
     * @param y the y position.
     * @param heading the heading in radians.
     */
    public Position(double x, double y, double heading) {
        super(x, y);
        this.heading = heading;
    }

    @Override
    public Position quantize() {
        return isQuantized() ? this : new Position(super.quantize(), heading);
    }

    /**
     * Gets the heading.
     * 
     * @return the heading in radians
     */
    public double getHeading() {
        return heading;
    }

    /**
     * Sets the heading
     * 
     * @param heading the heading in radians.
     */
    public void setHeading(double heading) {
        this.heading = heading;
    }

    /**
     * Set the heading to a Point.
     * 
     * @param heading the point to head towards.
     */
    public void setHeading(Point heading) {
        this.heading = headingTo(heading);
    }

    /**
     * Calculates the next position.
     * <p>
     * The heading is will be the theta from the relative coordinates.
     * </p>
     * 
     * @param relativeCoordinates The coordinates relative to this position to move
     * to.
     * @return the new Position centered on the new position with the proper
     * heading.
     */
    public Position nextPosition(Coordinates relativeCoordinates) {
        if (relativeCoordinates.getRange() == 0) {
            return new Position(getX(), getY(), relativeCoordinates.getTheta());
        }
        return new Position(PointUtils.plus(this, relativeCoordinates), relativeCoordinates.getTheta());
    }

    /**
     * Checks if this position will strike the obstacle within the specified
     * distance.
     * 
     * @param obstacle the obstacle to check.
     * @param radius the size of the obstacle.
     * @param distance the maximum distance to check.
     * @return True if the obstacle will be struck fasle otherwise.
     */
    public boolean checkCollision(Point obstacle, double radius, double distance) {

        Coordinates m = Coordinates.fromXY(PointUtils.minus(obstacle, this));

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
     * Determines if collision calculation has looked in the proper direction.
     * <p>
     * The direction is correct if the trig function and the delta have the same
     * sign.
     * </p>
     * 
     * @param trig the trig function value.
     * @param delta the change in trig function direction (e.g. X for cos, Y for
     * sin).
     * @return true if the direction is correct.
     */
    private boolean rightDirection(double trig, double delta) {
        if (Precision.equals(trig, 0, 2 * Precision.EPSILON)) {
            return true;
        }
        return (trig < 0) ? delta <= 0 : delta >= 0;
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

    @Override
    public String toString() {
        return String.format("Position[ %s heading:%.4f ]", PointUtils.toString(this, 4), Math.toDegrees(heading));
    }

}
