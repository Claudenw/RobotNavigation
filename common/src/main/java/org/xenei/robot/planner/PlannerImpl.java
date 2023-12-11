package org.xenei.robot.planner;

import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.Optional;
import java.util.Stack;
import java.util.concurrent.CopyOnWriteArrayList;

import org.locationtech.jts.geom.Coordinate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.common.DistanceSensor;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.mapping.Map;
import org.xenei.robot.common.planning.Planner;
import org.xenei.robot.common.planning.Solution;
import org.xenei.robot.common.planning.Step;
import org.xenei.robot.common.planning.Planner.Diff;
import org.xenei.robot.common.utils.DoubleUtils;

public class PlannerImpl implements Planner {
    private static final Logger LOG = LoggerFactory.getLogger(PlannerImpl.class);
    private final Stack<Coordinate> target;
    private final Map map;
    private final Collection<Planner.Listener> listeners;
    private Position currentPosition;
    private Solution solution;
    private final DiffImpl diff;

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
        this.diff = new DiffImpl();
        if (target != null) {
            setTarget(target.getCoordinate());
        }
        restart(startPosition);
    }

    public Diff getDiff() {
        return diff;
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
        diff.reset();
        currentPosition = position;
        solution.add(position);
        map.addTarget(new Step(position.getCoordinate(), position.distance(target.peek())));
    }
    
    /**
     * Restart from the new location using the current map.
     * 
     * @param start the new starting position.
     */
    public void restart(Location start) {
        double distance = Double.NaN;
        double angle = 0.0; 
        if (getTarget() != null) {
            distance = start.distance(getTarget());
            angle = start.headingTo(getTarget());
        }
        currentPosition = new Position(start, angle);
        diff.reset();
        map.addTarget(new Step(currentPosition.getCoordinate(), distance));
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
    public Diff selectTarget() {
        if (currentPosition.equals2D(getTarget(), map.getScale().getTolerance())) {
            LOG.debug("Reached intermediate target");
            target.pop();
            if (target.isEmpty()) {
                LOG.debug("Reached final target");
            }
           
        } else {
            Optional<Step> selected = map.getBestTarget(currentPosition.getCoordinate());
            if (selected.isPresent()) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Selected target: " + selected.get());
                }
                if (!selected.get().equals2D(target.peek(), map.getScale().getTolerance())) {
                    target.push(selected.get().getCoordinate());
                }
            }
        }
        if (diff.didChange()) {
            solution.add(currentPosition);
        }
        return diff;
    }
    
    public void recalculateCosts() {
        // recalculate the distances
        map.recalculate(target.peek());
        // update the planning model make sure we don't revisit where we have been.
        solution.stream().forEach(t -> map.setTemporaryCost(new Step(t, Double.POSITIVE_INFINITY)));
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
            if (this.target.get(0).equals2D(target, map.getScale().getTolerance()))
            {
                this.target.clear();
                this.target.push(target);
            } else {
                this.target.pop();
            }
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
    public Coordinate getRootTarget() {
        return target.isEmpty() ? null : target.get(0);
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
    
    private class DiffImpl implements Planner.Diff {
        private Position lastPosition;
        private Coordinate target;
        
        public void reset() {
            this.lastPosition = currentPosition;
            this.target = getTarget();
        }
        
        public boolean didHeadingChange() {
            return ! DoubleUtils.eq(lastPosition.getHeading(), currentPosition.getHeading());          
        }
        
        public boolean didPositionChange() {
            return ! lastPosition.equals2D(currentPosition, map.getScale().getTolerance());
        }
        
        public boolean didTargetChange() {
            return getTarget() == null || ! target.equals2D(getTarget(), map.getScale().getTolerance());
        }
    }

}
