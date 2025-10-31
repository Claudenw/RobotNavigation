package org.xenei.robot.common.mapping;

import org.junit.jupiter.api.AfterEach;
import org.xenei.robot.common.AbstractLocationTest;
import org.xenei.robot.common.ChassisInfoTest;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.ScaleInfo;
import org.xenei.robot.common.utils.RobutContext;

public class MapCoordinateTest extends AbstractLocationTest {

    protected static final RobutContext ctxt = new RobutContext(ScaleInfo.DEFAULT, ChassisInfoTest.DEFAULT);
    protected Map map;

    @AfterEach
    void cleanupMap() {
        map = null;
    }

    @Override
    protected MapCoordinate convertLocation(Location location) {
        if (map == null) {
            map = new Map(ctxt, new MapTest.TestingStorage());
        }
        return map.asMapCoordinate(location);
    }

    @Override
    protected double tolerance() {
        return ctxt.scaleInfo.getResolution();
    }
}
