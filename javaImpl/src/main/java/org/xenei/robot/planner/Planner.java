package org.xenei.robot.planner;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;
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
        this.sensed = new TreeSet<>(Coordinates.XYCompr);
        this.path = new Stack<>();
    }

    /**
     * call the sensors and add the results to the map.
     * 
     * @param position our current position.
     */
    private void sense(Position position) {
        Position qPosition = position.quantize();
        Set<Coordinates> sensedSet = Arrays.stream(sensor.sense(position)).filter(c -> c.getRange() > 1.0)
                .map(c -> c.quantize().plus(qPosition.coordinates())).filter(sensed::add).filter(c -> {
                    LOG.debug("Sensed {}", c);
                    map.remove(c);
                    return true;
                }).collect(Collectors.toSet());
        for (Coordinates absolute : sensedSet) {
            Optional<Coordinates> free = not(absolute, qPosition.coordinates());
            if (free.isPresent()) {
                LOG.debug("Mapped {}", free.get());
                map.add(new PlanRecord(free.get(), free.get().distanceTo(target)));
            }
        }
    }

    private Optional<Coordinates> not(Coordinates pos, Coordinates position) {
        Coordinates direct0 = pos.minus(position);
        Coordinates direct = Coordinates.fromRadians(direct0.getThetaRadians(), direct0.getRange() - 1);
        Coordinates qCandidate = position.plus(direct).quantize();
        if (sensed.contains(qCandidate)) {
            Coordinates adjustment = qCandidate.minus(pos);
            int xAdj = 0;
            int yAdj = 0;
            if (adjustment.getX() == 0) {
                double signum = Math.signum(direct.getX());
                if (signum < 0) {
                    xAdj = 1;
                } else if (signum > 0) {
                    xAdj = -1;
                }
            }
            if (adjustment.getY() == 0) {
                double signum = Math.signum(direct.getY());
                if (signum < 0) {
                    yAdj = 1;
                } else if (signum > 0) {
                    yAdj = -1;
                }
            }
            if (xAdj != 0 || yAdj != 0) {
                qCandidate = Coordinates.fromXY(qCandidate.getX() + xAdj, qCandidate.getY() + yAdj);
                if (sensed.contains(qCandidate)) {
                    return Optional.empty();
                }
            }
        }
        return Optional.of(qCandidate);
    }

    public Collection<Coordinates> getPlanRecords() {
        return map.getPlanRecords();
    }

    public List<Coordinates> getPath() {
        return path.stream().map(PlanRecord::position).collect(Collectors.toList());
    }

    public void updatePath(Coordinates position) {
        double distance = path.peek().position().distanceTo(position);
        path.stream().forEach(p -> p.setMaskingCost(p.cost() + distance));
        path.push(map.getOrAddPlanRecord(position, target));
    }

    public Optional<Coordinates> step(Position position) {
        Position qPosition = position.quantize();
        lock.lock();
        try {
            // map.remove(position);
            sense(position);
            return map.getBest(qPosition.coordinates()).map(PlanRecord::position);
        } finally {
            lock.unlock();
        }
    }

    public void setTarget(Coordinates target, Position position) {
        lock.lock();
        path.clear();
        try {
            this.target = target;
            position = position.quantize();
            if (map.isEmpty()) {
                sense(position);
            } else {
                map.update(target);
            }
            PlanRecord planRecord = map.getOrAddPlanRecord(position.coordinates(), target);
            path.push(planRecord);
        } finally {
            lock.unlock();
        }
    }

    public Set<Coordinates> getSensed() {
        return sensed;
    }
}
