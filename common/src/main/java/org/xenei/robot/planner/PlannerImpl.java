package org.xenei.robot.planner;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.Stack;
import java.util.concurrent.CopyOnWriteArrayList;

import org.locationtech.jts.geom.Coordinate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.mapping.Map;
import org.xenei.robot.common.planning.Planner;
import org.xenei.robot.common.planning.Solution;
import org.xenei.robot.common.planning.Step;

public class PlannerImpl implements Planner {
    private static final Logger LOG = LoggerFactory.getLogger(PlannerImpl.class);
    private final Stack<Coordinate> target;
    private final Map map;
    private final Collection<Planner.Listener> listeners;
    private Position currentPosition;
    private Solution solution;

    /**
     * Constructs a planner.
     * 
     * @param sensor the sensor to sense environment.
     * @param startPosition the starting position.
     */
    public PlannerImpl(Map map, Location startPosition) {
        this(map, startPosition, null);
    }

    /**
     * Constructs a planner.
     * 
     * @param sensor the sensor to sense environment.
     * @param startPosition the starting position.
     * @param target the coordinates of the target to reach.
     */
    public PlannerImpl(Map map, Location startPosition, Location target) {
        this.map = map;
        this.listeners = new CopyOnWriteArrayList<>();
        this.target = new Stack<>();
        if (target != null) {
            setTarget(target.getCoordinate());
        }
        restart(startPosition);
    }

    @Override
    public void addListener(Planner.Listener listener) {
        this.listeners.add(listener);
    }

    public void notifyListeners() {
        Collection<Planner.Listener> l = this.listeners;
        l.forEach(Planner.Listener::update);
    }

    /**
     * Sets the current position and resets the solution.
     * 
     * @param position the new current position.
     */
    @Override
    public void changeCurrentPosition(Position position) {
        currentPosition = position;
        solution.add(position);
        map.addTarget(new Step(position.getCoordinate(), position.distance(target.peek()), null));
    }

    /**
     * Sets the current position and resets the solution.
     * 
     * @param coords the new current position.
     */
    public void restart(Location coords) {
        double distance = Double.NaN;
        double angle = 0.0;
        if (getTarget() != null) {
            distance = coords.distance(getTarget());
            angle = coords.headingTo(getTarget());
        }
        currentPosition = new Position(coords, angle);
        map.addTarget(new Step(currentPosition.getCoordinate(), distance, null));
        resetSolution();
    }

    private void resetSolution() {
        solution = new Solution();
        if (currentPosition != null) {
            solution.add(currentPosition);
        }
    }

    public Collection<Step> getPlanRecords() {
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

    @Override
    public boolean step() {
        if (currentPosition.equals2D(getTarget(), map.getScale())) {
            LOG.debug("Reached intermediate target");
            target.pop();
            if (target.isEmpty()) {
                LOG.debug("Reached final target");
                solution.add(currentPosition);
                return false;
            }
            // see if we can get to target directly
            if (map.clearView(currentPosition.getCoordinate(), target.peek())) {
                LOG.debug("Can see final target");
                map.addPath(currentPosition.getCoordinate(), target.peek());
                currentPosition.setHeading(target.peek());
                return true;
            }
            // recalculate the distances
            map.recalculate(target.peek());
            // update the planning model make sure we don't revisit where we have been.
            solution.stream().forEach(t -> map.setTemporaryCost(new Step(t, Double.POSITIVE_INFINITY, null)));
        }

        Optional<Step> selected = map.getBestTarget(currentPosition.getCoordinate());
        if (selected.isPresent()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Selected target: " + selected.get());
            }
            // update the planning model for the current position.
            // map.update(Namespace.PlanningModel, currentPosition,
            // Namespace.distance, selected.get().cost());
            // if we are not at the target then make the next position the target.
            if (!selected.get().equals2D(target.peek(), map.getScale())) {
                target.push(selected.get().getCoordinate());
            }
            // if the heading changes mark the point on the current location
            if (currentPosition.getHeading() != currentPosition.headingTo(selected.get())) {
                solution.add(currentPosition);
            }
        }
        return true;
    }

    @Override
    public void setTarget(Coordinate target) {
        LOG.info("Setting target to {} starting from {}", target, currentPosition);
        this.target.clear();
        this.target.push(target);
        if (currentPosition != null) {
            currentPosition.setHeading(target);
        }
        map.recalculate(target);
        resetSolution();
    }

    @Override
    public void replaceTarget(Coordinate target) {
        if (this.target.size() != 1) {
        LOG.info("Replacing target to {} with {}", getTarget(), target);
        this.target.pop();
        } else {
            LOG.info("Adding target to {} to {}", target, getTarget());
        }
        this.target.push(target);
        if (currentPosition != null) {
            currentPosition.setHeading(target);
        }
    }

    @Override
    public Coordinate getTarget() {
        return target.isEmpty() ? null : target.peek();
    }

    @Override
    public Collection<Coordinate> getTargets() {
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
