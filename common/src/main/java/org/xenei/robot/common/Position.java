package org.xenei.robot.common;

import org.locationtech.jts.geom.Coordinate;
import org.xenei.robot.common.utils.AngleUtils;
import org.xenei.robot.common.utils.CoordUtils;
import org.xenei.robot.common.utils.DoubleUtils;
import org.xenei.robot.common.utils.GeometryUtils;

public interface Position extends Location {

    static Position from(Coordinate c, double head) {
        return new Position() {
            double heading = head;
            UnmodifiableCoordinate coord = UnmodifiableCoordinate.make(c);

            @Override
            public UnmodifiableCoordinate getCoordinate() {
                return coord;
            }

            @Override
            public double getHeading() {
                return heading;
            }

            @Override
            public String toString() {
                return String.format("Position[ %s heading:%.4f ]", CoordUtils.toString(this.getCoordinate(), 4),
                        Math.toDegrees(getHeading()));
            }
        };
    }

    /**
     * Constructs a position from a point with a heading of 0.0.
     * 
     * @param point the point to to center the position on.
     */
    static Position from(FrontsCoordinate point) {
        return from(point.getCoordinate(), 0.0);
    }

    /**
     * Constructs a position from a point with a heading of 0.0.
     * 
     * @param point the point to to center the position on.
     */
    static Position from(Coordinate point) {
        return from(point, 0.0);
    }

    /**
     * Constructs a position from a point an a heading.
     * 
     * @param point the point ot center the position on.
     * @param heading the heading in radians.
     */
    static Position from(FrontsCoordinate point, double heading) {
        return from(point.getCoordinate(), heading);
    }

    /**
     * Constructs a position from an X and Y coordinates with a heading of 0.0
     * 
     * @param x the x position.
     * @param y the y position.
     */
    static Position from(double x, double y) {
        return from(new Coordinate(x, y), 0.0);
    }

    /**
     * Constructs a position from an X and Y coordinates with the specified heading.
     * 
     * @param x the x position.
     * @param y the y position.
     * @param heading the heading in radians.
     */
    static Position from(double x, double y, double heading) {
        return from(new Coordinate(x, y), heading);
    }

    /**
     * Gets the heading.
     * 
     * @return the heading in radians
     */
    double getHeading();

    default double headingTo(Coordinate heading) {
        return AngleUtils.normalize(Math.atan2(heading.getY() - this.getY(), heading.getX() - this.getX()));
    }

    default double headingTo(FrontsCoordinate heading) {
        return headingTo(heading.getCoordinate());
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
    default Position nextPosition(Location relativeCoordinates) {
        if (relativeCoordinates.range() == 0) {
            return this;
        }

        double range = relativeCoordinates.range();
        double thetar = relativeCoordinates.theta();
        double thetah = this.getHeading();

        double apime = AngleUtils.normalize(thetah + thetar);
        // double aprime = this.headingTo(relativeCoordinates);

        // double heading =
        // AngleUtils.normalize(this.heading+this.headingTo(relativeCoordinates));
        Coordinate a = this.plus(CoordUtils.fromAngle(apime, range));
        return Position.from(a, apime);
    }

    default Location relativeLocation(Coordinate absoluteLocation) {
        double range = distance(absoluteLocation);
        if (range == 0) {
            return Location.ORIGIN;
        }
        // double x = this.getX() - absoluteLocation.getX();
        // double y = this.getY() - absoluteLocation.getY();
        double theta = this.headingTo(absoluteLocation) - this.getHeading();

        return Location.from(CoordUtils.fromAngle(theta, range));
    }

    default boolean checkCollision(FrontsCoordinate fc, double tolerance) {
        return checkCollision(fc.getCoordinate(), tolerance);
    }

    default boolean checkCollision(Coordinate c, double tolerance) {
        Coordinate l = CoordUtils.fromAngle(getHeading(), distance(c));
        double d = GeometryUtils.asPath(tolerance, this.getCoordinate(), l).distance(GeometryUtils.asPoint(c));
        return DoubleUtils.inRange(d, tolerance/2);
    }

}
