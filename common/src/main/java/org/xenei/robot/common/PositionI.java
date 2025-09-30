package org.xenei.robot.common;

import org.locationtech.jts.geom.Coordinate;
import org.xenei.robot.common.utils.AngleUtils;
import org.xenei.robot.common.utils.CoordUtils;
import org.xenei.robot.common.utils.DoubleUtils;
import org.xenei.robot.common.utils.RobutContext;

import java.util.Comparator;

public interface PositionI<L extends LocationI<L>, T extends PositionI<L, T>> extends LocationI<L> {
    /**
     * Compares Coordinates by angle and then range.
     */
    Comparator<PositionI<?, ?>> Compr = (one, two) -> {
        int x = LocationI.ThetaCompr.compare(one, two);
        return x == 0 ? Double.compare(one.getHeading(), two.getHeading()) : x;
    };

    T buildPosition(Coordinate coordinate, double heading);

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
     * @param coordinate
     *            the coordinate to calculate the heading to.
     * @return the heading in radians.
     */
    default double headingTo(Coordinate coordinate) {
        return AngleUtils.normalize(Math.atan2(coordinate.getY() - this.getY(), coordinate.getX() - this.getX()));
    }

    /**
     * Calculates the heading required to move from the current absolute position to
     * an absolute coordinate.
     *
     * @param coordinate
     *            the coordinate to calculate the heading to.
     * @return the heading in radians.
     */
    default double headingTo(FrontsCoordinate coordinate) {
        return headingTo(coordinate.getCoordinate());
    }

    /**
     * Calculates the next position.
     * <p>
     * The heading will be the theta from the relative coordinates.
     * </p>
     *
     * @param relativeCoordinates
     *            The coordinates relative to this position to move to.
     * @return the new Position centered on the new position with the proper
     *         heading.
     */
    default T nextPosition(FrontsCoordinate relativeCoordinates) {
        if (relativeCoordinates.range() == 0 && relativeCoordinates.theta() == 0) {
            return (T) this;
        }

        double relativeTheta = relativeCoordinates.theta();
        double headingTheta = this.getHeading();

        double newHeading = AngleUtils.normalize(headingTheta + relativeTheta);
        ThetaAndRange relativePosition = new ThetaAndRange(newHeading, relativeCoordinates.range());
        Coordinate newCoord = this.plus(relativePosition);

        return buildPosition(newCoord, newHeading);
    }

    /**
     * Calculates the next position by moving the specified distance..
     * <p>
     * The heading does not change
     * </p>
     *
     * @param range
     *            The distance to travel.
     * @return the new Position centered on the new position with the proper
     *         heading.
     */
    default T nextPosition(double range) {
        double heading = getHeading();
        Coordinate newCoord = this.plus(CoordUtils.fromAngle(heading, range));
        return buildPosition(newCoord, heading);
    }

    /**
     * Calculates the next position.
     * <p>
     * The heading is will be the theta from the relative coordinates.
     * </p>
     *
     * @param theta
     *            the number of radians to add to the heading. to.
     * @return the new Position with the same locatin and different heading.
     */
    default T addHeading(double theta) {
        return buildPosition(this.getCoordinate(), getHeading() + theta);
    }

    /**
     * Calculates the relative location from an absolute locaiton.
     *
     * @param absoluteLocation
     *            the coordinates of the absolute location.
     * @return the relative location based on current location and heading.
     */
    default L relativeLocation(FrontsCoordinate absoluteLocation) {
        double range = distance(absoluteLocation);
        if (range == 0) {
            return buildLocation(Location.ORIGIN.getCoordinate());
        }
        double theta = this.headingTo(absoluteLocation) - this.getHeading();

        return buildLocation(CoordUtils.fromAngle(theta, range));
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
