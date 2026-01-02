package org.xenei.robot.planner;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.Mover;
import org.xenei.robot.common.ScaleInfo;
import org.xenei.robot.common.mapping.MapLocation;
import org.xenei.robot.common.mapping.MapPath;
import org.xenei.robot.common.mapping.MapTargetData;
import org.xenei.robot.common.mapping.NavigationSnapshot;
import org.xenei.robot.common.mapping.Map;
import org.xenei.robot.common.mapping.MapPosition;
import org.xenei.robot.common.planning.Planner;
import org.xenei.robot.common.planning.Solution;
import org.xenei.robot.common.planning.Segment;
import org.xenei.robot.common.planning.TargetStack;
import org.xenei.robot.common.utils.RobutContext;


public class PlannerImpl implements Planner {
    private static final Logger LOG = LoggerFactory.getLogger(PlannerImpl.class);
    private final TargetStack target;
    private final Map map;
    private final Supplier<? extends Position> positionSupplier;
    private Solution solution;
    private NavigationSnapshot snapshot;
    private MapPath onPath;

    /**
     * Constructs a planner.
     *
     * @param map
     *            the map to use
     * @param positionSupplier
     *            a provider of the current position.
     */
    public PlannerImpl(Map map, Supplier<? extends Position> positionSupplier) {
        this(map, positionSupplier, null);
    }

    /**
     * Constructs a planner.
     *
     * @param map
     *            the map to use
     * @param positionSupplier
     *            a provider of the current position.
     * @param target
     *            the coordinates of the target to reach.
     */
    public PlannerImpl(Map map, Supplier<? extends Position> positionSupplier, Location target) {
        this.map = map;
        this.onPath = null;
        RobutContext.Topic<Location> moveToTopic = map.getContext().moveToTopic;
        map.getContext().motorStateTopic.listen(
                motorState -> {
                    if (Mover.MotorState.STOP == motorState) {
                        selectSegment().ifPresent(segment -> moveToTopic.send(segment.nextLocation()));
                    }
                });
        this.target = new TargetStack();
        this.positionSupplier = positionSupplier;
        this.solution = new Solution();

        this.snapshot = new NavigationSnapshot(map.asMapPosition(positionSupplier.get()), target == null ? null : map.asMapLocation(target));
        solution.add(snapshot.position);
        if (snapshot.target != null) {
            setTarget(snapshot.target);
        }

        LOG.debug("PlannerImpl: {}", snapshot);
    }

    @Override
    public NavigationSnapshot getSnapshot() {
        return snapshot;
    }

    @Override
    public void registerPositionChange(NavigationSnapshot snapshot) {
        snapshot.position.setVisited();
        MapTargetData targetData = snapshot.position.getTargetData(getFinalTarget());
        if (!targetData.indirect()) {
            solution.add(getFinalTarget());
        }
    }

    @Override
    public Solution getSolution() {
        return solution;
    }

    @Override
    public Optional<Segment> selectSegment() {
        MapPosition position = map.asMapPosition(positionSupplier.get());
        if (target.isEmpty() || position.sameCoordinate(getFinalTarget()))
        {
            return Optional.empty();
        }
        Optional<Segment> selected = selectSegment(position);

        if (selected.isPresent()) {
            Segment segment = selected.get();
            if (!segment.nextLocation().sameCoordinate(getTarget())) {
                target.push(segment.nextLocation());
                if (LOG.isDebugEnabled()) {
                    LOG.debug("New target registered: {}", selected.get());
                }
            }
        }
        else {
            double dist = position.distance(target.peek());
            selected = Optional.of(new Segment(target.peek(), dist, dist));
        }
        return selected;
    }

    private Optional<Segment> selectSegment(MapPosition position) {
        position.setVisited();

        if (position.sameCoordinate(getTarget())) {
            LOG.debug("Reached intermediate target");
            target.pop();
            if (target.isEmpty()) {
                LOG.debug("Reached final target");
                return Optional.empty();
            }
        }

        Optional<Segment> result = onPath != null ? onPath.getSegment(position) : Optional.empty();

        result = result.or(() -> {
                    if (position.hasPath(getFinalTarget())) {
                        onPath = position.getPath(getFinalTarget());
                        return onPath.getSegment(position);
                    }
                    return Optional.empty();
                });

        return result.or(() -> explore(position));
    }

    @VisibleForTesting
    Optional<Segment> explore(MapPosition position) {
        double cost = Double.POSITIVE_INFINITY;
        MapLocation newTarget = null;
        double distance = Double.POSITIVE_INFINITY;

        Collection<MapLocation> candidateLocations = position.getCandidateLocations(getFinalTarget())
                .filter(candidate -> !position.sameCoordinate(candidate)).collect(Collectors.toSet());
        for (MapLocation potentialTarget : candidateLocations) {
            MapTargetData potentialTargetData = position.getTargetData(potentialTarget);
            if (!potentialTargetData.indirect()) {
                MapTargetData finalTargetData = potentialTarget.getTargetData(getFinalTarget());
                double potentialCost = potentialTargetData.asSegment(position).cost() + finalTargetData.asSegment(position).cost();
                if (potentialCost < cost) {
                    cost = potentialCost;
                    distance = potentialTargetData.distance();
                    newTarget = potentialTarget;
                }
            }
        }
        if (newTarget == null) {
            return Optional.empty();
        }
        Segment segment = new Segment(newTarget, cost, distance);
        return Optional.of(segment);
    }

    @Override
    public double setTarget(Location target) {
        Position pos = positionSupplier.get();
        LOG.info("Setting target to {} starting from {}", target, pos);
        map.getContext().motorStateTopic.send(Mover.MotorState.PAUSE);
        this.target.clear();
        MapLocation start = map.asMapLocation(pos);
        MapLocation finish = map.asMapLocation(target);
        this.target.push(finish);
        solution = new Solution();
        solution.add(start);

        return pos.headingTo(target);
    }

    @Override
    public void replaceTarget(Location newLocation) {
        MapLocation newTarget = map.asMapLocation(newLocation);
        if (!map.getContext().scaleInfo.compare(ScaleInfo.OP.EQ, getTarget(), newTarget)) {
            if (this.target.size() == 1) {
                this.target.push(newTarget);
            } else {
                LOG.info("Replacing target to {} with {} while at {}", getTarget(), newTarget, Position.PositionUtils.toString(positionSupplier.get()));
                this.target.pop();
                this.target.push(newTarget);
            }
            this.snapshot = new NavigationSnapshot(snapshot.position, newTarget);
        }
    }

    @Override
    public MapLocation getTarget() {
        return target.isEmpty() ? null : target.peek();
    }

    @Override
    public MapLocation getFinalTarget() {
        return target.isEmpty() ? null : target.get(0);
    }

    @Override
    public List<? extends Location> getTargets() {
        return target.stream().toList();
    }

    @Override
    public void recordSolution() {
        Solution solution = this.solution;
        this.solution = new Solution();
        solution.simplify();
        if (solution.stepCount() > 0) {
            map.addPath(solution.stream().map(map::asMapLocation));
        }
    }

    /**
     * For testing only
     *
     * @return the map this planner is using.
     */
    public Map getMap() {
        return map;
    }

}
