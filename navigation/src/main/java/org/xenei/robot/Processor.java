package org.xenei.robot;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.common.Coordinates;
import org.xenei.robot.common.Mover;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.Sensor;
import org.xenei.robot.planner.Planner;

public class Processor {
    private final Mover mover;
    private final Planner planner;
    private MoveRunner runner = new MoveRunner();
    private static final Logger LOG = LoggerFactory.getLogger(Processor.class);
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
   
    private final int MAX_STEPS = 1000;

    public Processor(Sensor sensor, Mover mover) {
        this.mover = mover;
        this.planner = new Planner(sensor, mover.position().coordinates());
    }

    public void setTarget(Coordinates target) {
        planner.setTarget(target);
    }

    public void stop() {
        if (runner.isRunning) {
            runner.isRunning = false;
        }
    }
    
    public boolean isMoving() {
        return runner.isRunning;
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

    public void moveTo(Coordinates coord) {
        setTarget(coord);
        if (!runner.isRunning) {
            executor.execute(runner);
        }
    }

    public Stream<Coordinates> getSolution() {
        return planner.getSolution().stream();
    }

    public Planner getPlanner() {
        return planner;
    }
    
    private class MoveRunner implements Runnable {
        boolean isRunning = false;
        @Override
        public void run() {
            isRunning = true;
            try {
                Optional<Position> p = Optional.of(mover.position());
                int steps = 0;
                while (isRunning && p.isPresent()) {
                    if (steps > MAX_STEPS) {
                        LOG.error("Unable to find path in {} steps", MAX_STEPS);
                        return;
                    }
                    p = step();
                }
                if (isRunning) {
                    planner.recordSolution();
                }
            } finally {
                isRunning = false;
            }
        }
    }
}
