package org.xenei.robot;

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

    public Position step() {
        Coordinates nextLoc = planner.step(mover.position());
        Coordinates move = mover.position().minus(nextLoc);
        return mover.move(move);
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
