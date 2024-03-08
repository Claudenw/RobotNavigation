package org.xenei.robot.common.testUtils;

import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.mapping.CoordinateMap;
import org.xenei.robot.common.utils.CoordUtils;

public class FakeDistanceSensor1 implements FakeDistanceSensor {
    private static final Logger LOG = LoggerFactory.getLogger(FakeDistanceSensor.class);
    private static final int BLOCKSIZE = 17;
    private static final double RADIANS = Math.toRadians(360.0 / BLOCKSIZE);
    private final CoordinateMap map;
    private static final double MAX_RANGE = 350;
    private final Supplier<Position> positionSupplier;
    
    public FakeDistanceSensor1(CoordinateMap map, Supplier<Position> positionSupplier) {
        this.map = map;
        this.positionSupplier = positionSupplier;
    }

    @Override
    public CoordinateMap map() {
        return map;
    }

    @Override
    public Location[] sense() {
        Location[] result = new Location[BLOCKSIZE];
        Position position = positionSupplier.get();
        for (int i = 0; i < BLOCKSIZE; i++) {
            double h = position.getHeading() + (RADIANS * i);
            Location nxt = look(position, position.getHeading() + (RADIANS * i));
            double heading = position.headingTo(nxt) - position.getHeading();
            double d = position.distance(nxt);
            result[i] = Location.from(CoordUtils.fromAngle(heading, d));
            if (LOG.isDebugEnabled()) {
                LOG.debug("Reading {}: {}", i, result[i]);
            }
        }
        return result;
    }

    private Location look(Location position, double heading) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Scanning heading: {} {}", heading, Math.toDegrees(heading));
        }
        return map.look(position, heading, 350)
                .orElse(Location.from(CoordUtils.fromAngle(heading, Double.POSITIVE_INFINITY)));
    }

    @Override
    public double maxRange() {
        return MAX_RANGE;
    }
}
