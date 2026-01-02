package org.xenei.robot.common.mapping;

import org.locationtech.jts.geom.Coordinate;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.utils.CoordUtils;

public record ThetaAndRange(double theta, double range) implements Location {

    /**
     * Gets the relative location of the reading.
     *
     * @return the relative Location from the reading.
     */
    @Override
    public Coordinate getCoordinate() {
        return CoordUtils.fromAngle(theta, range);
    }

}
