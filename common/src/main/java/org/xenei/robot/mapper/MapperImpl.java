package org.xenei.robot.mapper;

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
import org.xenei.robot.common.Position;
import org.xenei.robot.common.ScaleInfo;
import org.xenei.robot.common.mapping.Map;
import org.xenei.robot.common.mapping.Mapper;
import org.xenei.robot.common.mapping.Obstacle;
import org.xenei.robot.common.planning.Step;
import org.xenei.robot.common.utils.AngleUtils;
import org.xenei.robot.common.utils.CoordUtils;
import org.xenei.robot.common.utils.DoubleUtils;
import org.xenei.robot.common.utils.GeometryUtils;

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
    public List<Step> processSensorData(Position currentPosition, double buffer, Coordinate target,
            Location[] obstacles) {

        LOG.debug("Sense position: {}", currentPosition);

        ObstacleMapper mapper = new ObstacleMapper(currentPosition, buffer);
        List.of(obstacles).forEach(mapper::doMap);
        map.updateIsIndirect(target, buffer, mapper.newObstacles);
        
        System.out.println(MapReports.dumpObstacleDistance((MapImpl)map));
        System.out.println(MapReports.dumpModel((MapImpl)map));
        
        return mapper.coordSet.stream()
                .map(c -> map.addCoord(c, c.distance(target), false, !map.isClearPath(c, target, buffer)))
                .collect(Collectors.toList());
    }

    @Override
    public boolean equivalent(FrontsCoordinate position, Coordinate target) {
        return map.areEquivalent(position.getCoordinate(), target);
    }

    @Override
    public boolean isClearPath(Position currentPosition, Coordinate target, double buffer) {
        return map.isClearPath(currentPosition.getCoordinate(), target, buffer);
    }

    class ObstacleMapper {
        final Position currentPosition;
        final double buffer;
        final Set<Obstacle> newObstacles;
        final Set<Coordinate> coordSet;

        ObstacleMapper(Position currentPosition, double buffer) {
            this.currentPosition = currentPosition;
            this.buffer = buffer + map.getContext().scaleInfo.getResolution();
            this.newObstacles = new HashSet<>();
            this.coordSet = new HashSet<Coordinate>();
        }

        private Position adjustPosition(Location relative, double adjustment) {
            Location adjustedRelative = Location
                    .from(CoordUtils.fromAngle(relative.theta(), relative.range() + adjustment));
            return currentPosition.nextPosition(adjustedRelative);
        }

        void doMap(Location relativeObstacle) {
            /* create absolute coordinates
             * relativeObstacle is always a point on an edge of an obstacle. so add 1/2 map resolution to 
             * the relative distance to place the obstacle within a cell.
             */

            newObstacles.addAll(map.addObstacle(map.createObstacle(currentPosition,  relativeObstacle)));

            // filter out any range < 1.0
            if (!DoubleUtils.inRange(relativeObstacle.range(), buffer)) {
                Optional<Coordinate> possibleCoord = findCoordinateNear(relativeObstacle);
                if (possibleCoord.isPresent()) {
                    coordSet.add(possibleCoord.get());
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
             double d = relativeObstacle.range() - buffer;
            if (d < buffer) {
                return Optional.empty();
            }
            Location relativeCoord = Location.from(CoordUtils.fromAngle(relativeObstacle.theta(), d));
            Location candidate = currentPosition.nextPosition(relativeCoord);
            Coordinate newCoord = map.adopt(candidate.getCoordinate());
            if (map.isObstacle(newCoord)) {
                d -= map.getContext().scaleInfo.getResolution();
                if (d < buffer) {
                    return Optional.empty();
                }
                relativeCoord = Location.from(CoordUtils.fromAngle(relativeObstacle.theta(), d));
                candidate = currentPosition.nextPosition(relativeCoord);
                newCoord = map.adopt(candidate.getCoordinate());
                if (map.isObstacle(newCoord)) {
                    return Optional.empty();
                }
            }
            return Optional.ofNullable(currentPosition.distance(newCoord) < buffer ? null : newCoord);
        }
    }
}
