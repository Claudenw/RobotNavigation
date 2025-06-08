package org.xenei.robot.common.testUtils;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.mapping.Map;
import org.xenei.robot.common.utils.CoordUtils;

public class FakeDistanceSensor2 implements FakeDistanceSensor {
    private static final Logger LOG = LoggerFactory.getLogger(FakeDistanceSensor2.class);
    private final Map map;
    private final double angle;
    private static final double MAX_RANGE = 350;
    private final Supplier<Position> positionSupplier;
    private final CopyOnWriteArrayList<Consumer<DistanceReading>> listeners;

    public FakeDistanceSensor2(Map map, double angle, Supplier<Position> positionSupplier) {
        this.map = map;
        this.angle = angle;
        this.positionSupplier = positionSupplier;
        this.listeners = new CopyOnWriteArrayList<>();
    }

    @Override
    public Map map() {
        return map;
    }

    @Override
    public void run() {
        Position pos = positionSupplier.get();
        deliver(Location.from(look(pos, pos.getHeading() - angle).minus(pos)));
        deliver(Location.from(look(pos, pos.getHeading()).minus(pos)));
        deliver(Location.from(look(pos, pos.getHeading() + angle).minus(pos)));
    }

    private void deliver(Location location) {
        DistanceReading dr = new DistanceReading(location.theta(), location.range());
        for (Consumer<DistanceReading> listener : listeners) {
            listener.accept(dr);
        }
    }

    private Location look(Position position, double heading) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Scanning heading: {} {}", heading, Math.toDegrees(heading));
        }
        return map.look(position, heading, 350).join()
                .orElse(Location.from(CoordUtils.fromAngle(heading, Double.POSITIVE_INFINITY)));
    }

    @Override
    public double maxRange() {
        return MAX_RANGE;
    }

    @Override
    public void addListener(Consumer<DistanceReading> listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(Consumer<DistanceReading> listener) {
        listeners.remove(listener);
    }

}
