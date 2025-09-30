package org.xenei.robot.common;

import org.locationtech.jts.geom.Coordinate;

import java.util.Comparator;

public interface LocationI<L extends LocationI<?>> extends FrontsCoordinate {

    /**
     * Compares Coordinates by angle and then range.
     */
    Comparator<LocationI<?>> ThetaCompr = (one, two) -> {
        int x = Double.compare(one.theta(), two.theta());
        return x == 0 ? Double.compare(one.range(), two.range()) : x;
    };

    /**
     * Compares Coordinates by range and then angle.
     */
    Comparator<LocationI<?>> RangeCompr = (one, two) -> {
        int x = Double.compare(one.range(), two.range());
        return x == 0 ? Double.compare(one.theta(), two.theta()) : x;
    };

    default UnmodifiableCoordinate getCoordinate() {
        return unmodifiableCoordinate();
    }

    UnmodifiableCoordinate unmodifiableCoordinate();

    L buildLocation(Coordinate coordinate);

}
