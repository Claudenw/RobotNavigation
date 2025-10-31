package org.xenei.robot.common;

public class PositionTest extends AbstractPositionTest {

    @Override
    protected double tolerance() {
        return 0.000000000001;
    }

    @Override
    protected Position convertPosition(Position position) {
        return position;
    }
}
