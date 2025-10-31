package org.xenei.robot.common.mapping;

import org.locationtech.jts.geom.Coordinate;
import org.xenei.robot.common.AbstractPositionTest;
import org.xenei.robot.common.ChassisInfoTest;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.ScaleInfo;
import org.xenei.robot.common.utils.RobutContext;

public class MapPositionTest extends AbstractPositionTest {
    protected static final RobutContext ctxt = new RobutContext(ScaleInfo.DEFAULT, ChassisInfoTest.DEFAULT);
    protected Map map;

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
