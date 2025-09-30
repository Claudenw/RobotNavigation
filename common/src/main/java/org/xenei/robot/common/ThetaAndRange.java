package org.xenei.robot.common;

import org.locationtech.jts.geom.Coordinate;
import org.xenei.robot.common.utils.CoordUtils;

public record ThetaAndRange(double theta, double range) implements LocationI<ThetaAndRange> {
    public UnmodifiableCoordinate unmodifiableCoordinate() {
        return UnmodifiableCoordinate.make(CoordUtils.fromAngle(theta, range));
    }

    @Override
    public ThetaAndRange buildLocation(Coordinate coordinate) {
        Location l = new Location(coordinate);
        return new ThetaAndRange(l.theta(), l.range());
    }
}
