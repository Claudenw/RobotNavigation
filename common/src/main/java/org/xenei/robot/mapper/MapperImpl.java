package org.xenei.robot.mapper;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.common.sensor.distance.DistanceSensor;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.ScaleInfo;
import org.xenei.robot.common.mapping.MapCoordinate;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.mapping.Map;
import org.xenei.robot.common.mapping.Mapper;
import org.xenei.robot.common.mapping.ThetaAndRange;

public class MapperImpl implements Mapper {
    private static final Logger LOG = LoggerFactory.getLogger(MapperImpl.class);
    private final Map map;

    /**
     *
     * @param map
     *            The map to work with.     * @param targetSupplier
     *            a target supplier scaled to the map.
     */
    public MapperImpl(Map map) {
        this.map = map;

        // wire the mapper to the distance sensor
        map.getContext().distanceSensorTopic.listen(getRelativeObstacleProcessor());

    }

    @Override
    public Consumer<DistanceSensor.Readings> getRelativeObstacleProcessor() {
        return readings -> {
            MapCoordinate mapCoordinate = map.asMapCoordinate(readings.origin());
            readings.readings().forEach(relativeLocation -> {
                LOG.debug("Processing reading {}", relativeLocation);
                createObstacle(mapCoordinate, readings.origin().heading(), relativeLocation);
            });
        };
    }

    @Override
    public boolean isClearPath(Position currentPosition, Location target) {
        return map.isClearPath(map.asMapPosition(currentPosition), map.asMapLocation(target));
    }

    /**
     * Adds coordinates to the map that are near a registered obstacle.
     * @param mapCoordinate the coordinate on the map of the position when the sensor reading was taken.
     * @param heading the heading of the robut when the sensor reading was taken.
     * @param relativeLocation the location of the obstacle
     */
    private CompletableFuture<?> createObstacle(final MapCoordinate mapCoordinate, final double heading, final Location relativeLocation) {
        double distance = relativeLocation.range() - map.getContext().scaledRadius;
        Position positionAtSensorReading = Position.asPosition(mapCoordinate, heading);
        if (distance >= map.getContext().scaledRadius) {
            ScaleInfo scaleInfo = map.getContext().scaleInfo;
            MapCoordinate obstacle = map.asMapCoordinate(positionAtSensorReading.nextPosition(relativeLocation));
            return map.createObstacleInBackground(obstacle)
                    .thenAccept(mapObstacle -> {
                        double range = mapObstacle.getGeometry().distance(mapCoordinate.getGeometry());
                        MapCoordinate proxyObstacle = map.asMapCoordinate(new ThetaAndRange(relativeLocation.theta(), range));
                        if (scaleInfo.compare(ScaleInfo.OP.GT, proxyObstacle.distance(positionAtSensorReading), scaleInfo.getResolution())) {
                            ThetaAndRange thetaAndRange = new ThetaAndRange(relativeLocation.theta(), range - scaleInfo.getResolution());
                            MapCoordinate target = map.asMapCoordinate(positionAtSensorReading.nextPosition(thetaAndRange));
                            // if target is not in an obstacle add it as a target
                            if (!map.isObstacle(target)) {
                                map.asMapLocation(target);
                            }
                        }
                    });
        }
        return CompletableFuture.completedFuture(null);
    }
}
