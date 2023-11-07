package org.xenei.robot.testUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.navigation.Coordinates;
import org.xenei.robot.navigation.Position;
import org.xenei.robot.utils.Mover;

public class FakeMover implements Mover {
    private static final Logger LOG = LoggerFactory.getLogger(Mover.class);
    private Position position;
    private int speed;

    public FakeMover(Coordinates initial, int speed) {
        this.position = new Position(initial);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Initial position {}", position);
        }
        this.speed = speed;
    }

    @Override
    public Position move(Coordinates location) {
        double theta = position.angleTo(location);
        double range = position.distanceTo(location);
        Coordinates check = Coordinates.fromDegrees(-90, 2);
        double th = check.getThetaDegrees();
        Coordinates move = Coordinates.fromRadians(theta, range);
        th = move.getThetaDegrees();
        position = position.nextPosition(move);
        if (LOG.isDebugEnabled()) {
            LOG.debug("New position {}", position);
        }
        return position;
    }

    @Override
    public Position position() {
        return position;
    }

}
