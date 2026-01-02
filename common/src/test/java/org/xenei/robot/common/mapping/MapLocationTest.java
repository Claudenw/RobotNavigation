package org.xenei.robot.common.mapping;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.locationtech.jts.geom.Coordinate;
import org.xenei.robot.common.AbstractLocationTest;
import org.xenei.robot.common.ChassisInfoTest;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.ScaleInfo;
import org.xenei.robot.common.utils.RobutContext;

public final class MapLocationTest extends AbstractLocationTest {

    private RobutContext ctxt;
    private Map map;

    @BeforeEach
    void setup() {
        RobutContext.Builder builder = RobutContext.builder();
        builder.setOptions(builder.defaultOptions())
                .setChassisInfo(ChassisInfoTest.DEFAULT);
        ctxt = builder.build();
    }

    @AfterEach
    void teardown() {
        ctxt.close();
    }

    @Override
    protected double tolerance() {
        return ctxt.scaleInfo.getResolution();
    }

    @Override
    protected MapLocation convertLocation(Location location) {
        if (map == null) {
            map = new Map(ctxt, new MapTest.TestingStorage());
        }
        return map.asMapLocation(location);
    }
}
