package org.xenei.robot.planner;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.Stack;

import org.apache.commons.math3.util.Precision;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.common.AngleUnits;
import org.xenei.robot.common.Coordinates;
import org.xenei.robot.common.Map;
import org.xenei.robot.common.Planner;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.Solution;
import org.xenei.robot.common.Target;
import org.xenei.robot.common.utils.PointUtils;

import mil.nga.sf.Point;

public class PlannerImpl implements Planner {
    private static final Logger LOG = LoggerFactory.getLogger(PlannerImpl.class);
    private final Stack<Point> target;
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
        map.add(new Target(position, position.distanceTo(target.peek())));
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
        map.add(new Target(currentPosition, distance));
        resetSolution();
    }

    private void resetSolution() {
        solution = new Solution();
        if (currentPosition != null) {
            solution.add(currentPosition);
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
        if (PointUtils.equivalent(currentPosition, target.peek(), Precision.EPSILON )) {
            target.pop();
            if (target.isEmpty()) {
                solution.add(currentPosition);
                return false;
            }
            // see if we can get to target directly
            if (map.clearView(currentPosition, target.peek())) {
                map.path(currentPosition, target.peek());
                currentPosition.setHeading(target.peek());
            }
            // recalculate the distances
            map.recalculate(target.peek());
            // update the planning model make sure we don't revisit where we have been.
            solution.stream().forEach(t -> map.setTemporaryCost(new Target(t, Double.POSITIVE_INFINITY)));
            // add the current location to the solution.
            solution.add(currentPosition);
        }

        Optional<Target> selected = map.getBestTarget(currentPosition);
        if (selected.isPresent()) {
            // update the planning model for the current position.
            // map.update(Namespace.PlanningModel, currentPosition,
            // Namespace.distance, selected.get().cost());
            // if we are not at the target then make the next position the target.
            if (!PointUtils.equivalent(selected.get(), target.peek(), Precision.EPSILON )) {
                target.push(selected.get());
            }
            // if the heading changes mark the point on the current location
            if (currentPosition.getHeading(AngleUnits.RADIANS) != currentPosition
                    .angleTo(selected.get())) {
                solution.add(currentPosition);
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
    public void setTarget(Point target) {
        LOG.info("Setting target to {} starting from {}", target, currentPosition);
        this.target.clear();
        this.target.push(target);
        if (currentPosition != null) {
            currentPosition.setHeading(target);
        }
        map.reset(getTarget());
        resetSolution();
    }

    @Override
    public Point getTarget() {
        return target.isEmpty() ? null : target.peek();
    }

    @Override
    public Collection<Point> getTargets() {
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
