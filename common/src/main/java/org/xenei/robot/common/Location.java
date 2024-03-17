package org.xenei.robot.common;

import java.util.Comparator;

import org.locationtech.jts.geom.Coordinate;
import org.xenei.robot.common.utils.CoordUtils;

public interface Location extends FrontsCoordinate {
    static Location ORIGIN = from(new Coordinate(0, 0));

    static Location from(Coordinate c) {
        return new Location() {
            UnmodifiableCoordinate coord = UnmodifiableCoordinate.make(c);

            @Override
            public UnmodifiableCoordinate getCoordinate() {
                return coord;
            }

            @Override
            public String toString() {
                return String.format("Location[ %s r:%.2f]", CoordUtils.toString(getCoordinate(), 4), range());
            }
        };
    }

    /**
     * Construct coordinates from a Point.
     * 
     * @param p The point representing the X and Y positions
     */
    static Location from(FrontsCoordinate p) {
        return from(p.getCoordinate());
    }

    /**
     * Construct coordinates from X and Y positions.
     * 
     * @param x the X position.
     * @param y the Y position.
     */
    static Location from(double x, double y) {
        return from(new Coordinate(x, y));
    }

    /**
     * Compares Coordinates by angle and then range.
     */
    static Comparator<Location> ThetaCompr = (one, two) -> {
        int x = Double.compare(one.theta(), two.theta());
        return x == 0 ? Double.compare(one.range(), two.range()) : x;
    };

    /**
     * Compares Coordinates by range and then angle.
     */
    static Comparator<Location> RangeCompr = (one, two) -> {
        int x = Double.compare(one.range(), two.range());
        return x == 0 ? Double.compare(one.theta(), two.theta()) : x;
    };

    /**
     * Return the angle in radians from the origin.
     * 
     * @return the angle in radians from the origin to this coordinates.
     */
    default double theta() {
        return ORIGIN.angleBetween(this);
    }

    /**
     * Get the range to the coordinates in meters.
     * 
     * @return the range to the coordinates.
     */
    default double range() {
        return ORIGIN.distance(this);
    }
}
