package org.xenei.robot.common.testUtils;

import java.util.function.Supplier;

import org.xenei.robot.common.PositionI;

public class TestingPositionSupplier implements Supplier<PositionI<?, ?>> {
    public PositionI<?, ?> position;

    public TestingPositionSupplier(PositionI<?, ?> initial) {
        position = initial;
    }

    @Override
    public PositionI<?, ?> get() {
        return position;
    }
}
