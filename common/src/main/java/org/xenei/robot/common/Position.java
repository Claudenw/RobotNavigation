package org.xenei.robot.common;

import java.util.Comparator;

import org.locationtech.jts.geom.Coordinate;
import org.xenei.robot.common.utils.AngleUtils;
import org.xenei.robot.common.utils.CoordUtils;
import org.xenei.robot.common.utils.DoubleUtils;
import org.xenei.robot.common.utils.RobutContext;

/**
 * A position is a location and a heading.
 */
public interface Position extends Location {
    Position ORIGIN = Position.from(Location.ORIGIN, 0.0);
    /**
     * Create a position from a coordinate and a heading
     * @param coordinate the coordinate for the position.
     * @param head the heading for the position.
     * @return the position at the coordinate and heading.
     */
    static Position from(Coordinate coordinate, double head) {
        return new Position() {
            final double heading = head;
            final UnmodifiableCoordinate coord = UnmodifiableCoordinate.make(coordinate);

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
                return String.format("Position[ %s heading:%.4f (%.4f) ]", CoordUtils.toString(this.getCoordinate(), 4),
                        getHeading(), Math.toDegrees(getHeading()));
            }
        };
    }

    /**
     * Compares Coordinates by angle and then range.
     */
    Comparator<Position> Compr = (one, two) -> {
        int x = Location.ThetaCompr.compare(one, two);
        return x == 0 ? Double.compare(one.getHeading(), two.getHeading()) : x;
    };

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
     * @return the heading current heading in radians.
     */
    double getHeading();

    /**
     * Calculates the heading required to move from the current absolute position to
     * another absolute coordinate.
     * 
     * @param coordinate the coordinate to calculate the heading to.
     * @return the heading in radians.
     */
    default double headingTo(Coordinate coordinate) {
        return AngleUtils.normalize(Math.atan2(coordinate.getY() - this.getY(), coordinate.getX() - this.getX()));
    }

    /**
     * Calculates the heading required to move from the current absolute position to
     * an absolute coordinate.
     * 
     * @param coordinate the coordinate to calculate the heading to.
     * @return the heading in radians.
     */
    default double headingTo(FrontsCoordinate coordinate) {
        return headingTo(coordinate.getCoordinate());
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
    default Position nextPosition(FrontsCoordinate relativeCoordinates) {
        if (relativeCoordinates.range() == 0  && relativeCoordinates.theta() == 0) {
            return this;
        }

        double relativeTheta = relativeCoordinates.theta();
        double headingTheta = this.getHeading();

        double newHeading = AngleUtils.normalize(headingTheta + relativeTheta);
        ThetaAndRange relativePosition = new ThetaAndRange(newHeading, relativeCoordinates.range());
        Coordinate newCoord = this.plus(relativePosition);
        return Position.from(newCoord, newHeading);
    }

    /**
     * Calculates the next position by moving the specified distance..
     * <p>
     * The heading does not change
     * </p>
     *
     * @param range The distance to travel.
     * @return the new Position centered on the new position with the proper
     * heading.
     */
    default Position nextPosition(double range) {
        double heading = getHeading();
        Coordinate a = this.plus(CoordUtils.fromAngle(heading, range));
        return Position.from(a, heading);
    }

    /**
     * Calculates the next position.
     * <p>
     * The heading is will be the theta from the relative coordinates.
     * </p>
     *
     * @param theta the number of radians to add to the heading.
     * to.
     * @return the new Position with the same locatin and different heading.
     */
    default Position addHeading(double theta) {
        return Position.from(this, this.getHeading()+theta);
    }

    /**
     * Calculates the relative location from an absolute locaiton.
     * @param absoluteLocation the coordinates of the absolute location.
     * @return the relative location based on current location and heading.
     */
    default Location relativeLocation(FrontsCoordinate absoluteLocation) {
        double range = distance(absoluteLocation);
        if (range == 0) {
            return Location.ORIGIN;
        }
        // double x = this.getX() - absoluteLocation.getX();
        // double y = this.getY() - absoluteLocation.getY();
        double theta = this.headingTo(absoluteLocation) - this.getHeading();

        return Location.from(CoordUtils.fromAngle(theta, range));
    }

    default boolean checkCollision(RobutContext ctxt, FrontsCoordinate fc, double tolerance) {
        return checkCollision(ctxt, fc.getCoordinate(), tolerance);
    }

    default boolean checkCollision(RobutContext ctxt, Coordinate c, double tolerance) {
        Coordinate l = CoordUtils.fromAngle(getHeading(), distance(c));
        double d = ctxt.geometryUtils.asPath(tolerance, this.getCoordinate(), l)
                .distance(ctxt.geometryUtils.asPoint(c));
        return DoubleUtils.inRange(d, tolerance / 2);
    }

    default boolean isNan() {
        return CoordUtils.isNaN(this.getCoordinate()) || Double.isNaN(getHeading());
    }

}
