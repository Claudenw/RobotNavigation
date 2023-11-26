package org.xenei.robot.planner;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.Stack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.common.AngleUnits;
import org.xenei.robot.common.Coordinates;
import org.xenei.robot.common.Map;
import org.xenei.robot.common.Planner;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.Solution;
import org.xenei.robot.common.Target;

public class PlannerImpl implements Planner {
    private static final Logger LOG = LoggerFactory.getLogger(PlannerImpl.class);
    private final Stack<Coordinates> target;
    private final Map map;
    private Position currentPosition;
    private Solution solution;

    /**
     * Constructs a planner.
     * 
     * @param sensor the sensor to sense environment.
     * @param startPosition the starting position.
     */
    public PlannerImpl(Map map, Coordinates startPosition) {
        this(map, startPosition, null);
    }

    /**
     * Constructs a planner.
     * 
     * @param sensor the sensor to sense environment.
     * @param startPosition the starting position.
     * @param target the coordinates of the target to reach.
     */
    public PlannerImpl(Map map, Coordinates startPosition, Coordinates target) {
        this.map = map;
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
    @Override
    public void changeCurrentPosition(Position position) {
        currentPosition = position.quantize();
        map.add(new Target(position.coordinates(), position.coordinates().distanceTo(target.peek())));
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
        map.add(new Target(currentPosition.coordinates(), distance));
        resetSolution();
    }

    private void resetSolution() {
        solution = new Solution();
        if (currentPosition != null) {
            solution.add(currentPosition.coordinates());
        }
    }

    public Collection<Target> getPlanRecords() {
        return map.getTargets();
    }

    @Override
    public Solution getSolution() {
        return solution;
    }

    @Override
    public Position getCurrentPosition() {
        return currentPosition;
    }

    /**
     * Plans a step. Returns the best location to move to based on the current
     * position. The target position may be updated. The best position to head for
     * is in the target.
     * 
     * @return true if the target has not been reached. (processing should continue)
     */
    @Override
    public boolean step() {
        if (currentPosition.coordinates().equals(target.peek())) {
            target.pop();
            if (target.isEmpty()) {
                solution.add(currentPosition.coordinates());
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
            solution.stream().forEach(t -> map.setTemporaryCost(new Target(t, Double.POSITIVE_INFINITY)));
            // add the current location to the solution.
            solution.add(currentPosition.coordinates());
        }

        Optional<Target> selected = map.getBestTarget(currentPosition.coordinates());
        if (selected.isPresent()) {
            // update the planning model for the current position.
            // map.update(Namespace.PlanningModel, currentPosition.coordinates(),
            // Namespace.distance, selected.get().cost());
            // if we are not at the target then make the next position the target.
            if (!selected.get().coordinates().equals(target.peek())) {
                target.push(selected.get().coordinates());
            }
            // if the heading changes mark the point on the current location
            if (currentPosition.getHeading(AngleUnits.RADIANS) != currentPosition.coordinates()
                    .angleTo(selected.get().coordinates())) {
                solution.add(currentPosition.coordinates());
            }
        }
        return true;
    }

    /**
     * Set the target for the planner. Setting the target causes the current plan to
     * be cleared and a new plan started.
     * 
     * @param target The coordinates to head toward.
     */
    @Override
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

    @Override
    public Coordinates getTarget() {
        return target.isEmpty() ? null : target.peek();
    }

    @Override
    public Collection<Coordinates> getTargets() {
        return Collections.unmodifiableCollection(target);
    }

    /**
     * For testing only
     * 
     * @return
     */
    public Map getMap() {
        return map;
    }

}
