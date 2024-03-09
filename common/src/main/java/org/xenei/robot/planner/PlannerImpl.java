package org.xenei.robot.planner;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.Stack;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

import org.locationtech.jts.geom.Coordinate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.common.Listener;
import org.xenei.robot.common.ListenerContainer;
import org.xenei.robot.common.ListenerContainerImpl;
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
    private final TargetStack target;
    private final Map map;
    private final ListenerContainer listeners;
    private final Supplier<Position> positionSupplier;
    private Solution solution;
    private final DiffImpl diff;

    /**
     * Constructs a planner.
     * 
     * @param sensor the sensor to sense environment.
     * @param startPosition the starting position.
     */
    public PlannerImpl(Map map, Supplier<Position> positionSupplier) {
        this(map, positionSupplier, null);
    }

    /**
     * Constructs a planner.
     * 
     * @param sensor the sensor to sense environment.
     * @param startPosition the starting position.
     * @param target the coordinates of the target to reach.
     */
    public PlannerImpl(Map map, Supplier<Position> positionSupplier, Location target) {
        this.map = map;
        this.listeners = new ListenerContainerImpl();
        this.target = new TargetStack();
        this.positionSupplier = positionSupplier;
        this.solution = new Solution();

        Position currentPosition = positionSupplier.get();
        this.diff = new DiffImpl(currentPosition, target == null ? null : target.getCoordinate());
        boolean isIndirect = false;
        double distance = Double.NaN;
        solution.add(currentPosition);
        if (target != null) {
            setTarget(target.getCoordinate());
            distance = currentPosition.distance(getRootTarget());
            isIndirect = !map.isClearPath(currentPosition.getCoordinate(), getRootTarget());
//            if (!isIndirect) {
//                solution.add(getRootTarget());
//            }
        }
        map.addCoord(currentPosition.getCoordinate(), distance, true, isIndirect);
        LOG.debug( "Constructor: target arg:{}, {}", target, currentPosition);
    }

    @Override
    public Diff getDiff() {
        return diff;
    }

    @Override
    public void addListener(Listener listener) {
        this.listeners.addListener(listener);
    }

    @Override
    public void notifyListeners() {
        this.listeners.notifyListeners();
    }

    @Override
    public void registerPositionChange() {
        Position pos = positionSupplier.get();
        Optional<Step> step = map.addCoord(pos.getCoordinate(), pos.distance(getRootTarget()), true,
                !map.isClearPath(pos.getCoordinate(), getRootTarget()));
        step.ifPresent(s ->solution.add(s.getCoordinate()));
    }

//    private Position newPosition(Coordinate start) {
//        return Position.from(start, CoordUtils.calcHeading(start, getTarget()));
//    }

    /**
     * Restart from the new location using the current map.
     * 
     * @param start the new starting position.
     */
//    @Override
//    public void restart(Location start) {
//        double distance = Double.NaN;
//
//        boolean isIndirect = false;
//        if (getTarget() != null) {
//            currentPosition = newPosition(start.getCoordinate());
//            distance = start.distance(getRootTarget());
//            isIndirect = !map.isClearPath(currentPosition.getCoordinate(), getRootTarget(), buffer);
//        } else {
//            currentPosition = Position.from(start, 0);
//        }
//        diff.reset();
//        map.addCoord(currentPosition.getCoordinate(), distance, true, isIndirect);
//        resetSolution();
//    }

//    private void resetSolution() {
//        solution = new Solution();
//        if (currentPosition != null) {
//            solution.add(currentPosition);
//        }
//    }

    @Override
    public Collection<Step> getPlanRecords() {
        return map.getTargets();
    }

    @Override
    public Solution getSolution() {
        return solution;
    }

//    @Override
//    public Position getCurrentPosition() {
//        return currentPosition;
//    }

    @Override
    public Diff selectTarget() {
        Position pos = positionSupplier.get();
        if (pos.equals2D(getTarget(), map.getContext().scaleInfo.getResolution())) {
            LOG.debug("Reached intermediate target");
            target.pop();
            if (target.isEmpty()) {
                LOG.debug("Reached final target");
            }

        } else {
            Optional<Step> selected = map.getBestStep(pos.getCoordinate());
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
            solution.add(pos);
        }
        return diff;
    }

    @Override
    public void recalculateCosts() {
        // recalculate the distances
        map.recalculate(target.peek());
    }

    @Override
    public double setTarget(Coordinate target) {
        Position pos = positionSupplier.get();
        LOG.info("Setting target to {} starting from {}", target, pos);
        this.target.clear();
        this.target.push(target);
        double heading = CoordUtils.calcHeading(pos.getCoordinate(), getTarget());
        map.recalculate(target);
        solution = new Solution();
        solution.add(pos);;
        return heading;
    }

    @Override
    public double replaceTarget(Coordinate target) {
        Position pos = positionSupplier.get();
        if (this.target.size() != 1) {
            LOG.info("Replacing target to {} with {} while at {}", getTarget(), target, pos);
            if (this.target.get(0).equals2D(target)) {
                this.target.clear();
                this.target.push(target);
            } else {
                this.target.pop();
            }
        } else {
            LOG.info("Adding target to {} to {}", target, getTarget());
        }
        this.target.push(target);
        return CoordUtils.calcHeading(pos.getCoordinate(), getTarget());
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
        
        DiffImpl(Position initial, Coordinate target) {
            lastPosition = initial;
            this.target = target;
        }

        @Override
        public void reset() {
            this.lastPosition = positionSupplier.get();
            this.target = getTarget();
        }
        
        @Override
        public boolean didChange() {
            Position pos = positionSupplier.get();
            return didHeadingChange(pos) || didPositionChange(pos) || didTargetChange();
        }

        private boolean didHeadingChange(Position pos) {
            return !DoubleUtils.eq(lastPosition.getHeading(), pos.getHeading());
        }

        private boolean didPositionChange(Position pos) {
            return !lastPosition.equals2D(pos.getCoordinate());
        }
        
        
        @Override
        public boolean didHeadingChange() {
            return didHeadingChange(positionSupplier.get());
        }

        @Override
        public boolean didPositionChange() {
            return didPositionChange(positionSupplier.get());
        }

        @Override
        public boolean didTargetChange() {
            return getTarget() == null || !target.equals2D(getTarget());
        }
    }
    
    private class TargetStack extends Stack<Coordinate> {
        TargetStack() {
            super();
        }

        @Override
        public Coordinate push(Coordinate item) {
            if (this.contains(item)) {
                while (item != this.pop()) {
                    // all activity in the while statement
                }
            }
            return super.push(item);
        }
    }

}
