package org.xenei.robot.common;

import java.util.Comparator;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.xenei.robot.common.utils.AngleUtils;
import org.xenei.robot.common.utils.RobutContext;
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
     * Calculates the heading required to move from the current absolute position to another absolute coordinate.
     * @param coordinate the coordinate to calculate the heading to.
     * @return the heading in radians.
     */
    default double headingTo(Coordinate coordinate) {
        return AngleUtils.normalize(Math.atan2(coordinate.getY() - this.getY(), coordinate.getX() - this.getX()));
    }

    /**
     * Calculate sthe heading required to move from the current absolute position to another absolute position.
     * @param position the position to calculate the heading to.
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
    default Position nextPosition(Location relativeCoordinates) {
        if (relativeCoordinates.range() == 0) {
            return this;
        }

        double x = AngleUtils.RADIANS_90;
        
        double range = relativeCoordinates.range();
        double thetar = relativeCoordinates.theta();
        double thetah = this.getHeading();

        double apime = AngleUtils.normalize(thetah + thetar);
        //double apime = thetah + thetar;

        //Coordinate c = CoordUtils.fromAngle(apime, range);
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

    default boolean checkCollision(RobutContext ctxt, FrontsCoordinate fc, double tolerance) {
        return checkCollision(ctxt, fc.getCoordinate(), tolerance);
    }

    default boolean checkCollision(RobutContext ctxt, Coordinate c, double tolerance) {
        Coordinate l = CoordUtils.fromAngle(getHeading(), distance(c));
        double d = ctxt.geometryUtils.asPath(tolerance, this.getCoordinate(), l).distance(ctxt.geometryUtils.asPoint(c));
        return DoubleUtils.inRange(d, tolerance/2);
    }

}
