package org.xenei.robot;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.Mover;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.ScaleInfo;
import org.xenei.robot.common.mapping.MapCoordinate;
import org.xenei.robot.common.mapping.MapLocation;
import org.xenei.robot.common.mapping.NavigationSnapshot;
import org.xenei.robot.common.mapping.Map;
import org.xenei.robot.common.mapping.MapPosition;
import org.xenei.robot.common.mapping.Mapper;
import org.xenei.robot.common.planning.Planner;
import org.xenei.robot.common.planning.Segment;
import org.xenei.robot.common.planning.Solution;
import org.xenei.robot.common.utils.DoubleUtils;
import org.xenei.robot.common.utils.RobutContext;
import org.xenei.robot.mapper.MapperImpl;
import org.xenei.robot.mover.BaseMover;
import org.xenei.robot.planner.PlannerImpl;

public class Processor {
    private static final Logger LOG = LoggerFactory.getLogger(Processor.class);

    public final Map map;
    private final RobutContext ctxt;
    protected final MappedPlanner planner;
    private final Mapper mapper;
    private final MappedMover mover;
    private final Supplier<MapPosition> positionSupplier;
    private final RobutContext.Topic<Location> moveToTopic;

    public Processor(BaseMover mover, Supplier<Position> positionSupplier, Map map) {
        this.ctxt = map.getContext();
        this.moveToTopic = ctxt.moveToTopic;
        this.mover = new MappedMover(mover);
        this.positionSupplier = () -> map.asMapPosition(positionSupplier.get());
        this.map = map;
        this.planner = new MappedPlanner(new PlannerImpl(map, this.positionSupplier));
        this.mapper = new MapperImpl(map);
        LOG.debug("Registered reading consumer");
    }

    public Planner getPlanner() {
        return planner;
    }

    public Supplier<MapPosition> getPositionSupplier() {
        return positionSupplier;
    }

    private boolean checkTarget(NavigationSnapshot snapshot) {
        if (!ctxt.scaleInfo.compare(ScaleInfo.OP.EQ, snapshot.position, planner.getFinalTarget())) {
            // if we can see the final target go that way.
            if (mapper.isClearPath(snapshot.position, planner.getFinalTarget())) {
                double newHeading = snapshot.position.headingTo(planner.getFinalTarget());
                boolean cont = DoubleUtils.eq(newHeading, snapshot.position.heading());
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
                        mover.setHeading(snapshot.position.heading());
                    }
                }
                if (cont) {
                    // we can really see the final position.
                    LOG.info("can see {} from {}", planner.getFinalTarget(), snapshot.position);
                    planner.replaceTarget(planner.getFinalTarget());
                    return true;
                }
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

    public void moveTo(Location finalLocation) {
        MapLocation mapLocation = map.asMapLocation(finalLocation);
        NavigationSnapshot snapshot = new NavigationSnapshot(map.asMapPosition(positionSupplier.get()), mapLocation);
        double heading = planner.setTarget(snapshot.target);
        if (LOG.isDebugEnabled()) {
            LOG.debug("calculated heading {} compare to {}", heading, positionSupplier.get().heading());
        }
        moveToTopic.send(mapLocation);
    }

    /**
     * Wraps a Mover so that the positions returned are Map positions.
     */
    class MappedMover implements Mover {
        private final Mover delegate;

        public MappedMover(Mover delegate) {
            this.delegate = delegate;
        }

        @Override
        public void move(Location location) {
            delegate.move(location);
        }

        @Override
        public MapPosition position() {
            return map.asMapPosition(delegate.position());
        }

        @Override
        public void setHeading(double heading) {
            delegate.setHeading(heading);
        }

        @Override
        public void register(LogicModule logicModule) {
            delegate.register(logicModule);
        }

        @Override
        public void close() throws Exception {
            delegate.close();
        }
    }

    public class MappedPlanner implements Planner {
        private final Planner delegate;

        public MappedPlanner(Planner delegate) {
            this.delegate = delegate;
        }

        @Override
        public MapLocation getTarget() {
            Location loc = delegate.getTarget();
            return loc == null ? null : map.asMapLocation(delegate.getTarget());
        }

        @Override
        public MapLocation getFinalTarget() {
            return map.asMapLocation(delegate.getFinalTarget());
        }

        @Override
        public Collection<? extends MapCoordinate> getTargets() {
            return delegate.getTargets().stream().map(map::asMapCoordinate).toList();
        }

        @Override
        public double setTarget(Location target) {
            return delegate.setTarget(map.asMapLocation(target));
        }

        @Override
        public void replaceTarget(Location target) {
            delegate.replaceTarget(map.asMapLocation(target));
        }

        @Override
        public Solution getSolution() {
            return delegate.getSolution();
        }

        @Override
        public void recordSolution() {
            delegate.recordSolution();
        }

        @Override
        public Optional<Segment> selectSegment() {
            return delegate.selectSegment();
        }

        @Override
        public void registerPositionChange(NavigationSnapshot snapshot) {
            delegate.registerPositionChange(snapshot);
        }

        @Override
        public NavigationSnapshot getSnapshot() {
            return delegate.getSnapshot();
        }
    }
}
