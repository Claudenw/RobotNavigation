package org.xenei.robot.planner;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.Stack;
import java.util.stream.Stream;

import org.apache.jena.util.iterator.UniqueFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.common.AngleUnits;
import org.xenei.robot.common.Coordinates;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.Sensor;
import org.xenei.robot.planner.rdf.Namespace;

public class Planner {
    private static final Logger LOG = LoggerFactory.getLogger(Planner.class);
    private final Stack<Coordinates> target;
    private final Sensor sensor;
    private final PlannerMap map;
    private Position currentPosition;
    private Solution solution;
    private ObstacleMapper obstacleMapper = new ObstacleMapper();

    /**
     * Constructs a planner.
     * 
     * @param sensor the sensor to sense environment.
     * @param startPosition the starting position.
     */
    public Planner(Sensor sensor, Coordinates startPosition) {
        this(sensor, startPosition, null);
    }

    /**
     * Constructs a planner.
     * 
     * @param sensor the sensor to sense environment.
     * @param startPosition the starting position.
     * @param target the coordinates of the target to reach.
     */
    public Planner(Sensor sensor, Coordinates startPosition, Coordinates target) {
        this.sensor = sensor;
        this.map = new PlannerMap();
        this.target = new Stack<>();
        if (target != null) {
            setTarget(target);
        }
        restart(startPosition);
    }

    /**
     * Sets the current position and resets the solution.
     * 
     * @param position the new current position.
     */
    public void changeCurrentPosition(Position position) {
        currentPosition = position.quantize();
        map.add(position.coordinates(), position.coordinates().distanceTo(target.peek()));
    }

    /**
     * Sets the current position and resets the solution.
     * 
     * @param coords the new current position.
     */
    public void restart(Coordinates coords) {
        double distance = Double.NaN;
        double angle = 0.0;
        if (getTarget() != null) {
            distance = coords.distanceTo(getTarget());
            angle = coords.angleTo(getTarget());
        }
        currentPosition = new Position(coords, angle);
        map.add(currentPosition.coordinates(), distance);
        resetSolution();
    }

    private void resetSolution() {
        solution = new Solution();
        if (currentPosition != null) {
            solution.add(currentPosition.coordinates());
        }
    }

    /**
     * Call the sensors, record obstacles, and return a stream of valid points to
     * add. Also sets the obstacleMapper if a collision with the current path was
     * detected.
     */
    Stream<Coordinates> sense() {
        // next target set if collision detected.
        obstacleMapper.reset();
        LOG.trace("Sense position: {}", currentPosition);
        // get the sensor readings and add arcs to the map
        //@formatter:off
        return Arrays.stream(sensor.sense(currentPosition))
                // filter out any range < 1.0
                .filter(c -> c.getRange() > 1.0)
                // create absolute coordinates
                .map(c -> {
                    LOG.trace( "Checking {}", c);
                    return currentPosition.coordinates().plus(c).quantize();
                })
                .filter( new UniqueFilter<Coordinates>() )
                .map(obstacleMapper::map)
                // filter out non entries
                .filter(c -> c.isPresent() && !c.get().equals(currentPosition.coordinates()))
                .map(Optional::get)
                .filter( new UniqueFilter<Coordinates>() );
        //@formatter::on
    }
    

    /**
     * Registers the obstacle on the map and checks if it is on the path we are currently traveling.
     * @param position the position we are currently at.
     * @param obstacle the obstacle we may hit.
     * @return true if the obstacle is on our path.
     */
    private boolean registerObstacle(Coordinates obstacle) {
        map.setObstacle(obstacle);
        boolean result = !currentPosition.hasClearView(getTarget(), obstacle);
        if (result) {
            LOG.info("Future collision from {} detected at {}", currentPosition, obstacle);
        }
        return result;
    }

