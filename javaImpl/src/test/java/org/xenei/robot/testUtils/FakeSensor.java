package org.xenei.robot.testUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.navigation.Coordinates;
import org.xenei.robot.navigation.Position;
import org.xenei.robot.utils.CoordinateMap;
import org.xenei.robot.utils.Sensor;

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
            result[i] = look(position.coordinates(), position.getHeadingRadians() + (RADIANS * i)).minus(position.coordinates());
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
        return map.look(position, heading, 350).orElse(Coordinates.fromRadians(heading, Double.POSITIVE_INFINITY));
    }
    
    @Override
    public double maxRange() {
        return MAX_RANGE;
    }

}
