package org.xenei.robot.common;

import org.locationtech.jts.geom.Coordinate;
import org.xenei.robot.common.utils.CoordUtils;

public class Location implements LocationI<Location> {
    /**
     * The origin for the map (0,0)
     */
    public static final Location ORIGIN = new Location(FrontsCoordinate.ORIGIN);
    /**
     * An exemplar of an infinite location.
     *
     * @see CoordUtils#isInfinite(Coordinate)
     * @see FrontsCoordinate#INFINITE
     */
    public static final Location INFINITE = new Location(FrontsCoordinate.INFINITE);

    private final UnmodifiableCoordinate coordinate;

    public Location(FrontsCoordinate coordinate) {
        this(coordinate.getCoordinate());
    }

    public Location(Coordinate coordinate) {
        this.coordinate = UnmodifiableCoordinate.make(coordinate);
    }

    @Override
    public Location buildLocation(Coordinate coordinate) {
        return new Location(coordinate);
    }

    @Override
    public UnmodifiableCoordinate unmodifiableCoordinate() {
        return coordinate;
    }

    @Override
    public String toString() {
        return String.format("Location[ %s r:%.2f]", CoordUtils.toString(getCoordinate(), 4), range());
    }

}
