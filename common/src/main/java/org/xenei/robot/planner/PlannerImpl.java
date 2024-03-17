package org.xenei.robot.planner;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.Stack;
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
import org.xenei.robot.mapper.rdf.Namespace;

public class PlannerImpl implements Planner {
    private static final Logger LOG = LoggerFactory.getLogger(PlannerImpl.class);
    private final TargetStack target;
    private final Map map;
    private final ListenerContainer listeners;
    private final Supplier<Position> positionSupplier;
    private Solution solution;
    private NavigationSnapshot snapshot;

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

        this.snapshot = new NavigationSnapshot(positionSupplier.get(), target == null ? null : target.getCoordinate());
        boolean isIndirect = false;
        double distance = Double.NaN;
        solution.add(snapshot.position);
        if (snapshot.target != null) {
            setTarget(snapshot.target);
            distance = snapshot.position.distance(getFinalTarget());
            isIndirect = !map.isClearPath(snapshot.position.getCoordinate(), getFinalTarget());
        }
        map.addCoord(snapshot.position.getCoordinate(), distance, true, isIndirect);
        LOG.debug("PlannerImpl: {}", snapshot);
    }

    @Override
    public NavigationSnapshot getSnapshot() {
        return snapshot;
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
    public void registerPositionChange(NavigationSnapshot snapshot) {
        Optional<Step> step = map.addCoord(snapshot.position.getCoordinate(),
                snapshot.position.distance(getFinalTarget()), true,
                !map.isClearPath(snapshot.position.getCoordinate(), getFinalTarget()));
        step.ifPresent(s -> solution.add(s.getCoordinate()));
    }

    @Override
    public Solution getSolution() {
        return solution;
    }

    @Override
    public Optional<Step> selectTarget() {
        Position pos = positionSupplier.get();
        if (pos.equals2D(getTarget(), map.getContext().scaleInfo.getResolution())) {
            LOG.debug("Reached intermediate target");
            // TODO use thread for set visited
            map.setVisited(getFinalTarget(), target.pop());
            if (target.isEmpty()) {
                LOG.debug("Reached final target");
                return Optional.empty();
            }
        }
        Optional<Step> selected = map.getBestStep(pos.getCoordinate());
        if (selected.isPresent()) {
            if (!map.areEquivalent(selected.get().getCoordinate(), getTarget())) {
                target.push(selected.get().getCoordinate());
                if (LOG.isDebugEnabled()) {
                    LOG.debug("New target registered: " + selected.get());
                }
            }
        }
        return selected;
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
        solution.add(pos);
        ;
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
        this.snapshot = new NavigationSnapshot(snapshot.position, target);
        return CoordUtils.calcHeading(pos.getCoordinate(), getTarget());
    }

    @Override
    public Coordinate getTarget() {
        return target.isEmpty() ? null : target.peek();
    }

    @Override
    public Coordinate getFinalTarget() {
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
            map.addPath(Namespace.KnownModel, coords);
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
                while (!item.equals2D(this.pop())) {
                    // all activity in the while statement
                }
            }
            return super.push(item);
        }
    }

}
