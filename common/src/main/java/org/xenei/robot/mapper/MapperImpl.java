package org.xenei.robot.mapper;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.util.iterator.UniqueFilter;
import org.apache.jena.vocabulary.RDF;
import org.locationtech.jts.geom.Coordinate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.common.AbstractFrontsCoordinate;
import org.xenei.robot.common.FrontsCoordinate;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.mapping.Map;
import org.xenei.robot.common.mapping.Mapper;
import org.xenei.robot.common.planning.Step;
import org.xenei.robot.common.utils.CoordUtils;
import org.xenei.robot.common.utils.DoubleUtils;
import org.xenei.robot.mapper.rdf.Namespace;


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
        
        ObstacleMapper mapper = new ObstacleMapper(currentPosition, buffer, target);
        List.of(obstacles).forEach(mapper::doMap);
        map.updateIsIndirect(target, buffer, mapper.newObstacles);
        
        System.out.println( "Obstacles");
        mapper.newObstacles.forEach(c ->System.out.format("%s,%s\n",c.x, c.y));
        System.out.println( "Coords");
        mapper.coordSet.stream().forEach( c ->System.out.format("%s,%s,'%s'\n",c.x, c.y, map.clearView(c, target, buffer)));
        
        
        return mapper.coordSet.stream()
                .map(c -> map.addCoord(c, c.distance(target), false, !map.clearView(c, target, buffer)))
                .collect(Collectors.toList());
    }

    @Override
    public boolean equivalent(FrontsCoordinate position, Coordinate target) {
        return map.areEquivalent(position.getCoordinate(), target);
    }

    @Override
    public boolean clearView(Position currentPosition, Coordinate target, double buffer) {
        return map.clearView(currentPosition.getCoordinate(), target, buffer);
    }

    class ObstacleMapper {
        final Position currentPosition;
        final Coordinate target;
        final double buffer;
        final Set<Coordinate> newObstacles;
        final Set<Coordinate> coordSet;

        ObstacleMapper(Position currentPosition, double buffer, Coordinate target) {
            this.currentPosition = currentPosition;
            this.target = target;
            this.buffer = buffer + map.getScale().getResolution();
            this.newObstacles = new HashSet<>();
            this.coordSet = new HashSet<Coordinate>();
        }
        
        void doMap(Location relativeObstacle) {
            // filter out any range < 1.0
             if (!DoubleUtils.inRange(relativeObstacle.range(), buffer)) { 
                 // create absolute coordinates
                 Location absoluteObstacle = currentPosition.nextPosition(relativeObstacle);
                 newObstacles.add( map.addObstacle(absoluteObstacle.getCoordinate()) );
                 Optional<Coordinate> possibleCoord = findCoordinateNear(relativeObstacle);
                 if (possibleCoord.isPresent()) {
                     coordSet.add(possibleCoord.get());
                 }
             }
        }


        Optional<Coordinate> findCoordinateNear(Location relativeObstacle) {
            
            double d = relativeObstacle.range() - buffer;
            if (d < buffer) {
                return Optional.empty();
            }
            double theta = relativeObstacle.theta()+currentPosition.getHeading();
            Location candidate =  currentPosition.plus(CoordUtils.fromAngle(theta, d));
            Coordinate newCoord = map.adopt(candidate.getCoordinate());
            if (map.isObstacle(newCoord)) {
                d -= buffer;
                if (d < buffer) {
                    return Optional.empty();
                }
                candidate =  currentPosition.plus(CoordUtils.fromAngle(theta, d));
                newCoord = map.adopt(candidate.getCoordinate());
                if (map.isObstacle(newCoord)) {
                    return Optional.empty();
                }
            }
            return Optional.ofNullable( newCoord.distance(target) < buffer ? null : newCoord);
        }
    }
}
