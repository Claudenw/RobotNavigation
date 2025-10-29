package org.xenei.robot.common.mapping;

import org.xenei.robot.common.Location;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.utils.CoordUtils;

public final class MapPosition extends MapLocation implements Position {
    final double heading;

    MapPosition(Map map, MapCoordinate coord, double heading) {
        super(map, coord);
        this.heading = heading;
    }

    /**
     * Gets the heading.
     *
     * @return the heading current heading in radians.
     */
    public double getHeading() {
        return heading;
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
    public MapPosition nextPosition(MapCoordinate relativeCoordinates) {
        return getMap().asMapPosition(PositionUtils.nextPosition(this, relativeCoordinates));
    }

    /**
     * Calculates the next position.
     * <p>
     * The heading will be the theta from the relative coordinates.
     * </p>
     *
     * @param thetaAndRange
     *            The coordinates relative to this position to move to.
     * @return the new Position centered on the new position with the proper
     *         heading.
     */
    public MapPosition nextPosition(ThetaAndRange thetaAndRange) {
        return getMap().asMapPosition(PositionUtils.nextPosition(this, thetaAndRange));
    }

    /**
     * Calculates the next position by moving the specified distance.
     * <p>
     * The heading does not change
     * </p>
     *
     * @param scaledRange
     *            The distance to travel.
     * @return the new Position centered on the new position with the proper
     *         heading.
     */
    public MapPosition nextPosition(double scaledRange) {
        return getMap().asMapPosition(PositionUtils.nextPosition(this, scaledRange));
    }

    /**
     * Calculates the next position.
     * <p>
     * The heading is will be the theta from the relative coordinates.
     * </p>
     *
     * @param theta
     *            the number of radians to add to the heading. to.
     * @return the new Position with the same location and different heading.
     */
    public MapPosition addHeading(double theta) {
        return getMap().asMapPosition(this, heading + theta);
    }

    public MapPosition nextPosition(Location relativeLocation) {
        return getMap().asMapPosition(Position.PositionUtils.nextPosition(this, getMap().asMapCoordinate(relativeLocation)));
    }

    public MapObstacle createRelativeObstacle(MapCoordinate relativeStart,
                                                             MapCoordinate relativeEnd) {
        return getMap().createObstacle(nextPosition(relativeStart).getCoordinate(),
                nextPosition(relativeEnd).getCoordinate());
    }

    public boolean isNan() {
        return CoordUtils.isNaN(this.getCoordinate()) || Double.isNaN(getHeading());
    }

    @Override
    public String toString() {
        return PositionUtils.toString(this);
    }
}
