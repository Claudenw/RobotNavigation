package org.xenei.robot.common.mapping;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.locationtech.jts.geom.Coordinate;
import org.xenei.robot.common.AbstractPositionTest;
import org.xenei.robot.common.ChassisInfoTest;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.ScaleInfo;
import org.xenei.robot.common.TestingConfiguration;
import org.xenei.robot.common.utils.RobutContext;

public final class MapPositionTest extends AbstractPositionTest {
    private Map map;

    @AfterEach
    void teardown() {
        if (map != null) {
            map.getContext().close();
            map = null;
        }
    }

    @Override
    protected double tolerance() {
        return ScaleInfo.DEFAULT.getResolution();
    }

    @Override
    protected Position convertPosition(Position position) {
        if (map == null) {
            map = new Map(TestingConfiguration.getContextBuilder("MapPositionTest").build(), new MapTest.TestingStorage());
        }
        return map.asMapPosition(position);
    }
}
