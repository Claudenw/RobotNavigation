package org.xenei.robot;

import org.locationtech.jts.geom.Coordinate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.common.DistanceSensor;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.mapping.Map;
import org.xenei.robot.common.planning.Solution;
import org.xenei.robot.common.utils.RobutContext;
import org.xenei.robot.mapper.MapDistanceSensorAdapter;
import org.xenei.robot.mapper.MapImpl;
import org.xenei.robot.mover.BaseMover;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class Robut {

    private final Supplier<Position> positionSupplier;
    private final Processor processor;

    private static final Logger LOG = LoggerFactory.getLogger(Robut.class);

    public Robut(RobutContext ctxt, DistanceSensor distSensor, BaseMover mover) throws InterruptedException {
        // wire the mover into the bump sensor
        positionSupplier = mover::position;
        MapImpl map = new MapImpl(ctxt);
        // wire the sensors into the map.
        distSensor.addListener(new MapDistanceSensorAdapter(map, positionSupplier));
        // create the processor
        this.processor = new Processor(mover, positionSupplier, map);
        // wire the mapper to the distance sensor
        distSensor.addListener(processor.getMapper().getRelativeObstacleConsumer());
        // schedule the sensors to sense
        ctxt.scheduleAtFixedRate(distSensor, 500, 250, TimeUnit.MILLISECONDS);
    }

    public void moveTo(Location relativeLocation) {
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
            public Supplier<Solution> solutionSupplier() {
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
