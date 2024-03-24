package org.xenei.robot.mapper;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.locationtech.jts.geom.Coordinate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.common.FrontsCoordinate;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.NavigationSnapshot;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.mapping.Map;
import org.xenei.robot.common.mapping.Mapper;
import org.xenei.robot.common.mapping.Obstacle;
import org.xenei.robot.common.planning.Step;
import org.xenei.robot.common.utils.CoordUtils;
import org.xenei.robot.common.utils.DoubleUtils;

public class MapperImpl implements Mapper {
    private static final Logger LOG = LoggerFactory.getLogger(MapperImpl.class);
    private final Map map;

    public MapperImpl(Map map) {
        this.map = map;
    }

    public Map getMap() {
        return map;
    }

    @Override
    public List<Step> processSensorData(Coordinate finalTarget, NavigationSnapshot snapshot, Location[] obstacles) {

        LOG.debug("Sense position: {}", snapshot.position);
        if (obstacles.length == 0) {
            LOG.debug("No positions returned from sensor");
            return Collections.emptyList();
        }
        ObstacleMapper mapper = new ObstacleMapper(snapshot.position);
        List.of(obstacles).forEach(mapper::doMap);
        if (mapper.newObstacles.isEmpty()) {
            LOG.debug("No new obstacles detected");
            return Collections.emptyList();
        } 
        if (LOG.isDebugEnabled()) {
            LOG.debug("{} obstacles detected", mapper.newObstacles.size());
        }

        if (finalTarget != null) {
            map.updateIsIndirect(finalTarget, mapper.newObstacles);
        }
        return mapper.coordSet.stream()
                .map(c -> map.addCoord(c, finalTarget == null ? null : c.distance(finalTarget), false,
                        finalTarget == null ? null : !map.isClearPath(c, finalTarget)))
                .flatMap(Optional::stream).collect(Collectors.toList());
    }

    @Override
    public boolean equivalent(FrontsCoordinate position, Coordinate target) {
        return map.areEquivalent(position.getCoordinate(), target);
    }

    @Override
    public boolean isClearPath(Position currentPosition, Coordinate target) {
        return map.isClearPath(currentPosition.getCoordinate(), target);
    }

    class ObstacleMapper {
        final Position currentPosition;
        final double tolerance;
        /** the set of new obstacles */
        final Set<Obstacle> newObstacles;
        /** a set of coordinates that represent new coords */
        final Set<Coordinate> coordSet;

        ObstacleMapper(Position currentPosition) {
            this.currentPosition = currentPosition;
            this.tolerance = map.getContext().getScaledRadius();
            this.newObstacles = new HashSet<>();
            this.coordSet = new HashSet<Coordinate>();
        }

        /**
         * Adds the relative obstacle to the map and potentially adds values to the
         * coordSet.
         * 
         * @param relativeObstacle the relative location to the obstacle.
         */
        void doMap(Location relativeObstacle) {
            if (!relativeObstacle.isInfinite())
            {
                /* create absolute coordinates
                 * relativeObstacle is always a point on an edge of an obstacle. so add 1/2 map resolution to 
                 * the relative distance to place the obstacle within a cell.
                 */
                newObstacles.addAll(map.addObstacle(map.createObstacle(currentPosition, relativeObstacle)));
                if (!DoubleUtils.inRange(relativeObstacle.range(), tolerance)) {
                    Optional<Coordinate> possibleCoord = findCoordinateNear(relativeObstacle);
                    if (possibleCoord.isPresent()) {
                        coordSet.add(possibleCoord.get());
                    }
                }
            }
        }

        /**
         * Finds an open coordinate between the obstacle and the current position when
         * heading toward the obstacle.
         * 
         * @param relativeObstacle
         * @return
         */
        Optional<Coordinate> findCoordinateNear(Location relativeObstacle) {
            double d = relativeObstacle.range() - tolerance;
            if (d < tolerance) {
                return Optional.empty();
            }
            Location relativeCoord = Location.from(CoordUtils.fromAngle(relativeObstacle.theta(), d));
            Location candidate = currentPosition.nextPosition(relativeCoord);
            Coordinate newCoord = map.adopt(candidate.getCoordinate());
            if (map.isObstacle(newCoord)) {
                d -= map.getContext().scaleInfo.getResolution();
                if (d < tolerance) {
                    return Optional.empty();
                }
                relativeCoord = Location.from(CoordUtils.fromAngle(relativeObstacle.theta(), d));
                candidate = currentPosition.nextPosition(relativeCoord);
                newCoord = map.adopt(candidate.getCoordinate());
                if (map.isObstacle(newCoord)) {
                    return Optional.empty();
                }
            }
            return Optional.ofNullable(currentPosition.distance(newCoord) < tolerance ? null : newCoord);
        }
    }
}
