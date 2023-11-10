package org.xenei.robot;

import java.util.Optional;

import org.xenei.robot.navigation.Coordinates;
import org.xenei.robot.navigation.Position;
import org.xenei.robot.planner.Planner;
import org.xenei.robot.utils.Mover;
import org.xenei.robot.utils.Sensor;

public class Processor implements Runnable {
    private final Mover mover;
    private final Planner planner;
    private boolean running;

    public Processor(Sensor sensor, Mover mover) {
        this.mover = mover;
        this.planner = new Planner(sensor);

    }

    public void gotoTarget(Coordinates target) {
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
        Optional<Coordinates> nextLoc = planner.step(mover.position());
        if (nextLoc.isEmpty()) {
            return Optional.empty();
        }
        Optional<Position> result = Optional.of(mover.move(nextLoc.get().minus(mover.position().coordinates())));
        if (result.isPresent()) {
            planner.updatePath(result.get().coordinates().quantize());
        }
        return result;
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
