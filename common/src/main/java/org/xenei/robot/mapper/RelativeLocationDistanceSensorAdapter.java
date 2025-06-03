package org.xenei.robot.mapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.common.DistanceSensor;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.utils.CoordUtils;

import java.util.function.Consumer;

public class RelativeLocationDistanceSensorAdapter implements Consumer<DistanceSensor.DistanceReading> {
    private static final Logger LOG = LoggerFactory.getLogger(RelativeLocationDistanceSensorAdapter.class);
    final Consumer<Location> consumer;

    public RelativeLocationDistanceSensorAdapter(final Consumer<Location> consumer) {
        this.consumer = consumer;
    }

    @Override
    public void accept(DistanceSensor.DistanceReading reading) {
        Location relativeObstacle = Location.from(CoordUtils.fromAngle(reading.theta(), reading.range()));
        consumer.accept(relativeObstacle);
    }

}
