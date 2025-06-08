package org.xenei.robot.common.testUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.common.BumpSensor;
import org.xenei.robot.common.FrontsCoordinate;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.Mover;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.utils.CoordUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class FakeMover implements Mover {
    private static final Logger LOG = LoggerFactory.getLogger(FakeMover.class);
    Position position;
    private final int speed;
    private final List<BumpSensor.BumpState> readings;

    public FakeMover(FrontsCoordinate initial, int speed) {
        this.position = Position.from(initial);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Initial position {}", position);
        }
        this.speed = speed;
        readings = new ArrayList<>();
    }
    

    @Override
    public Position move(Location move) {
        if (move.range() > speed) {
            move = Location.from(CoordUtils.fromAngle(move.theta(), speed));
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
        position = Position.from(position, heading);
    }

    @Override
    public Consumer<BumpSensor.BumpState> getBumpSensorListener() {
        return readings::add;
    }
}
