package org.xenei.robot.mapper;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.common.DistanceSensor;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.mapping.MapCoordinate;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.mapping.Map;
import org.xenei.robot.common.mapping.Mapper;
import org.xenei.robot.common.mapping.ThetaAndRange;
import org.xenei.robot.common.utils.DoubleUtils;

public class MapperImpl implements Mapper {
    private static final Logger LOG = LoggerFactory.getLogger(MapperImpl.class);
    private final Map map;
    private final Supplier<Location> targetSupplier;

    /**
     *
     * @param map
     *            The map to work with.
     * @param targetSupplier
     *            a target supplier scaled to the map.
     */
    public MapperImpl(Map map, Supplier<Location> targetSupplier) {
        this.map = map;
        this.targetSupplier = targetSupplier;
    }

    @Override
    public Consumer<DistanceSensor.Readings> getRelativeObstacleConsumer() {
        return readings -> {
            MapCoordinate mapCoordinate = map.asMapCoordinate(readings.origin());

            readings.readings().forEach(relativeLocation -> {
                if (!DoubleUtils.inRange(relativeLocation.range(), map.getContext().chassisInfo.radius)) {
                    map.getContext().submit(obstacleMapper(mapCoordinate, relativeLocation));
                }
            });
        };
    }

    @Override
    public boolean isClearPath(Position currentPosition, Location target) {
        return map.isClearPath(map.asMapPosition(currentPosition), map.asMapLocation(target));
    }

    /**
     * Adds coordinates to the map that are near a registered obstacle.
     */
    private Runnable obstacleMapper(final MapCoordinate mapCoordinate, final Location relativeLocation) {
        return () -> {
            double distance = relativeLocation.range() - map.getContext().scaledRadius;
            if (distance >= map.getContext().scaledRadius) {
                MapCoordinate obstacle = mapCoordinate.nextCoordinate(relativeLocation);
                map.createObstacleInBackground(mapCoordinate.nextCoordinate(relativeLocation))
                        .thenAccept( mapObstacle -> {
                            MapCoordinate target = mapCoordinate.nextCoordinate(relativeLocation.theta(), distance);
                            // if target is not in an obstacle add it as a target
                            if (!map.isObstacle(target)) {
                                map.asMapLocation(target);
                            }
                        });
            }
        };
    }
}
