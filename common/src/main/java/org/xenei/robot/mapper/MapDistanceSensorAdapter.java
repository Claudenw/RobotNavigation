package org.xenei.robot.mapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.common.DistanceSensor;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.ScaleInfo;
import org.xenei.robot.common.mapping.Map;
import org.xenei.robot.common.utils.CoordUtils;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class MapDistanceSensorAdapter implements Consumer<DistanceSensor.Readings> {
    private static final Logger LOG = LoggerFactory.getLogger(MapDistanceSensorAdapter.class);
    private final Map map;
    private final Supplier<Position> positionSupplier;

    public MapDistanceSensorAdapter(final Map map, final Supplier<Position> positionSupplier) {
        this.map = map;
        this.positionSupplier = positionSupplier;
    }

    public Map getMap() {
        return map;
    }

    @Override
    public void accept(DistanceSensor.Readings readings) {
        ScaleInfo scaleInfo = map.getContext().scaleInfo;
        readings.readings().stream().map(reading -> scaleInfo.round(reading.getLocation()))
                .filter(relativeLocation -> !relativeLocation.isNaN() && !relativeLocation.isInfinite())
                .map(relativeLocation -> map.createObstacle(readings.origin(), relativeLocation))
                .forEach(map::addObstacle);
    }
}
