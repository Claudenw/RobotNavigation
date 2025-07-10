package org.xenei.robot.common;

import org.locationtech.jts.geom.Coordinate;
import org.xenei.robot.common.utils.CoordUtils;

import java.util.Comparator;

public record ThetaAndRange(double theta, double range) implements Location {
    public UnmodifiableCoordinate getCoordinate() {
        return UnmodifiableCoordinate.make(CoordUtils.fromAngle(theta, range));
    }
}
