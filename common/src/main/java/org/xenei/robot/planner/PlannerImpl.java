package org.xenei.robot.planner;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.Stack;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.locationtech.jts.geom.Coordinate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.common.Listener;
import org.xenei.robot.common.ListenerContainer;
import org.xenei.robot.common.ListenerContainerImpl;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.NavigationSnapshot;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.mapping.Map;
import org.xenei.robot.common.planning.Planner;
import org.xenei.robot.common.planning.Solution;
import org.xenei.robot.common.planning.Step;
import org.xenei.robot.common.utils.CoordUtils;
import org.xenei.robot.common.utils.DoubleUtils;
import org.xenei.robot.mapper.rdf.Namespace;

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

    @Override
    public Solution getSolution() {
        return solution;
    }

    @Override
    public Diff selectTarget() {
        Position pos = positionSupplier.get();
        if (pos.equals2D(getTarget(), map.getContext().scaleInfo.getResolution())) {
            LOG.debug("Reached intermediate target");
            map.setVisited(target.pop());
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

    @Override
    public void recordSolution() {
        Solution solution = this.solution;
        this.solution = new Solution();
        solution.simplify((a, b) -> map.isClearPath(a, b));
        if (solution.stepCount() > 0) {
            Coordinate[] coords = solution.stream().collect(Collectors.toList()).toArray(new Coordinate[0]);
            map.addPath(Namespace.KnownModel,coords);
        }
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
        NavigationSnapshot snapshot;
        
        DiffImpl(Position initial, Coordinate target) {
            snapshot = new NavigationSnapshot(initial, target);
        }

        @Override
        public void reset() {
            snapshot = new NavigationSnapshot(positionSupplier.get(),getTarget());
        }
        
        @Override
        public boolean didChange() {
            Position pos = positionSupplier.get();
            return didHeadingChange(pos) || didPositionChange(pos) || didTargetChange();
        }

        private boolean didHeadingChange(Position pos) {
            return !DoubleUtils.eq(snapshot.heading(), pos.getHeading());
        }

        private boolean didPositionChange(Position pos) {
            return !snapshot.currentPosition.equals2D(pos.getCoordinate());
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
            Coordinate newTarget = getTarget();
            if (snapshot.target == null) {
                return (newTarget != null);
            }
            return !snapshot.target.equals(newTarget);
        }
    }
    
    private class TargetStack extends Stack<Coordinate> {
        TargetStack() {
            super();
        }

        @Override
        public Coordinate push(Coordinate item) {
            if (this.size() == 2) {
                this.pop();
            }
            if (this.contains(item)) {
                while (item != this.pop()) {
                    // all activity in the while statement
                }
            }
            return super.push(item);
        }
    }

}
