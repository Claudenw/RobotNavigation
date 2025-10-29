package org.xenei.robot.mapper;

import org.xenei.robot.common.DistanceSensor;
import org.xenei.robot.common.ScaleInfo;
import org.xenei.robot.common.mapping.Map;
import org.xenei.robot.common.utils.CoordUtils;

import java.util.function.Consumer;

public class MapDistanceSensorAdapter {

    public MapDistanceSensorAdapter() {
        // do not instantiate
    }

    public static Consumer<DistanceSensor.Readings> create(final Map map) {
        return readings -> {
            ScaleInfo scaleInfo = map.getContext().scaleInfo;
            readings.readings().stream().map(scaleInfo::round)
                    .filter(relativeLocation -> !relativeLocation.isNaN() && !relativeLocation.isInfinite())
                    .map(relativeLocation -> map.asMapCoordinate(CoordUtils.add(readings.origin().getCoordinate(), relativeLocation.getCoordinate())))
                    .forEach(map::createObstacleInBackground);
        };
    }
}
