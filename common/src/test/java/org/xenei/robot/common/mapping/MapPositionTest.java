package org.xenei.robot.common.mapping;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.locationtech.jts.geom.Coordinate;
import org.xenei.robot.common.AbstractPositionTest;
import org.xenei.robot.common.ChassisInfoTest;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.ScaleInfo;
import org.xenei.robot.common.utils.RobutContext;

public final class MapPositionTest extends AbstractPositionTest {
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
    protected Position convertPosition(Position position) {
        if (map == null) {
            map = new Map(ctxt, new MapTest.TestingStorage());
        }
        return map.asMapPosition(position);
    }
}
