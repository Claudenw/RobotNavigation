package org.xenei.robot.testUtils;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.navigation.Coordinates;
import org.xenei.robot.navigation.Position;
import org.xenei.robot.utils.CoordinateMap;
import org.xenei.robot.utils.Sensor;

public class FakeSensor implements Sensor {
    private static final Logger LOG = LoggerFactory.getLogger(Sensor.class);
    private static final int blocksize = 17;
    private static final double radians = Math.toRadians(360.0 / blocksize);
    private final CoordinateMap map;

    public FakeSensor(CoordinateMap map) {
        this.map = map;
    }

    public CoordinateMap map() {
        return map;
    }

    @Override
    public Coordinates[] sense(Position position) {
        Coordinates[] result = new Coordinates[blocksize];

        for (int i = 0; i < blocksize; i++) {
            result[i] = look(position, position.getHeadingRadians() + (radians * i));
            if (LOG.isDebugEnabled()) {
                LOG.debug("Reading {}: {}", i, result[i]);
            }
        }
        return result;
    }

    private Coordinates look(Coordinates position, double heading) {
        Optional<Coordinates> result = map.look( position, heading, 350);
        return result.orElse(Coordinates.fromRadians(heading, Double.POSITIVE_INFINITY));
    }

}
