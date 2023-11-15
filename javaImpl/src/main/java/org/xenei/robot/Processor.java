package org.xenei.robot;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.navigation.Coordinates;
import org.xenei.robot.navigation.Position;
import org.xenei.robot.planner.Planner;
import org.xenei.robot.utils.Mover;
import org.xenei.robot.utils.Sensor;

public class Processor implements Runnable {
    private final Mover mover;
    private final Planner planner;
    private boolean running;
    private static final Logger LOG = LoggerFactory.getLogger(Processor.class);

    public Processor(Sensor sensor, Mover mover) {
        this.mover = mover;
        this.planner = new Planner(sensor);

    }

    public void setTarget(Coordinates target) {
        planner.setTarget(target, mover.position());
    }

    public void stop() {
        this.running = false;
    }

    /**
     * Calculates the next step and moves to it.
     * @return The location after the movement. or empty if there is no move to take.
     */
    public Optional<Position> step() {
        if (planner.step(mover.position()))
        {
            Coordinates nextLoc = planner.getTarget();
                if (nextLoc == null) {
            return Optional.empty();
        }
        
        
        Optional<Position> result = Optional.of(mover.move(nextLoc.minus(mover.position().coordinates())));
        LOG.info("next step {}", result);
        return result;
        }
        else {
            return Optional.empty();
        }
    }

    @Override
    public void run() {
        running = true;
        while (running) {
            step();
        }
    }

    public Planner getPlanner() {
        return planner;
    }
}
