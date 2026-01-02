package org.xenei.robot.common.mapping;

import org.locationtech.jts.geom.Coordinate;
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
    public double heading() {
        return heading;
    }

    @Override
    public MapPosition nextPosition(Location relativeLocation) {
        return getMap().asMapPosition(PositionUtils.nextPosition(this, relativeLocation));
    }

    @Override
    public MapPosition nextPosition(Coordinate relativeCoordinates) {
        return getMap().asMapPosition(PositionUtils.nextPosition(this, relativeCoordinates));
    }

    @Override
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


    public MapObstacle createRelativeObstacle(MapCoordinate relativeStart,
                                                             MapCoordinate relativeEnd) {
        return getMap().createObstacle(nextPosition(relativeStart).getCoordinate(),
                nextPosition(relativeEnd).getCoordinate());
    }

    public boolean isNan() {
        return CoordUtils.isNaN(this.getCoordinate()) || Double.isNaN(heading());
    }

    @Override
    public String toString() {
        return PositionUtils.toString(this);
    }
}
