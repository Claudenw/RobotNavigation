package org.xenei.robot.common;

import org.locationtech.jts.geom.Coordinate;
import org.xenei.robot.common.mapping.ThetaAndRange;
import org.xenei.robot.common.utils.AngleUtils;
import org.xenei.robot.common.utils.CoordUtils;

/**
 * A position is a location and a heading.
 */
public interface Position extends Location  {

    static Position asPosition(Coordinate coord, double heading) {
        return new Position() {

            @Override
            public Coordinate getCoordinate() {
                return coord;
            }

            @Override
            public double getHeading() {
                return heading;
            }

            @Override
            public String toString() {
                return PositionUtils.toString(this);
            }
        };
    }

    static Position asPosition(Location loc, double heading) {
        return new Position() {

            @Override
            public Coordinate getCoordinate() {
                return loc.getCoordinate();
            }

            @Override
            public double getHeading() {
                return heading;
            }
        };
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
     * @param mapCoordinate
     *            the coordinate to calculate the heading to.
     * @return the heading in radians.
     */
    default double headingTo(Location mapCoordinate) {
        return PositionUtils.headingTo(this, mapCoordinate);
    }

//    default Location relativeLocation(Location absoluteLocation) {
//        return PositionUtils.relativeLocation(this, absoluteLocation);
//    }

    default Position nextPosition(Location relativeLocation) {
        return Position.PositionUtils.nextPosition(this, relativeLocation);
    }

    final class PositionUtils {

        private PositionUtils() {
            // do not instantiate.
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
        static public Position nextPosition(Position position, Coordinate relativeCoordinates) {
            return nextPosition(position, new ThetaAndRange(CoordUtils.angleBetween(position.getCoordinate(), relativeCoordinates),
                    relativeCoordinates.distance(ORIGIN.getCoordinate())));
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
        static public Position nextPosition(Position position, Location relativeCoordinates) {
            return nextPosition(position, relativeCoordinates.getCoordinate());
        }

        static public Position nextPosition(Position position, double range) {
            double heading = position.getHeading();
            return asPosition(CoordUtils.add(position.getCoordinate(), CoordUtils.fromAngle(heading, range)), heading);
        }

//        /**
//         * Calculates the next position.
//         * <p>
//         * The heading will be the theta from the relative coordinates.
//         * </p>
//         *
//         * @param thetaAndRange
//         *            The coordinates relative to this position to move to.
//         * @return the new Position centered on the new position with the proper
//         *         heading.
//         */
//        static public Position nextPosition(Position position, ThetaAndRange thetaAndRange) {
//            if (thetaAndRange.range() == 0 && thetaAndRange.theta() == 0) {
//                return position;
//            }
//            double newHeading = AngleUtils.normalize(position.getHeading() + thetaAndRange.theta());
//            Coordinate newCoordinate = CoordUtils.fromAngle(newHeading, thetaAndRange.range());
//
//            return asPosition(newCoordinate, newHeading);
//        }

        /**
         * Calculates the heading required to move from the current absolute position to
         * another absolute coordinate.
         *
         * @param position the position to start at.
         * @param location
         *            the coordinate to calculate the heading to.
         * @return the heading in radians.
         */
        static public double headingTo(Position position, Location location) {
            Coordinate pCoordinate = position.getCoordinate();
            Coordinate lCoordinate = location.getCoordinate();
            return AngleUtils.normalize(Math.atan2(lCoordinate.getY() - pCoordinate.getY(), lCoordinate.getX() - pCoordinate.getX()));
        }

//        static public Location relativeLocation(Position position, Location absoluteLocation) {
//            double range = position.getCoordinate().distance(absoluteLocation.getCoordinate());
//            if (range == 0) {
//                return position;
//            }
//            double theta = headingTo(position, absoluteLocation) - position.getHeading();
//            return Location.asLocation(CoordUtils.fromAngle(theta, range));
//        }

        static public String toString(Position position) {
            return String.format("Position[%s, h: %s]", position.getCoordinate(), position.getHeading());
        }
    }
}
