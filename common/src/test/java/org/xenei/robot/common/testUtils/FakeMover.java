package org.xenei.robot.common.testUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.common.AngleUnits;
import org.xenei.robot.common.Coordinates;
import org.xenei.robot.common.Mover;
import org.xenei.robot.common.Position;

import mil.nga.sf.Point;

public class FakeMover implements Mover {
    private static final Logger LOG = LoggerFactory.getLogger(FakeMover.class);
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
    public Position move(Coordinates move) {
        if (move.getRange() > speed) {
            move = Coordinates.fromAngle(move.getTheta(AngleUnits.RADIANS), AngleUnits.RADIANS, speed);
        }
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
