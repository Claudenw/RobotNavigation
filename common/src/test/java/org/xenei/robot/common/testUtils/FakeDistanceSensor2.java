package org.xenei.robot.common.testUtils;

import java.util.List;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.common.FrontsCoordinate;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.PositionI;
import org.xenei.robot.common.mapping.Map;
import org.xenei.robot.common.messages.Topic;

public class FakeDistanceSensor2 implements FakeDistanceSensor {
    private static final Logger LOG = LoggerFactory.getLogger(FakeDistanceSensor2.class);
    private final Map<?, ?, ?> map;
    private final double angle;
    private static final double MAX_RANGE = 5;
    private final Supplier<Position> positionSupplier;
    private final Topic<Readings> distanceTopic;

    public FakeDistanceSensor2(Map<?, ?, ?> map, double angle, Supplier<Position> positionSupplier) {
        this.map = map;
        this.angle = angle;
        this.positionSupplier = positionSupplier;
        this.distanceTopic = map.getContext().bus.distance;;
    }

    @Override
    public Map<?, ?, ?> map() {
        return map;
    }

    @Override
    public void run() {
        Position pos = positionSupplier.get();
        deliver(pos, new Location(look(pos, pos.getHeading() - angle).minus(pos)));
        deliver(pos, new Location(look(pos, pos.getHeading()).minus(pos)));
        deliver(pos, new Location(look(pos, pos.getHeading() + angle).minus(pos)));
    }

    private void deliver(Position position, Location location) {
        if (location.range() <= maxRange()) {
            DistanceReading dr = new DistanceReading(location.theta(), location.range());
            distanceTopic.send(new Readings(position, List.of(dr)));
        }
    }

    private FrontsCoordinate look(PositionI<?, ?> position, double heading) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Scanning heading: {} {}", heading, Math.toDegrees(heading));
        }
        return map.look(position, heading, (int) Math.round(maxRange())).join().orElse(Location.INFINITE);
    }

    @Override
    public double maxRange() {
        return MAX_RANGE;
    }
}
