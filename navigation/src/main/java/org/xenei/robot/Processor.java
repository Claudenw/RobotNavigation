package org.xenei.robot;

import java.util.Optional;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.common.Coordinates;
import org.xenei.robot.common.Map;
import org.xenei.robot.common.Mapper;
import org.xenei.robot.common.Mover;
import org.xenei.robot.common.Planner;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.Sensor;
import org.xenei.robot.mapper.MapperImpl;
import org.xenei.robot.mapper.PlannerMap;
import org.xenei.robot.planner.PlannerImpl;

public class Processor {
    private final Mover mover;
    private final Sensor sensor;
    private final Planner planner;
    private final Mapper mapper;
    private final Map map;
    private static final Logger LOG = LoggerFactory.getLogger(Processor.class);

    private final int MAX_STEPS = 1000;

    public Processor(Sensor sensor, Mover mover) {
        this.mover = mover;
        this.sensor = sensor;
        this.map = new PlannerMap();
        this.mapper = new MapperImpl(map);
        this.planner = new PlannerImpl(map, mover.position().coordinates());
    }

    public void setTarget(Coordinates target) {
        planner.setTarget(target);
    }

    /**
     * Calculates the next step and moves to it.
     * 
     * @return The location after the movement. or empty if there is no move to
     * take.
     */
    private Optional<Position> step() {
        if (planner.step()) {
            Coordinates nextLoc = planner.getTarget();
            if (nextLoc == null) {
                return Optional.empty();
            }

            Optional<Position> result = Optional.of(mover.move(nextLoc.minus(mover.position().coordinates())));
            LOG.info("next step {}", result);
            if (result.isPresent()) {
                planner.changeCurrentPosition(result.get());
            }
            return result;
        }
        return Optional.empty();
    }

    public boolean moveTo(Coordinates coord) {
        setTarget(coord);
        Optional<Position> p = Optional.of(mover.position());
        int steps = 0;
        while (p.isPresent()) {
            if (steps > MAX_STEPS) {
                LOG.error("Unable to find path in {} steps", MAX_STEPS);
                return false;
            }
            p = step();
        }
        return true;
    }
    
    public void recordSolution() {
        map.recordSolution(planner.getSolution());
    }

    public Stream<Coordinates> getSolution() {
        return planner.getSolution().stream();
    }

    public Planner getPlanner() {
        return planner;
    }

}