    /**
     * Finds an open position closer to the position from the badCell
     * @param badCell the position of an obstacle or other location not to be in.
     * @return An optional that contains the nearest open coordinates if any.
     */
    private Optional<Coordinates> not(Coordinates badCell) {
        Coordinates direct0 = badCell.minus(currentPosition.coordinates());
        Coordinates direct = Coordinates.fromAngle(direct0.getTheta(AngleUnits.RADIANS), AngleUnits.RADIANS, direct0.getRange() - 1);
        Coordinates qCandidate = currentPosition.coordinates().plus(direct).quantize();
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

    public Solution getSolution() {
        return solution;
    }
    
    public Position getPosition() {
        return currentPosition;
    }

    /**
     * Plans a step.  Returns the best location to move to based on the current position.
     * The target position may be updated.
     * The best position to head for is in the target.
     * @return true if the target has not been reached. (processing should continue)
     */
    public boolean step() {
        if (currentPosition.coordinates().equals(target.peek())) {
            target.pop();
            if (target.isEmpty()) {
                solution.add( currentPosition.coordinates());
                return false;
            }
            // see if we can get to target directly
            if (map.clearView(currentPosition.coordinates(), target.peek())) {
                map.path(currentPosition.coordinates(), target.peek());
                currentPosition.setHeading(target.peek());
            }
            // recalculate the distances
            map.recalculate(target.peek());
            // update the planning model make sure we don't revisit where we have been.
            solution.stream().forEach(t -> map.update(Namespace.PlanningModel, t, Namespace.adjustment, Double.POSITIVE_INFINITY));
            // add the current location to the solution.
            solution.add(currentPosition.coordinates());
        }
        
        sense().forEach(this::recordMapPoint);
        // collision was detected
        if (obstacleMapper.nextTarget.isPresent()) {
            map.cutPath(solution.end(), target.peek());
            map.path(solution.end(), obstacleMapper.nextTarget.get());
        }

        Optional<PlanRecord> selected = map.getBest(currentPosition.coordinates());
        if (selected.isPresent()) {
            // update the planning model for the current position.
           // map.update(Namespace.PlanningModel, currentPosition.coordinates(), Namespace.distance, selected.get().cost());
            // if we are not at the target then make the next position the target.
            if (!selected.get().coordinates().equals(target.peek())) {
                target.push(selected.get().coordinates());
            }
            // if the heading changes mark the point on the current location
            if (currentPosition.getHeading(AngleUnits.RADIANS) != currentPosition.coordinates().angleTo(selected.get().coordinates())) {
                solution.add(currentPosition.coordinates());
            }
        }
        return true;
    }
    
    private void recordMapPoint(Coordinates c) {
        map.add(c, c.distanceTo(target.peek()));
        map.path(currentPosition.coordinates(), c);
    }

    /**
     * Set the target for the planner.  
     * Setting the target causes the current plan to be cleared and a new plan started.
     * @param target The coordinates to head toward.
     */
    public void setTarget(Coordinates target) {
        LOG.info("Setting target to {} starting from {}", target, currentPosition);
        this.target.clear();
        this.target.push(target.quantize());
        if (currentPosition != null) {
            currentPosition.setHeading(target);
        }
        map.reset(getTarget());
        resetSolution();
    }
    
    public Coordinates getTarget() {
        return target.isEmpty() ? null : target.peek();
    }

    /**
     * For testing only
     * @return
     */
    public PlannerMap getMap() {
         return map;
    }
    
    public void recordSolution() {
        solution.simplify(map);
        Coordinates[] previous = { null };
        solution.stream().forEach( c -> { 
            if (previous[0] != null) {
                map.path(Namespace.BaseModel, previous[0], c);
            }
            previous[0] = c;
        });
    }
    
    private class ObstacleMapper {
        private Optional<Coordinates> nextTarget = Optional.empty();
        private double d = 0.0;
        
        public Optional<Coordinates> map(Coordinates obstacle) {
            LOG.debug("Sensed {}", obstacle);
            boolean collisionDetected = registerObstacle(obstacle);
            Optional<Coordinates> result = not(obstacle);
            if (collisionDetected) {
                if (result.isPresent()) {
                    if (nextTarget.isEmpty()) {
                        nextTarget = result;
                        d = result.get().distanceTo(currentPosition.coordinates());
                    } else {
                        double n = result.get().distanceTo(currentPosition.coordinates());
                        if (n<d) {
                            d = n;
                            nextTarget = result;
                        }
                    }
                }
            }
            return result;
        }
        
        public void reset() {
            nextTarget = Optional.empty();
            d = 0.0;
        }
    }

}
