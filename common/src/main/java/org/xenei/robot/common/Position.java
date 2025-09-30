package org.xenei.robot.common;

import org.locationtech.jts.geom.Coordinate;

/**
 * A position is a location and a heading.
 */
public class Position extends Location implements PositionI<Location, Position> {

    public static Position ORIGIN = new Position(Location.ORIGIN.getCoordinate(), 0.0);

    private final double heading;

    public Position(Coordinate coordinate, double heading) {
        super(coordinate);
        this.heading = heading;
    }

    @Override
    public Position buildPosition(Coordinate coordinate, double heading) {
        return new Position(coordinate, heading);
    }

    /**
     * Gets the heading.
     *
     * @return the heading current heading in radians.
     */
    public double getHeading() {
        return heading;
    }
}
