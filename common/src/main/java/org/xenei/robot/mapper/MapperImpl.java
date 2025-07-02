package org.xenei.robot.mapper;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.locationtech.jts.geom.Coordinate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.common.FrontsCoordinate;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.ScaleInfo;
import org.xenei.robot.common.mapping.Map;
import org.xenei.robot.common.mapping.Mapper;
import org.xenei.robot.common.utils.CoordUtils;
import org.xenei.robot.common.utils.DoubleUtils;

public class MapperImpl implements Mapper {
    private static final Logger LOG = LoggerFactory.getLogger(MapperImpl.class);
    private final Map map;
    private final double tolerance;
    private final Supplier<Position> positionSupplier;
    private final ScaleInfo scaleInfo;
    private final Supplier<Coordinate> targetSupplier;

    /**
     *
     * @param map The map to work with.
     * @param positionSupplier a position supplier scaled to the map.
     * @param targetSupplier a target supplier scaled to the map.
     */
    public MapperImpl(Map map, Supplier<Position> positionSupplier, Supplier<Coordinate> targetSupplier) {
        this.map = map;
        tolerance = map.getContext().getScaledRadius();
        this.scaleInfo = map.getContext().scaleInfo;
        this.positionSupplier = positionSupplier;
        this.targetSupplier = targetSupplier;
    }


    @Override
    public Consumer<Location> getRelativeObstacleConsumer() {
        return relativeObstacle -> {
            if (!DoubleUtils.inRange(relativeObstacle.range(), map.getContext().chassisInfo.radius)) {
                Position position = positionSupplier.get();
                Location scaledObstacle = map.getContext().scaleInfo.round(relativeObstacle);
                map.getContext().submit(new ObstacleMapper(map, position, scaledObstacle));
            }
        };
    }

//    @Override
//    public List<Step> processSensorData(Coordinate finalTarget, NavigationSnapshot snapshot, Location[] obstacles) {
//
//        LOG.debug("Sense position: {}", snapshot.position);
//        if (obstacles.length == 0) {
//            LOG.debug("No positions returned from sensor");
//            return Collections.emptyList();
//        }
//        ObstacleMapper mapper = new ObstacleMapper(snapshot.position);
//        ScaleInfo scaleInfo = map.getContext().scaleInfo;
//        List.of(obstacles).stream().map( l -> scaleInfo.round(l)).forEach(mapper::doMap);
//        if (mapper.newObstacles.isEmpty()) {
//            LOG.debug("No new obstacles detected");
//            return Collections.emptyList();
//        }
//        if (LOG.isDebugEnabled()) {
//            LOG.debug("{} obstacles detected", mapper.newObstacles.size());
//        }
//
//        if (finalTarget != null) {
//            map.updateIsIndirect(finalTarget, mapper.newObstacles);
//        }
//        return mapper.coordSet.stream()
//                .map(c -> map.addCoord(c, finalTarget == null ? null : c.distance(finalTarget), false,
//                        finalTarget == null ? null : !map.isClearPath(c, finalTarget)))
//                .flatMap(Optional::stream).collect(Collectors.toList());
//    }

    @Override
    public boolean equivalent(FrontsCoordinate position, Coordinate target) {
        return map.areEquivalent(position.getCoordinate(), target);
    }

    @Override
    public boolean isClearPath(Position currentPosition, Coordinate target) {
        return map.isClearPath(currentPosition.getCoordinate(), target);
    }

    /**
     * Adds coordinates to the map that are near a registered obstacle.
     */
    class ObstacleMapper implements Runnable {
        final Map map;
        final Position currentPosition;
        final double tolerance;
        final Location relativeObstacle;

        ObstacleMapper(Map map, Position currentPosition, Location relativeObstacle) {
            this.map = map;
            this.currentPosition = currentPosition;
            this.tolerance = map.getContext().getScaledRadius();
            this.relativeObstacle = relativeObstacle;
        }

        public void run() {
            //Optional<Coordinate> findCoordinateNear(Location relativeObstacle) {
            double distance = relativeObstacle.range() - tolerance;
            if (distance < tolerance) {
                return;
            }
            Location relativeCoord = Location.from(CoordUtils.fromAngle(relativeObstacle.theta(), distance));
            Location candidate = currentPosition.nextPosition(relativeCoord);
            Coordinate newCoord = map.adopt(candidate.getCoordinate());
            // if it is not an obstical add it.
            if (!map.isObstacle(newCoord)) {
                map.addObstacle(map.createObstacle(currentPosition, relativeObstacle));
            }
            map.addCoord(newCoord, targetSupplier.get(), false);
        }
    }
}
