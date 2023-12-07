package org.xenei.robot.common.testUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.DistanceSensor;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.mapping.CoordinateMap;
import org.xenei.robot.common.utils.AngleUtils;
import org.xenei.robot.common.utils.CoordUtils;

public class FakeDistanceSensor2 implements DistanceSensor {
    private static final Logger LOG = LoggerFactory.getLogger(FakeDistanceSensor2.class);
    private final CoordinateMap map;
    private static final double MAX_RANGE = 350;

    private Position position;

    public FakeDistanceSensor2(CoordinateMap map) {
        this.map = map;
    }

    public CoordinateMap map() {
        return map;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    @Override
    public Location[] sense() {
        Location[] result = new Location[3];

        result[0] = new Location(look(position, position.getHeading() - AngleUtils.RADIANS_90).minus(position));
        result[1] = new Location(look(position, position.getHeading()).minus(position));
        result[2] = new Location(look(position, position.getHeading() + AngleUtils.RADIANS_90).minus(position));
 
        return result;
    }

    private Location look(Location position, double heading) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Scanning heading: {} {}", heading, Math.toDegrees(heading));
        }
        return map.look(position, heading, 350).orElse(new Location(CoordUtils.fromAngle(heading, Double.POSITIVE_INFINITY)));
    }

    @Override
    public double maxRange() {
        return MAX_RANGE;
    }

}
