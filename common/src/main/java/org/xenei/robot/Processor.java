//package org.xenei.robot;
//
//import java.util.Optional;
//import java.util.stream.Stream;
//
//import org.locationtech.jts.geom.Coordinate;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.xenei.robot.common.Location;
//import org.xenei.robot.common.DistanceSensor;
//import org.xenei.robot.common.Mover;
//import org.xenei.robot.common.Position;
//import org.xenei.robot.common.ScaleInfo;
//import org.xenei.robot.common.mapping.Map;
//import org.xenei.robot.common.mapping.Mapper;
//import org.xenei.robot.common.planning.Planner;
//import org.xenei.robot.common.utils.CoordUtils;
//import org.xenei.robot.mapper.MapperImpl;
//import org.xenei.robot.mapper.MapImpl;
//import org.xenei.robot.planner.PlannerImpl;
//
//public class Processor {
//    private final Mover mover;
//    private final DistanceSensor sensor;
//    private final Planner planner;
//    private final Mapper mapper;
//    private final Map map;
//    private static final Logger LOG = LoggerFactory.getLogger(Processor.class);
//
//    private final int MAX_STEPS = 1000;
//
//    public Processor(DistanceSensor sensor, Mover mover) {
//        this.mover = mover;
//        this.sensor = sensor;
//        ScaleInfo scaleInfo = new ScaleInfo.Builder().setScale(1).build();
//        this.map = new MapImpl(scaleInfo);
//        this.mapper = new MapperImpl(map);
//        this.planner = new PlannerImpl(map, mover.position());
//    }
//
//    public void setTarget(Location target) {
//        planner.setTarget(target);
//    }
//
//    /**
//     * Calculates the next step and moves to it.
//     * 
//     * @return The location after the movement. or empty if there is no move to
//     * take.
//     */
//    private Optional<Position> step() {
//        if (planner.step()) {
//            Coordinate nextLoc = planner.getTarget();
//            if (nextLoc == null) {
//                return Optional.empty();
//            }
//            Location c = new Location(nextLoc);
//            Optional<Position> result = Optional.of(mover.move(c.minus(mover.position())));
//            LOG.info("next step {}", result);
//            if (result.isPresent()) {
//                planner.changeCurrentPosition(result.get());
//            }
//            return result;
//        }
//        return Optional.empty();
//    }
//
//    public boolean moveTo(Location coord) {
//        setTarget(coord);
//        Optional<Position> p = Optional.of(mover.position());
//        int steps = 0;
//        while (p.isPresent()) {
//            if (steps > MAX_STEPS) {
//                LOG.error("Unable to find path in {} steps", MAX_STEPS);
//                return false;
//            }
//            p = step();
//        }
//        return true;
//    }
//    
//    public void recordSolution() {
//        map.recordSolution(planner.getSolution());
//    }
//
//    public Stream<Coordinate> getSolution() {
//        return planner.getSolution().stream();
//    }
//
//    public Planner getPlanner() {
//        return planner;
//    }
//
//}
