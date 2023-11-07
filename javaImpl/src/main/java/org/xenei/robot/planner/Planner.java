package org.xenei.robot.planner;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.navigation.Coordinates;
import org.xenei.robot.navigation.Position;
import org.xenei.robot.utils.Sensor;

public class Planner {
    private static final Logger LOG = LoggerFactory.getLogger(Planner.class);
    private Coordinates target;
    private final Sensor sensor;
    private final PlannerMap map;
    private final ReentrantLock lock;
    private final Set<Coordinates> sensed;
    private final Stack<PlanRecord> path;
   

    public Planner(Sensor sensor) {
        this.sensor = sensor;
        this.map = new PlannerMap();
        this.lock = new ReentrantLock();
        this.sensed = new HashSet<>();
        this.path = new Stack<>();
    }

    private void sense(Position position) {
        Coordinates[] sensedAry = sensor.sense(position);
        Arrays.stream( sensedAry).map( c -> c.plus(position)).forEach(sensed::add);
        if (LOG.isDebugEnabled()) {
            for (Coordinates sensed : sensedAry) {
                LOG.debug("Sensed {}", sensed);
                Coordinates adjusted = sensed.minus(Coordinates.fromRadians(sensed.getThetaRadians(), 1))
                        .plus(position);
                LOG.debug("Adjusted {}", adjusted);
                map.add(new PlanRecord(adjusted, adjusted.distanceTo(target)));
            }
        } else {
            Arrays.stream(sensedAry).map(c -> c.minus(Coordinates.fromRadians(c.getThetaRadians(), 1)))
                    .map(c -> c.plus(position)).map(c -> new PlanRecord(c, c.distanceTo(target))).forEach(map::add);
        }
    }

    public PlanRecord getPlanRecord(Coordinates location) {
        return map.getPlanRecord(location);
    }
    
    public Collection<Coordinates> getPlanRecords() {
        return map.getPlanRecords();
    }
    
    public Collection<Coordinates> getPath() {
        return path.stream().map(PlanRecord::position).collect(Collectors.toList());
    }
    
    public Coordinates step(Position position) {
        lock.lock();
        try {
            //map.remove(position);
            sense(position);
            Optional<PlanRecord> result = map.getBest(position);
            if (result.isPresent()) {
                Coordinates coords = result.get().position();
                double distance = path.peek().position().distanceTo(coords);
                path.stream().forEach( p -> p.setMaskingCost( p.cost()+distance));
                path.push(result.get());
                return coords;
            }
            return null;
        } finally {
            lock.unlock();
        }
    }

    public void setTarget(Coordinates target, Position position) {
        lock.lock();
        path.clear();
        try {
            this.target = target;
            if (map.isEmpty()) {
                sense(position);
            } else {
                map.update(target);
            }
            PlanRecord planRecord = map.getPlanRecord(position);
            if (planRecord == null) {
                planRecord = new PlanRecord(position, position.distanceTo(target));
                map.add(planRecord);
            }
            path.push(planRecord);
        } finally {
            lock.unlock();
        }
    }
    
    public Set<Coordinates> getSensed() {
        return sensed;
    }
}
