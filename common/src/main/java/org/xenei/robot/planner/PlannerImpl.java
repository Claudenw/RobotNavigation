package org.xenei.robot.planner;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.locationtech.jts.geom.Coordinate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.Mover;
import org.xenei.robot.common.NavigationSnapshot;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.mapping.Map;
import org.xenei.robot.common.messages.Topic;
import org.xenei.robot.common.planning.Planner;
import org.xenei.robot.common.planning.Solution;
import org.xenei.robot.common.planning.Segment;
import org.xenei.robot.common.planning.TargetStack;
import org.xenei.robot.common.utils.CoordUtils;
import org.xenei.robot.common.utils.RobutContext;
import org.xenei.robot.mapper.rdf.Namespace;

public class PlannerImpl implements Planner  {
    private static final Logger LOG = LoggerFactory.getLogger(PlannerImpl.class);
    private final TargetStack target;
    private final Map map;
    private final Supplier<Position> positionSupplier;
    private final Topic<Mover.MoveTo> moveToTopic;
    private final Topic<Mover.MotorState> motorTopic;
    private Solution solution;
    private NavigationSnapshot snapshot;

    /**
     * Constructs a planner.
     * @param map the map to use
     * @param positionSupplier a provider of the current position.
     */
    public PlannerImpl(Map map, Supplier<Position> positionSupplier) {
        this(map, positionSupplier, null);
    }

    /**
     * Constructs a planner.
     * @param map the map to use
     * @param positionSupplier a provider of the current position.
     * @param target the coordinates of the target to reach.
     */
    public PlannerImpl(Map map, Supplier<Position> positionSupplier, Location target) {
        this.map = map;
        this.moveToTopic = map.getContext().bus.moveTo;
        this.motorTopic = map.getContext().bus.motor;
        this.target = new TargetStack();
        this.positionSupplier = positionSupplier;
        this.solution = new Solution();

        this.snapshot = new NavigationSnapshot(positionSupplier.get(), target == null ? null : target.getCoordinate());
        boolean isIndirect = false;
        double distance = Double.NaN;
        solution.add(snapshot.position);
        if (snapshot.target != null) {
            setTarget(snapshot.target);
        }
        map.addCoord(snapshot.position.getCoordinate(), getTarget(), true);
        LOG.debug("PlannerImpl: {}", snapshot);
    }

    public void accept(Mover.MotorState motorState) {
        if (Mover.MotorState.STOP.equals(motorState)) {
            Position position = positionSupplier.get();
            solution.add(position);
            map.setVisited(getFinalTarget(), position.getCoordinate());
            if (target.isEmpty()) {
                LOG.debug("Reached final target");
                return;
            }
            Optional<Segment> selected = map.getBestStep(position.getCoordinate());
            if (selected.isPresent()) {
                Segment step = selected.get();
                ;
                if (!map.areEquivalent(step.getCoordinate(), getTarget())) {
                    target.push(selected.get().getCoordinate());
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("New target registered: " + selected.get());
                    }
                } else {
                    RobutContext ctxt = map.getContext();
                    Position nextPosition = ctxt.scaleInfo.round(step.nextPosition(position));
                    moveToTopic.send(new Mover.MoveTo(nextPosition));
                }
            }
        }
    }

    @Override
    public NavigationSnapshot getSnapshot() {
        return snapshot;
    }


    @Override
    public void registerPositionChange(NavigationSnapshot snapshot) {
        map.addCoord(snapshot.position.getCoordinate(), getFinalTarget(), true )
                        .thenAccept( step ->
        step.ifPresent(s -> solution.add(s.getCoordinate())));
    }

    @Override
    public Solution getSolution() {
        return solution;
    }

    @Override
    public Optional<Segment> selectTarget() {
        Position pos = positionSupplier.get();
        if (pos.equals2D(getTarget(), map.getContext().scaleInfo.getResolution())) {
            LOG.debug("Reached intermediate target");
            map.setVisited(getFinalTarget(), target.pop());
            if (target.isEmpty()) {
                LOG.debug("Reached final target");
                return Optional.empty();
            }
        }
        Optional<Segment> selected = map.getBestStep(pos.getCoordinate());
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
        motorTopic.send(Mover.MotorState.STOP);
        this.target.clear();
        this.target.push(target);
        map.recalculate(target);
        solution = new Solution();
        solution.add(pos);
        return map.getContext().scaleInfo.round(CoordUtils.calcHeading(pos.getCoordinate(), getTarget()));
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

}
