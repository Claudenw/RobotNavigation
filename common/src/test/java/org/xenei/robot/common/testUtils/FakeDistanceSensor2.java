package org.xenei.robot.common.testUtils;

import java.util.List;
import java.util.function.Supplier;

import org.xenei.robot.common.Location;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.mapping.Map;
import org.xenei.robot.common.mapping.ThetaAndRange;
import org.xenei.robot.common.messages.Topic;

public class FakeDistanceSensor2 implements FakeDistanceSensor {
    private final Map map;
    private final double angle;
    private static final double MAX_RANGE = 5;
    private final Supplier<Position> positionSupplier;
    private final Topic<Readings> distanceTopic;

    public FakeDistanceSensor2(Map map, double angle, Supplier<Position> positionSupplier) {
        this.map = map;
        this.angle = angle;
        this.positionSupplier = positionSupplier;
        this.distanceTopic = map.getContext().bus.distance;
    }

    @Override
    public Map map() {
        return map;
    }

    @Override
    public void run() {
        Position pos = positionSupplier.get();
        deliver(pos, look(pos, pos.getHeading() - angle).minus(pos));
        deliver(pos, look(pos, pos.getHeading()).minus(pos));
        deliver(pos, look(pos, pos.getHeading() + angle).minus(pos));
    }

    private void deliver(Position position, Location location) {
        if (location.range() <= maxRange()) {
            ThetaAndRange thetaAndRange = new ThetaAndRange(location.theta(), location.range());
            distanceTopic.send(new Readings(position, List.of(thetaAndRange)));
        }
    }

    @Override
    public double maxRange() {
        return MAX_RANGE;
    }
}
