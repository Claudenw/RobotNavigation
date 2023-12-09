package org.xenei.robot.common.testUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.ScaleInfo;
import org.xenei.robot.common.mapping.CoordinateMap;
import org.xenei.robot.common.utils.CoordUtils;

public class FakeDistanceSensor1 implements FakeDistanceSensor {
    private static final Logger LOG = LoggerFactory.getLogger(FakeDistanceSensor.class);
    private static final int BLOCKSIZE = 17;
    private static final double RADIANS = Math.toRadians(360.0 / BLOCKSIZE);
    private final CoordinateMap map;
    private static final double MAX_RANGE = 350;

    private Position position;

    public FakeDistanceSensor1(CoordinateMap map) {
        this.map = map;
    }

    public CoordinateMap map() {
        return map;
    }

    @Override
    public void setPosition(Position position) {
        this.position = position;
    }

    @Override
    public Location[] sense() {
        Location[] result = new Location[BLOCKSIZE];

        for (int i = 0; i < BLOCKSIZE; i++) {
            result[i] = new Location(look(position, position.getHeading() + (RADIANS * i)).minus(position));
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
                .orElse(new Location(CoordUtils.fromAngle(heading, Double.POSITIVE_INFINITY)));
    }

    @Override
    public double maxRange() {
        return MAX_RANGE;
    }

    @Override
    public ScaleInfo getScale() {
        return FakeDistanceSensor.SCALE;
    }
}
