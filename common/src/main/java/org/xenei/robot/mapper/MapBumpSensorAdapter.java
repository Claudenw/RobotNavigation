package org.xenei.robot.mapper;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.common.BumpSensor;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.PositionI;
import org.xenei.robot.common.ScaleInfo;
import org.xenei.robot.common.mapping.Map;
import org.xenei.robot.common.utils.AngleUtils;
import org.xenei.robot.common.utils.CoordUtils;

import java.util.HashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class MapBumpSensorAdapter implements Consumer<BumpSensor.BumpState> {
    private static final Logger LOG = LoggerFactory.getLogger(MapBumpSensorAdapter.class);
    private final Map<?, ?, ?> map;
    private final Supplier<PositionI<?, ?>> positionSupplier;

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

    public MapBumpSensorAdapter(final Map map, Supplier<PositionI<?, ?>> positionSupplier) {
        this.map = map;
        this.positionSupplier = positionSupplier;
    }

    @Override
    public void accept(BumpSensor.BumpState bumpState) {
        if (bumpState.getValue() == 0) {
            return;
        }
        PositionI<?, ?> position = positionSupplier.get();
        LOG.debug("Sense position: {}", position);

        ScaleInfo scaleInfo = map.getContext().scaleInfo;

        double range = map.getContext().chassisInfo.radius * 2;
        for (BumpSensor.State state : BumpSensor.State.values()) {
            if (bumpState.is(state)) {
                Pair<Double, Double> thetas = ANGLES.get(state);
                Location relativeStart = new Location(scaleInfo.round(CoordUtils.fromAngle(thetas.getLeft(), range)));
                Location relativeEnd = new Location(scaleInfo.round(CoordUtils.fromAngle(thetas.getRight(), range)));
                map.addObstacle(map.createObstacle(position, relativeStart, relativeEnd));
            }
        }
    }
}
