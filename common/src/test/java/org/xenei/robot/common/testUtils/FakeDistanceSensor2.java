package org.xenei.robot.common.testUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.common.DistanceSensor;
import org.xenei.robot.common.FrontsCoordinate;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.mapping.Map;
import org.xenei.robot.common.messages.Topic;
import org.xenei.robot.common.utils.CoordUtils;

public class FakeDistanceSensor2 implements FakeDistanceSensor {
    private static final Logger LOG = LoggerFactory.getLogger(FakeDistanceSensor2.class);
    private final Map map;
    private final double angle;
    private static final double MAX_RANGE = 5;
    private final Supplier<Position> positionSupplier;
    private final Topic<Readings> distanceTopic;

    public FakeDistanceSensor2(Map map, double angle, Supplier<Position> positionSupplier) {
        this.map = map;
        this.angle = angle;
        this.positionSupplier = positionSupplier;
        this.distanceTopic = map.getContext().bus.distance;;
    }

    @Override
    public Map map() {
        return map;
    }

    @Override
    public void run() {
        Position pos = positionSupplier.get();
        deliver(pos, Location.from(look(pos, pos.getHeading() - angle).minus(pos)));
        deliver(pos, Location.from(look(pos, pos.getHeading()).minus(pos)));
        deliver(pos, Location.from(look(pos, pos.getHeading() + angle).minus(pos)));
    }

    private void deliver(Position position, Location location) {
        if (location.range() <= maxRange()) {
            DistanceReading dr = new DistanceReading(location.theta(), location.range());
            distanceTopic.send(new Readings(position, List.of(dr)));
        }
    }

    private FrontsCoordinate look(Position position, double heading) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Scanning heading: {} {}", heading, Math.toDegrees(heading));
        }
        return map.look(position, heading, (int)Math.round(maxRange())).join()
                .orElse(Location.from(CoordUtils.fromAngle(heading, Double.POSITIVE_INFINITY)));
    }

    @Override
    public double maxRange() {
        return MAX_RANGE;
    }
}
