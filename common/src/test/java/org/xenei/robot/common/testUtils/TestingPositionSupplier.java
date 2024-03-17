package org.xenei.robot.common.testUtils;

import java.util.function.Supplier;

import org.xenei.robot.common.Position;

public class TestingPositionSupplier implements Supplier<Position> {
    public Position position;
    
    public TestingPositionSupplier(Position initial) {
        position = initial;
    }

    @Override
    public Position get() {
        return position;
    }
}