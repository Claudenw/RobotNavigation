package org.xenei.robot;

import org.locationtech.jts.geom.Coordinate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.common.AbortedException;
import org.xenei.robot.common.BumpSensor;
import org.xenei.robot.common.DistanceSensor;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.Mover;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.mapping.Map;
import org.xenei.robot.common.planning.Solution;
import org.xenei.robot.common.utils.RobutContext;
import org.xenei.robot.mapper.MapBumpSensorAdapter;
import org.xenei.robot.mapper.MapDistanceSensorAdapter;
import org.xenei.robot.mapper.MapImpl;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class Robut {

    private final Supplier<Position> positionSupplier;
    private final Processor processor;

    private static final Logger LOG = LoggerFactory.getLogger(Robut.class);

    public Robut(RobutContext ctxt, BumpSensor bumpSensor, DistanceSensor distSensor, Mover mover) throws InterruptedException {

        bumpSensor.addListener(mover.getBumpSensorListener());
        positionSupplier = mover::position;
        MapImpl map = new MapImpl(ctxt);
        bumpSensor.addListener(new MapBumpSensorAdapter(map, positionSupplier));
        distSensor.addListener(new MapDistanceSensorAdapter(map, positionSupplier));
        this.processor = new Processor(ctxt, mover, positionSupplier, map);
        distSensor.addListener(processor.getMapper().getRelativeObstacleConsumer());
        ctxt.scheduleAtFixedRate(bumpSensor, 500, 42, TimeUnit.MILLISECONDS);
        ctxt.scheduleAtFixedRate(distSensor, 500, 250, TimeUnit.MILLISECONDS);
    }

    public void moveTo(Location relativeLocation) throws AbortedException {
        Location nextCoord = positionSupplier.get().nextPosition(relativeLocation);
        processor.moveTo(nextCoord);
    }

    public Map.VisualizationInitializer visualizationInitializer() {
        return new Map.VisualizationInitializer() {
            @Override
            public Map map() {
                return processor.map;
            }

            @Override
            public Supplier<Solution> solutionSupplie() {
                return processor.getPlanner()::getSolution;
            }

            @Override
            public Supplier<Position> positionSupplier() {
                return positionSupplier;
            }

            @Override
            public Supplier<Coordinate> targetSupplier() {
                return processor.getPlanner()::getTarget;
            }
        };
    }
}
