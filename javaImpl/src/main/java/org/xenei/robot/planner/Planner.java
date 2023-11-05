package org.xenei.robot.planner;

import java.util.Arrays;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.navigation.Coordinates;
import org.xenei.robot.navigation.Position;
import org.xenei.robot.utils.Sensor;

public class Planner {
    private static final Logger LOG = LoggerFactory.getLogger(Planner.class);
    private Coordinates target;
    private Sensor sensor;
    private PlannerMap map;
    private ReentrantLock lock;

    public Planner(Sensor sensor) {
        this.sensor = sensor;
        this.map = new PlannerMap();
        this.lock = new ReentrantLock();
    }

    private void sense(Position position) {
        if (LOG.isDebugEnabled()) {
            for (Coordinates sensed : sensor.sense(position)) {
                LOG.debug("Sensed {}", sensed);
                Coordinates adjusted = sensed.minus(Coordinates.fromRadians(sensed.getThetaRadians(), 1))
                        .plus(position);
                LOG.debug("Adjusted {}", adjusted);
                map.add(new PlanRecord(adjusted, adjusted.distanceTo(target)));
            }
        } else {
            Arrays.stream(sensor.sense(position)).map(c -> c.minus(Coordinates.fromRadians(c.getThetaRadians(), 1)))
                    .map(c -> c.plus(position)).map(c -> new PlanRecord(c, c.distanceTo(target))).forEach(map::add);
        }
    }

    public Coordinates step(Position position) {
        lock.lock();
        try {
            sense(position);
            return map.getBest(position);
        } finally {
            lock.unlock();
        }
    }

    public void setTarget(Coordinates target, Position position) {
        lock.lock();
        map.add(new PlanRecord(position, position.distanceTo(target)));
        try {
            this.target = target;
            if (map.isEmpty()) {
                sense(position);
            } else {
                map.update(target);
            }
        } finally {
            lock.unlock();
        }
    }
}
