package org.xenei.robot.common.testUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.common.FrontsCoordinate;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.Mover;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.ScaleInfo;
import org.xenei.robot.common.utils.CoordUtils;

public class FakeMover implements Mover {
    private static final Logger LOG = LoggerFactory.getLogger(FakeMover.class);
    private Position position;
    private int speed;

    public FakeMover(FrontsCoordinate initial, int speed) {
        this.position = new Position(initial);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Initial position {}", position);
        }
        this.speed = speed;
    }

    @Override
    public Position move(Location move) {
        if (move.range() > speed) {
            move = new Location(CoordUtils.fromAngle(move.theta(), speed));
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
    
    @Override
    public void setHeading(double heading) {
        position.setHeading(heading);
    }

}
