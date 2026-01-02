package org.xenei.robot.common.mapping;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.xenei.robot.common.AbstractLocationTest;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.ScaleInfo;
import org.xenei.robot.common.TestingConfiguration;
import org.xenei.robot.common.utils.RobutContext;

public final class MapCoordinateTest extends AbstractLocationTest {
private Map map;


    @AfterEach
    void teardown() {
        if (map != null) {
            map.getContext().close();
            map = null;
        }
    }

    @Override
    protected MapCoordinate convertLocation(Location location) {
        if (map == null) {
            map = new Map(TestingConfiguration.getContextBuilder("MapCoordinateTest").build(), new MapTest.TestingStorage());
        }
        return map.asMapCoordinate(location);
    }

    @Override
    protected double tolerance() {
        return ScaleInfo.DEFAULT.getResolution();
    }
}
