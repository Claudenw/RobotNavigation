package org.xenei.robot.common;

import org.locationtech.jts.geom.Coordinate;
import org.xenei.robot.common.utils.CoordUtils;
import org.xenei.robot.common.utils.GeometryUtils;

/**
 * A combination of Coordinates and a heading. The coordinates are immutable but
 * the heading may be changed.
 */
public class Position extends Location {
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
    public Position(FrontsCoordinate point) {
        this(point.getCoordinate(), 0.0);
    }

    /**
     * Constructs a position from a point with a heading of 0.0.
     * 
     * @param point the point to to center the position on.
     */
    public Position(Coordinate point) {
        this(point, 0.0);
    }

    /**
     * Constructs a position from a point an a heading.
     * 
     * @param point the point ot center the position on.
     * @param heading the heading in radians.
     */
    public Position(Coordinate point, double heading) {
        super(point);
        this.heading = heading;
    }

    /**
     * Constructs a position from a point an a heading.
     * 
     * @param point the point ot center the position on.
     * @param heading the heading in radians.
     */
    public Position(FrontsCoordinate point, double heading) {
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
    public void setHeading(Coordinate heading) {
        this.heading = headingTo(heading);
    }

    /**
     * Set the heading to a Point.
     * 
     * @param heading the point to head towards.
     */
    public void setHeading(FrontsCoordinate heading) {
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
    public Position nextPosition(Location relativeCoordinates) {
        if (relativeCoordinates.range() == 0) {
            return new Position(getX(), getY(), relativeCoordinates.theta());
        }
        return new Position(this.plus(relativeCoordinates), relativeCoordinates.theta());
    }

    @Override
    public String toString() {
        return String.format("Position[ %s heading:%.4f ]", CoordUtils.toString(this.getCoordinate(), 4),
                Math.toDegrees(heading));
    }
    
    public boolean checkCollistion(FrontsCoordinate fc, double err) {
        return checkCollistion(fc.getCoordinate(), err);
    }

    public boolean checkCollistion(Coordinate c, double err) {
        double d = distance(c);
        Coordinate l = CoordUtils.fromAngle( heading, d);
        return GeometryUtils.asPath( this.coordinate, l ).intersects( GeometryUtils.asPolygon(c, err));
    }

}
