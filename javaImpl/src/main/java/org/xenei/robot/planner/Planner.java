package org.xenei.robot.planner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.jena.util.iterator.UniqueFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.navigation.Coordinates;
import org.xenei.robot.navigation.Position;
import org.xenei.robot.utils.Sensor;


public class Planner {
    private static final Logger LOG = LoggerFactory.getLogger(Planner.class);
    private final Stack<Coordinates> target;
    private final Sensor sensor;
    private final PlannerMap map;
    private final ReentrantLock lock;
    //private final Set<Coordinates> sensed;
    private final Stack<PlanRecord> path;
    // experimentally ascertained in PositionTest.collisiontTest.
    private final static double POINT_RADIUS = 0.57;

    public Planner(Sensor sensor) {
        this(sensor, null);
    }

    /**
     * Constructor for testing and internal use. DO NOT USE
     * 
     * @param sensor
     */
    Planner(Sensor sensor, Coordinates target) {
        this.sensor = sensor;
        this.map = new PlannerMap();
        this.lock = new ReentrantLock();
        //this.sensed = new TreeSet<>(Coordinates.XYCompr);
        this.path = new Stack<>();
        this.target = new Stack<>();
        this.target.push(target);
    }

    /**
     * Call the sensors and add the results to the map.
     * Updates the paths as necessary.
     * 
     * @param position our current position.
     */
    Set<Coordinates> sense(Position position) {
        Position qPosition = position.quantize();
        boolean[] collisionFlag = { false, false };
        Set<Coordinates> result = new HashSet<Coordinates>();
        LOG.trace("Sense position: {}", qPosition);
        // get the sensor readings and add arcs to the map
        //@formatter:off
        Arrays.stream(sensor.sense(qPosition))
                // filter out any range < 1.0
                .filter(c -> c.getRange() > 1.0)
                // create absolute coordinates
                .map(c -> {
                    LOG.trace( "Checking {}", c);
                    return position.coordinates().plus(c).quantize();
                })
                .filter( new UniqueFilter<Coordinates>() )
                .map( obstacle -> {
                    LOG.debug("Sensed {}", obstacle);
                    collisionFlag[0] = registerObstacle(position, obstacle);
                    return not(obstacle, qPosition.coordinates());
                    })
                // filter out non entries
                .filter(c -> c.isPresent() && !c.get().equals(qPosition.coordinates()))
                // add to Map
               .forEach( o -> {
                   Coordinates c = o.get();
                   map.add( c, c.distanceTo(target.peek()));
                   if (collisionFlag[0]) {
                       collisionFlag[1] = true;
                       LOG.trace("Mapped {}", c);
                       if (!path.isEmpty()) {
                           map.path(path.peek().coordinates(), c );
                           map.cutPath(path.peek().coordinates(), target.peek());
                       }
                   }
                   result.add(c);
               });
        //@formatter::on
        return collisionFlag[1] || path.isEmpty() ? result : Collections.emptySet();
    }
    

    
    private boolean registerObstacle(Position position, Coordinates obstacle) {
        //sensed.add(obstacle);
        map.setObstacle(obstacle);

        boolean result = position.checkCollision(obstacle, POINT_RADIUS, sensor.maxRange());
        if (result) {
            LOG.info("Future collision detected at {}", obstacle);
        }
        return result;
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
        if (map.isObstacle(qCandidate)) {
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
                if (map.isObstacle(qCandidate)) {
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

    /**
     * Plans a step.  Returns the best location to move to basd on the current position.
     * The target position may be updated.
     * The the best direction is in the target
     * @param currentPosition 
     * @return true if the target has not been reached. (processing should continue)
     */
    public boolean step(Position currentPosition) {
        Position qPosition = currentPosition.quantize();
        lock.lock();
        try {
            if (currentPosition.equals(target.peek())) {
                target.pop();
                if (target.isEmpty()) {
                    return false;
                }
            }
            Set<Coordinates> newPoints = sense(currentPosition);
            if (!newPoints.isEmpty()) {
                // collision avoided recalculate
                path.push( new PlanRecord(qPosition.coordinates(), qPosition.coordinates().distanceTo(target.peek())));
                // add the new points
                for(Coordinates c : newPoints) {
                    map.add(c, c.distanceTo(target.peek()));
                    map.path(qPosition.coordinates(), c);
                }
                Optional<PlanRecord> selected = map.getBest(qPosition.coordinates());
                if (selected.isPresent()) {
                    map.updateTargetWeight(qPosition.coordinates(), selected.get().cost());
                    target.push(selected.get().coordinates());
                }
            }
            return true;
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
        LOG.info("Setting target to {} starting from {}", target, position);
        lock.lock();
        path.clear();
        this.target.clear();
        try {
            map.reset(target.quantize());;
            position = position.quantize();
            this.target.push(target);
            path.push(map.add(position.coordinates(), position.coordinates().distanceTo(target)));
        } finally {
            lock.unlock();
        }
    }
    
    public Coordinates getTarget() {
        return target.isEmpty() ? null : target.peek();
    }

    /**
     * Get the set of sensed records.  This is the set of all coordinates that
     * have been detected as having an obstacle.
     * @return the set of detected obstacles.
     */
    public Set<Coordinates> getSensed() {
        return Collections.unmodifiableSet(map.getObstacles());
    }
    
    /**
     * For testing only
     * @return
     */
    public PlannerMap getMap() {
         return map;
    }
   
}
