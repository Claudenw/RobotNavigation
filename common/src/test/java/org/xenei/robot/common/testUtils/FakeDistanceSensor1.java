package org.xenei.robot.common.testUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.Position;
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

    @Override
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
            double h = position.getHeading() + (RADIANS * i);
            System.out.format( "\nLooking %s (%s)\n", h, Math.toDegrees(h) );
            Location nxt = look(position, position.getHeading() + (RADIANS * i));
            System.out.println(nxt);
            double heading = position.headingTo(nxt) - position.getHeading();
            double d = position.distance(nxt);
            System.out.println( String.format("%s %s %s", heading, Math.toDegrees(heading),d));
            result[i] = Location.from(CoordUtils.fromAngle(heading, d));
            System.out.println(result[i]);
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
                .orElse(Location.from(CoordUtils.fromAngle(heading, Double.POSITIVE_INFINITY)));
    }

    @Override
    public double maxRange() {
        return MAX_RANGE;
    }
}
