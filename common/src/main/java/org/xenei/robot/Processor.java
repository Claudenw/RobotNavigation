package org.xenei.robot;

import java.io.IOException;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.common.LocationI;
import org.xenei.robot.common.Mover;
import org.xenei.robot.common.NavigationSnapshot;
import org.xenei.robot.common.PositionI;
import org.xenei.robot.common.mapping.Map;
import org.xenei.robot.common.mapping.Mapper;
import org.xenei.robot.common.messages.Topic;
import org.xenei.robot.common.planning.Planner;
import org.xenei.robot.common.utils.DoubleUtils;
import org.xenei.robot.common.utils.RobutContext;
import org.xenei.robot.mapper.MapperImpl;
import org.xenei.robot.mapper.visualization.RemoteVis;
import org.xenei.robot.mover.BaseMover;
import org.xenei.robot.planner.PlannerImpl;

public class Processor {
    private static final Logger LOG = LoggerFactory.getLogger(Processor.class);

    public final Map<?, ?, ?> map;
    private final RobutContext ctxt;
    public final Planner planner;
    private final Mapper mapper;
    private final BaseMover mover;
    private final Supplier<PositionI<?, ?>> positionSupplier;
    private final RemoteVis remoteVis;
    private final RobutContext.Visualizations visualizations;
    private final Topic<Mover.MotorState> motorStateTopic;
    private final Topic<Mover.MoveTo> moveToTopic;

    public Processor(BaseMover mover, Supplier<PositionI<?, ?>> positionSupplier, Map<?, ?, ?> map) {
        this.ctxt = map.getContext();
        this.visualizations = ctxt.visualizations;
        this.motorStateTopic = ctxt.bus.motor;
        this.moveToTopic = ctxt.bus.moveTo;
        this.mover = mover;
        this.positionSupplier = positionSupplier;
        this.map = map;
        this.planner = new PlannerImpl(map, positionSupplier);
        this.mapper = new MapperImpl(map, planner::getFinalTarget);
        try {
            this.remoteVis = new RemoteVis(map, planner::getSolution, positionSupplier, planner::getFinalTarget);
            LOG.debug("Initial position: {}", positionSupplier.get());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Planner getPlanner() {
        return planner;
    }

    public Mapper getMapper() {
        return mapper;
    }

    private boolean checkTarget(NavigationSnapshot snapshot) {
        if (!ctxt.scaleInfo.areEquivalent(snapshot.position, planner.getFinalTarget())) {
            // if we can see the final target go that way.
            if (mapper.isClearPath(snapshot.position, planner.getFinalTarget())) {
                double newHeading = snapshot.position.headingTo(planner.getFinalTarget());
                boolean cont = DoubleUtils.eq(newHeading, snapshot.position.getHeading());
                if (!cont) {
                    // heading is different so reset the heading, scan, and check again.
                    // mover.setHeading(snapshot.currentPosition.headingTo(planner.getRootTarget()));
                    mover.setHeading(newHeading);
                    NavigationSnapshot testingSnapshot = new NavigationSnapshot(mover.position(),
                            planner.getFinalTarget());
                    // mapper.processSensorData(planner.getFinalTarget(), testingSnapshot,
                    // sensor.sense());
                    cont = mapper.isClearPath(testingSnapshot.position, planner.getFinalTarget());
                    if (!cont) {
                        // can't see the position really so reset the heading.
                        mover.setHeading(snapshot.position.getHeading());
                    }
                }
                if (cont) {
                    // we can really see the final position.
                    LOG.info("can see {} from {}", planner.getFinalTarget(), snapshot.position);
                    planner.replaceTarget(planner.getFinalTarget());
                    return true;
                }
                visualizations.redraw();
            }
        }
        // if we can not see the target replan.
        return mapper.isClearPath(snapshot.position, planner.getTarget());
    }

    private NavigationSnapshot newSnapshot() {
        return new NavigationSnapshot(positionSupplier.get(), planner.getTarget());
    }

    private NavigationSnapshot setHeading(double heading) {
        // adjust the heading
        mover.setHeading(heading);
        return newSnapshot();
    }

    // private NavigationSnapshot move(Step step) {
    // Location relativeLoc =
    // mover.position().relativeLocation(step.getCoordinate());
    // mover.move(relativeLoc);
    //
    //
    // map.setVisited(planner.getFinalTarget(),
    // mover.move(relativeLoc).getCoordinate()).join();
    // NavigationSnapshot snapshot = newSnapshot();
    // planner.registerPositionChange(snapshot);
    // return snapshot;
    // }

    public void moveTo(LocationI<?> finalLocation) {
        Map.Loc<?> mapLocation = map.asMapCoordinate(finalLocation);
        NavigationSnapshot snapshot = new NavigationSnapshot(positionSupplier.get(), mapLocation);
        double heading = planner.setTarget(snapshot.target);
        if (LOG.isDebugEnabled()) {
            LOG.debug("calculated heading {} compare to {}", heading, positionSupplier.get().getHeading());
        }
        moveToTopic.send(new Mover.MoveTo(finalLocation));
    }
    // while (planner.getTarget() != null) {
    // Optional<Step> opStep = planner.selectTarget();
    // if (planner.getTarget() == null) {
    // break;
    // }
    // if (opStep.isPresent()) {
    // Step step = opStep.get();
    // Position nextPosition =
    // map.getContext().scaleInfo.round(step.nextPosition(snapshot.position));
    // if (snapshot.didHeadingChange(nextPosition)) {
    // snapshot = setHeading(nextPosition.getHeading());
    // }
    // // can we still see the target
    // if (checkTarget(snapshot)) {
    // snapshot = move(step);
    // }
    // // should we abort
    // abortTest.check(this);
    // } else {
    // LOG.error("NO STEP SELECTED");
    // break;
    // }
    // }
    // ctxt.triggerVisualizations();
    // planner.recordSolution();
    // }

}
