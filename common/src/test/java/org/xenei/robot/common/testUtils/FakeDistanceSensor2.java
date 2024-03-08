package org.xenei.robot.common.testUtils;

import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.mapping.CoordinateMap;
import org.xenei.robot.common.utils.CoordUtils;

public class FakeDistanceSensor2 implements FakeDistanceSensor {
    private static final Logger LOG = LoggerFactory.getLogger(FakeDistanceSensor2.class);
    private final CoordinateMap map;
    private final double angle;
    private static final double MAX_RANGE = 350;
    private final Supplier<Position> positionSupplier;
    

    public FakeDistanceSensor2(CoordinateMap map, double angle, Supplier<Position> positionSupplier) {
        this.map = map;
        this.angle = angle;
        this.positionSupplier = positionSupplier;
    }

    @Override
    public CoordinateMap map() {
        return map;
    }


    @Override
    public Location[] sense() {
        Position pos = positionSupplier.get();
        Location[] result = new Location[3];

        result[0] = Location.from(look(pos, pos.getHeading() - angle).minus(pos));
        result[1] = Location.from(look(pos, pos.getHeading()).minus(pos));
        result[2] = Location.from(look(pos, pos.getHeading() + angle).minus(pos));

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
