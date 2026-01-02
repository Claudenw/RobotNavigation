package org.xenei.robot.common.testUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import io.nats.client.Nats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.mapping.Map;
import org.xenei.robot.common.mapping.ThetaAndRange;
import org.xenei.robot.common.planning.Solution;
import org.xenei.robot.common.utils.DoubleUtils;
import org.xenei.robot.common.utils.RobutContext;
import org.xenei.robot.mapper.visualization.TextViz;

public class FakeDistanceSensor1 implements FakeDistanceSensor {
    private static final Logger LOG = LoggerFactory.getLogger(FakeDistanceSensor1.class);
    private static final int BLOCKSIZE = 17;
    private static final double RADIANS = Math.toRadians(360.0 / BLOCKSIZE);
    private final Map map;
    private static final double MAX_RANGE = 15;
    private final Supplier<Position> positionSupplier;
    private Position lastPosition;

    public FakeDistanceSensor1(Map map, Supplier<Position> positionSupplier) {
        this.map = map;
        this.positionSupplier = positionSupplier;
    }

    @Override
    public Map map() {
        return map;
    }

    boolean samePosition(Position pos1, Position pos2) {
        if (pos1 == null) {
            return pos2 == null;
        }
        if (pos2 == null) {
            return false;
        }
        return DoubleUtils.eq(pos1.heading(), pos2.heading(), map.getContext().scaleInfo.getResolution())
                && DoubleUtils.eq(pos1.getX(), pos2.getX(), map.getContext().scaleInfo.getResolution())
                && DoubleUtils.eq(pos1.getY(), pos2.getY(), map.getContext().scaleInfo.getResolution());
    }

    @Override
    public void run() {
        Position position = positionSupplier.get();
        if (!samePosition(position, lastPosition)) {
            lastPosition = position;
            ThetaAndRange[] result = new ThetaAndRange[BLOCKSIZE];
            for (int i = 0; i < BLOCKSIZE; i++) {
                result[i] = look(position, position.heading() + (RADIANS * i));
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Reading {}: {}", i, result[i]);
                }
            }

            List<ThetaAndRange> resultList = Arrays.stream(result).filter(thetaAndRange -> {
                        return thetaAndRange.range() < maxRange();
                    })
                    .collect(Collectors.toList());
            map.getContext().distanceSensorTopic.send(new Readings(position, resultList));
        }
    }

    @Override
    public double maxRange() {
        return MAX_RANGE;
    }
}
