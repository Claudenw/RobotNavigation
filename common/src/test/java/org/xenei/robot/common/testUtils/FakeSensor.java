package org.xenei.robot.common.testUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.common.AngleUnits;
import org.xenei.robot.common.CoordinateMap;
import org.xenei.robot.common.Coordinates;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.Sensor;

public class FakeSensor implements Sensor {
    private static final Logger LOG = LoggerFactory.getLogger(FakeSensor.class);
    private static final int BLOCKSIZE = 17;
    private static final double RADIANS = Math.toRadians(360.0 / BLOCKSIZE);
    private final CoordinateMap map;
    private static final double MAX_RANGE=350;

    public FakeSensor(CoordinateMap map) {
        this.map = map;
    }

    public CoordinateMap map() {
        return map;
    }

    @Override
    public Coordinates[] sense(Position position) {
        Coordinates[] result = new Coordinates[BLOCKSIZE];

        for (int i = 0; i < BLOCKSIZE; i++) {
            result[i] = look(position.coordinates(), position.getHeading(AngleUnits.RADIANS) + (RADIANS * i)).minus(position.coordinates());
            if (LOG.isDebugEnabled()) {
                LOG.debug("Reading {}: {}", i, result[i]);
            }
        }
        return result;
    }

    private Coordinates look(Coordinates position, double heading) {
        if (LOG.isDebugEnabled()) {
            LOG.debug( "Scanning heading: {} {}", heading, Math.toDegrees(heading));
        }
        return map.look(position, heading, 350).orElse(Coordinates.fromAngle(heading, AngleUnits.RADIANS, Double.POSITIVE_INFINITY));
    }
    
    @Override
    public double maxRange() {
        return MAX_RANGE;
    }

}
