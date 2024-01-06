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
import org.xenei.robot.common.utils.CoordUtils;
import org.xenei.robot.common.utils.DoubleUtils;

public class PlannerImpl implements Planner {
    private static final Logger LOG = LoggerFactory.getLogger(PlannerImpl.class);
    private final Stack<Coordinate> target;
    private final Map map;
    private final Collection<Planner.Listener> listeners;
    private Position currentPosition;
    private Solution solution;
    private final DiffImpl diff;
    private final double buffer;

    /**
     * Constructs a planner.
     * 
     * @param sensor the sensor to sense environment.
     * @param startPosition the starting position.
     */
    public PlannerImpl(Map map, Location startPosition, double buffer) {
        this(map, startPosition, buffer, null);
    }

    /**
     * Constructs a planner.
     * 
     * @param sensor the sensor to sense environment.
     * @param startPosition the starting position.
     * @param target the coordinates of the target to reach.
     */
    public PlannerImpl(Map map, Location startPosition, double buffer, Location target) {
        this.map = map;
        this.buffer = buffer;
        this.listeners = new CopyOnWriteArrayList<>();
        this.target = new Stack<>();
        this.diff = new DiffImpl();
        if (target != null) {
            setTarget(target.getCoordinate());
        }
        restart(startPosition);
    }

    @Override
    public Diff getDiff() {
        return diff;
    }

    @Override
    public void addListener(Planner.Listener listener) {
        this.listeners.add(listener);
    }

    @Override
    public void notifyListeners() {
        Collection<Planner.Listener> l = this.listeners;
        try {
            l.forEach(Planner.Listener::update);
        } finally {
        }
    }

    /**
     * Sets the current position and adds to the solution.
     * 
     * @param pos the new current position.
     */
    @Override
    public void changeCurrentPosition(Position pos) {
        currentPosition = Position.from(pos, pos.getHeading());
        Step step = map.addCoord(currentPosition.getCoordinate(), currentPosition.distance(getRootTarget()), true,
                !map.clearView(currentPosition.getCoordinate(), getRootTarget(), buffer));
        solution.add(step.getCoordinate());
    }

    private Position newPosition(Coordinate start) {
        return Position.from(start, CoordUtils.calcHeading(start, getTarget()));
    }

    /**
     * Restart from the new location using the current map.
     * 
     * @param start the new starting position.
     */
    @Override
    public void restart(Location start) {
        double distance = Double.NaN;

        boolean isIndirect = false;
        if (getTarget() != null) {
            currentPosition = newPosition(start.getCoordinate());
            distance = start.distance(getRootTarget());
            isIndirect = !map.clearView(currentPosition.getCoordinate(), getRootTarget(), buffer);
        } else {
            currentPosition = Position.from(start, 0);
        }
        diff.reset();
        map.addCoord(currentPosition.getCoordinate(), distance, true, isIndirect);
        resetSolution();
    }

    private void resetSolution() {
        solution = new Solution();
        if (currentPosition != null) {
            solution.add(currentPosition);
        }
    }

    @Override
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
        if (currentPosition.equals2D(getTarget(), map.getContext().scaleInfo.getResolution())) {
            LOG.debug("Reached intermediate target");
            target.pop();
            if (target.isEmpty()) {
                LOG.debug("Reached final target");
            }

        } else {
            Optional<Step> selected = map.getBestStep(currentPosition.getCoordinate(), buffer);
            if (selected.isPresent()) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Selected target: " + selected.get());
                }
                if (!map.areEquivalent(selected.get().getCoordinate(), getTarget())) {
                    target.push(selected.get().getCoordinate());
                }
            }
        }
        if (diff.didChange()) {
            solution.add(currentPosition);
        }
        return diff;
    }

    @Override
    public void recalculateCosts() {
        // recalculate the distances
        map.recalculate(target.peek(), buffer);
    }

    @Override
    public void setTarget(Coordinate target) {
        LOG.info("Setting target to {} starting from {}", target, currentPosition);
        this.target.clear();
        this.target.push(target);
        if (currentPosition != null) {
            currentPosition = newPosition(currentPosition.getCoordinate());
        }
        map.recalculate(target, buffer);
        resetSolution();
    }

    @Override
    public void replaceTarget(Coordinate target) {
        if (this.target.size() != 1) {
            LOG.info("Replacing target to {} with {} while at {}", getTarget(), target, this.currentPosition);
            if (this.target.get(0).equals2D(target, buffer)) {
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
            currentPosition = newPosition(currentPosition.getCoordinate());
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

        @Override
        public void reset() {
            this.lastPosition = currentPosition;
            this.target = getTarget();
        }

        @Override
        public boolean didHeadingChange() {
            return !DoubleUtils.eq(lastPosition.getHeading(), currentPosition.getHeading());
        }

        @Override
        public boolean didPositionChange() {
            return !lastPosition.equals2D(currentPosition, buffer);
        }

        @Override
        public boolean didTargetChange() {
            return getTarget() == null || !target.equals2D(getTarget(), buffer);
        }
    }

}
