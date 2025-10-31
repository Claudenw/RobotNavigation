package org.xenei.robot.common;

import org.locationtech.jts.geom.Coordinate;
import org.xenei.robot.common.utils.AngleUtils;

public class LocationTest extends AbstractLocationTest {

    @Override
    protected double tolerance() {
        return AngleUtils.TOLERANCE;
    }

    @Override
    protected Location convertLocation(Location location) {
        return location;
    }
}
