package org.xenei.robot.planner;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
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
        this(sensor, null);
    }

    /**
     * Constructor for testing and internal use.  DO NOT USE
     * @param sensor
     */
    Planner(Sensor sensor, Coordinates target) {
        this.sensor = sensor;
        this.map = new PlannerMap();
        this.lock = new ReentrantLock();
        this.sensed = new TreeSet<>(Coordinates.XYCompr);
        this.path = new Stack<>();
        this.target = target;
    }

    /**
     * Call the sensors and add the results to the map.
     * 
     * @param position our current position.
     */
    void sense(Position position) {
        Position qPosition = position.quantize();
        // get the sensor readings and add arcs to the map
        //@formatter:off
        Arrays.stream(sensor.sense(position))
                // filter out any range < 1.0
                .filter(c -> c.getRange() > 1.0)
                // create absolute coordinates
                .map(c -> {
                    LOG.debug( "Checking "+c.toString());
                    Coordinates result = position.coordinates().plus(c).quantize();
                    return result;
                    })
                // add the coordinates to the sensed list.
                .filter( sensed::add )
                // if the coordinate is in the map remove it.
                .filter(c -> {
                    LOG.debug("Sensed {}", c);
                    map.remove(c);
                    return true;
                })
                // adjust to a free location
                .map(c -> not(c, qPosition.coordinates()))
                // filter out non entries
                .filter(c -> c.isPresent())
                // add to Map
               .forEach( o -> {
                   Coordinates c = o.get();
                   Coordinates qP = qPosition.coordinates();
                   LOG.debug("Mapped {}", c);
                   map.add( c, c.distanceTo(target));
                   map.path( c, qP );
               });
        //@formatter::on
    }

    /**
     * Finds an open position closer to the position from the badCell
     * @param badCell the position not be be in.
     * @param position The location to be closer to.
     * @return Either an open cell or an empty optional.
     */
    private Optional<Coordinates> not(Coordinates badCell, Coordinates position) {
        Coordinates direct0 = badCell.minus(position);
        Coordinates direct = Coordinates.fromRadians(direct0.getThetaRadians(), direct0.getRange() - 1);
        Coordinates qCandidate = position.plus(direct).quantize();
        if (sensed.contains(qCandidate)) {
            Coordinates adjustment = qCandidate.minus(badCell);
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

    public Collection<PlanRecord> getPlanRecords() {
        return map.getPlanRecords();
    }

    public List<PlanRecord> getPath() {
        return Collections.unmodifiableList(path);
    }

    public void updatePath(Coordinates coord) {
        Coordinates qC = coord.quantize();
        PlanRecord pr = new PlanRecord(qC, qC.distanceTo(target));
        map.add(pr.coordinates(), pr.cost());
        if (!path.isEmpty()) {
            map.path(path.peek().coordinates(), qC);
        }
        path.push(pr);
    }

    /**
     * Plans a step.  Returns the best location to move to basd on the current position.
     * @param currentPosition 
     * @return Coordinates of the best location.
     */
    public Optional<Coordinates> step(Position currentPosition) {
        Position qPosition = currentPosition.quantize();
        lock.lock();
        try {
            sense(currentPosition);
            return map.getBest(qPosition.coordinates()).map(PlanRecord::coordinates);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Set the target for the planner.  
     * Setting the target causes the current plan path to be cleared and a new plan 
     * started.
     * @param target The coordinates to head toward.
     * @param position the current position.
     */
    public void setTarget(Coordinates target, Position position) {
        lock.lock();
        path.clear();
        try {
            map.reset(target.quantize());;
            position = position.quantize();
            sense(position);
            path.push(map.add(position.coordinates(), position.coordinates().distanceTo(target)));
            PlanRecord planRecord = new PlanRecord(position.coordinates(), position.coordinates().distanceTo(target));
            path.push(planRecord);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Get the set of sensed records.  This is the set of all coordinates that
     * have been detected as having an obstacle.
     * @return the set of detected obstacles.
     */
    public Set<Coordinates> getSensed() {
        return Collections.unmodifiableSet(sensed);
    }
}
