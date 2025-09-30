package org.xenei.robot.planner;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.common.FrontsCoordinate;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.Mover;
import org.xenei.robot.common.NavigationSnapshot;
import org.xenei.robot.common.PositionI;
import org.xenei.robot.common.ScaleInfo;
import org.xenei.robot.common.mapping.Map;
import org.xenei.robot.common.messages.Topic;
import org.xenei.robot.common.planning.Planner;
import org.xenei.robot.common.planning.Solution;
import org.xenei.robot.common.planning.Segment;
import org.xenei.robot.common.planning.TargetStack;
import org.xenei.robot.common.utils.CoordUtils;
import org.xenei.robot.mapper.SegmentImpl;
import org.xenei.robot.mapper.rdf.Namespace;

public class PlannerImpl implements Planner {
    private static final Logger LOG = LoggerFactory.getLogger(PlannerImpl.class);
    private final TargetStack target;
    private final Map<?, ?, ?> map;
    private final Supplier<PositionI<?, ?>> positionSupplier;
    private final Topic<Mover.MoveTo> moveToTopic;
    private final Topic<Mover.MotorState> motorTopic;
    private final ScaleInfo scaleInfo;
    private Solution solution;
    private NavigationSnapshot snapshot;

    /**
     * Constructs a planner.
     *
     * @param map
     *            the map to use
     * @param positionSupplier
     *            a provider of the current position.
     */
    public PlannerImpl(Map<?, ?, ?> map, Supplier<PositionI<?, ?>> positionSupplier) {
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
    public PlannerImpl(Map<?, ?, ?> map, Supplier<PositionI<?, ?>> positionSupplier, Location target) {
        this.map = map;
        this.moveToTopic = map.getContext().bus.moveTo;
        this.motorTopic = map.getContext().bus.motor;
        this.scaleInfo = map.getContext().scaleInfo;
        motorTopic.register(motorState -> {
            if (Mover.MotorState.STOP.equals(motorState)) {
                selectSegment().ifPresent(nextPosition -> moveToTopic.send(new Mover.MoveTo(nextPosition)));
            }
        });
        this.target = new TargetStack();
        this.positionSupplier = positionSupplier;
        this.solution = new Solution();

        this.snapshot = new NavigationSnapshot(positionSupplier.get(), map.asMapCoordinate(target));
        solution.add(map.asMapCoordinate(snapshot.position));
        if (snapshot.target != null) {
            setTarget(snapshot.target);
        }

        map.asMapPosition(snapshot.position).addTarget(snapshot.target);
        LOG.debug("PlannerImpl: {}", snapshot);
    }

    @Override
    public NavigationSnapshot getSnapshot() {
        return snapshot;
    }

    @Override
    public void registerPositionChange(NavigationSnapshot snapshot) {
        map.addCoord(snapshot.position, getFinalTarget(), true).thenAccept(segment -> segment.ifPresent(solution::add));
    }

    @Override
    public Solution getSolution() {
        return solution;
    }

    @Override
    public Optional<Segment> selectSegment() {
        PositionI<?, ?> pos = positionSupplier.get();
        map.setVisited(pos);
        if (scaleInfo.areEquivalent(pos, getTarget())) {
            LOG.debug("Reached intermediate target");
            map.setVisited(target.pop());
            if (target.isEmpty()) {
                LOG.debug("Reached final target");
                return Optional.empty();
            }
        }
        Optional<Segment> selected = map.getBestSegment(pos);
        if (selected.isPresent()) {
            Segment segment = selected.get();
            if (!scaleInfo.areEquivalent(segment, getTarget())) {
                target.push(segment);
                if (LOG.isDebugEnabled()) {
                    LOG.debug("New target registered: " + selected.get());
                }
            }
        } else {
            double dist = pos.distance(target.peek());
            selected = Optional.of(SegmentImpl.builder().setCoordinate(target.peek()).setCost(dist).setDistance(dist)
                    .build(map.getContext()));
        }
        return selected;
    }

    @Override
    public void recalculateCosts() {
        // recalculate the distances
        map.recalculate(target.peek());
    }

    @Override
    public double setTarget(FrontsCoordinate target) {
        PositionI<?, ?> pos = positionSupplier.get();
        LOG.info("Setting target to {} starting from {}", target, pos);
        motorTopic.send(Mover.MotorState.PAUSE);
        this.target.clear();
        this.target.push(map.recalculate(target));

        solution = new Solution();
        solution.add(map.asMapCoordinate(pos));
        return map.getContext().scaleInfo.round(CoordUtils.calcHeading(pos, getTarget()));
    }

    @Override
    public void replaceTarget(FrontsCoordinate target) {
        PositionI<?, ?> pos = positionSupplier.get();
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
    }

    @Override
    public FrontsCoordinate getTarget() {
        return target.isEmpty() ? null : target.peek();
    }

    @Override
    public FrontsCoordinate getFinalTarget() {
        return target.isEmpty() ? null : target.get(0);
    }

    @Override
    public Collection<FrontsCoordinate> getTargets() {
        return Collections.unmodifiableCollection(target);
    }

    @Override
    public void recordSolution() {
        Solution solution = this.solution;
        this.solution = new Solution();
        solution.simplify(map::isClearPath);
        if (solution.stepCount() > 0) {
            FrontsCoordinate[] coords = solution.stream().toArray(FrontsCoordinate[]::new);
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

}
