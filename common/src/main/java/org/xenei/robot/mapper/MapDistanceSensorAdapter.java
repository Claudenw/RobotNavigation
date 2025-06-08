package org.xenei.robot.mapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.common.DistanceSensor;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.ScaleInfo;
import org.xenei.robot.common.mapping.Map;
import org.xenei.robot.common.utils.CoordUtils;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class MapDistanceSensorAdapter implements Consumer<DistanceSensor.DistanceReading> {
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
    public void accept(DistanceSensor.DistanceReading reading) {
        Position position = positionSupplier.get();
        ScaleInfo scaleInfo = map.getContext().scaleInfo;
        Location relativeObstacle = scaleInfo.round(Location.from(CoordUtils.fromAngle(reading.theta(), reading.range())));
        if (!relativeObstacle.isNaN() && !relativeObstacle.isInfinite()) {
            map.addObstacle(map.createObstacle(position, relativeObstacle));
        }
    }
}
