package org.xenei.robot.mapper;

import org.apache.commons.lang3.tuple.Pair;
import org.locationtech.jts.geom.Coordinate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.common.sensor.bump.BumpSensor;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.mapping.Map;
import org.xenei.robot.common.utils.AngleUtils;
import org.xenei.robot.common.utils.CoordUtils;

import java.util.HashMap;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

/**
 * Listens for Bump sensor notices and creates Obstacles in the Map.
 */
public class MapBumpSensorAdapter implements IntConsumer {
    private static final Logger LOG = LoggerFactory.getLogger(MapBumpSensorAdapter.class);
    private final Map map;
    private final Supplier<Position> positionSupplier;

    private final static HashMap<BumpSensor.State, Pair<Double, Double>> ANGLES = new HashMap<>();
    static {
        double start = 0;
        double end = AngleUtils.RADIANS_45;
        ANGLES.put(BumpSensor.State.RIGHT_FRONT_CORNER, Pair.of(start, end));
        start = end;
        end += AngleUtils.RADIANS_45;
        ANGLES.put(BumpSensor.State.RIGHT_SIDE_FRONT, Pair.of(start, end));
        start = end;
        end += AngleUtils.RADIANS_45;
        ANGLES.put(BumpSensor.State.RIGHT_SIDE_REAR, Pair.of(start, end));
        start = end;
        end += AngleUtils.RADIANS_45;
        ANGLES.put(BumpSensor.State.RIGHT_REAR_CORNER, Pair.of(start, end));
        start = end;
        end += AngleUtils.RADIANS_45;
        ANGLES.put(BumpSensor.State.LEFT_REAR_CORNER, Pair.of(start, end));
        start = end;
        end += AngleUtils.RADIANS_45;
        ANGLES.put(BumpSensor.State.LEFT_SIDE_REAR, Pair.of(start, end));
        start = end;
        end += AngleUtils.RADIANS_45;
        ANGLES.put(BumpSensor.State.LEFT_SIDE_FRONT, Pair.of(start, end));
        start = end;
        end += AngleUtils.RADIANS_45;
        ANGLES.put(BumpSensor.State.LEFT_FRONT_CORNER, Pair.of(start, end));
    }

    public MapBumpSensorAdapter(final Map map, Supplier<Position> positionSupplier) {
        this.map = map;
        this.positionSupplier = positionSupplier;
    }

    @Override
    public void accept(int bumpSensorMap) {
        if (bumpSensorMap == 0) {
            return;
        }
        byte bumpMap = (byte) bumpSensorMap;
        Position position = positionSupplier.get();
        LOG.debug("Sense position: {}", position);

        double range = map.getContext().chassisInfo.radius * 2;
        for (BumpSensor.State state : BumpSensor.State.values()) {
            if (state.match(bumpMap)) {
                Pair<Double, Double> thetas = ANGLES.get(state);
                Coordinate start = CoordUtils.add(position.getCoordinate(), CoordUtils.fromAngle(thetas.getLeft(), range));
                Coordinate end = CoordUtils.add(position.getCoordinate(), CoordUtils.fromAngle(thetas.getRight(), range));
                map.createObstacleInBackground(map.asMapCoordinate(start), map.asMapCoordinate(end));
            }
        }
    }
}
