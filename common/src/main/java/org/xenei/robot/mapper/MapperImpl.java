package org.xenei.robot.mapper;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.jena.util.iterator.UniqueFilter;
import org.locationtech.jts.geom.Coordinate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.mapping.Map;
import org.xenei.robot.common.mapping.Mapper;
import org.xenei.robot.common.planning.Planner;
import org.xenei.robot.common.planning.Solution;
import org.xenei.robot.common.planning.Step;
import org.xenei.robot.common.utils.AngleUtils;
import org.xenei.robot.common.utils.CoordUtils;
import org.xenei.robot.mapper.rdf.Namespace;

public class MapperImpl implements Mapper {
    private static final Logger LOG = LoggerFactory.getLogger(MapperImpl.class);
    private final Map map;
    private Planner planner;
    
    public MapperImpl(Map map) {
        this.map = map;
    }

    public void registerPlanner(Planner planner) {
        this.planner = planner;
    }

    @Override
    public Optional<Location> processSensorData(Position currentPosition, Coordinate target, Solution solution, Location[] obstacles) {
        // next target set if collision detected.
        ObstacleMapper obstacleMapper = new ObstacleMapper(currentPosition, target);
        LOG.trace("Sense position: {}", currentPosition);
        double halfScale = map.getScale()/2;
        // get the sensor readings and add arcs to the map
        //@formatter:off
        Arrays.stream(obstacles)
                // filter out any range < 1.0
                .filter(c -> c.range() > map.getScale())
                // create absolute coordinates
                .map(c -> {
                    LOG.trace( "Checking {}", c);
                    //return new Location( CoordUtils.fromAngle( currentPosition.theta()+
                            //c.theta(), c.range()));
                    return currentPosition.plus(c);
                })
                .filter( new UniqueFilter<Location>() )
                .map(obstacleMapper::map)
                // filter out non entries
                .filter(c -> c.isPresent() && !c.get().near(currentPosition, .5))
                .map(Optional::get)
                .filter( new UniqueFilter<Location>() )
                .forEach(c -> recordMapPoint(currentPosition, new Step( c.getCoordinate(), c.distance(target), null)));
        //@formatter::on
       
        if (obstacleMapper.nextTarget.isPresent() && !solution.isEmpty()) {
            map.cutPath(solution.end(), target);
            map.addPath(solution.end(), obstacleMapper.nextTarget.get().getCoordinate());
            
            if (currentPosition.distance(obstacleMapper.nextTarget.get()) >= currentPosition.distance(target)) {
                obstacleMapper.nextTarget = Optional.empty();
            } else if (currentPosition.equals2D(target, halfScale)) {
                obstacleMapper.nextTarget = Optional.empty();
            }
        }
        
        System.out.println( MapReports.dumpModel((MapImpl) map));
       // ((MapImpl)map).doCluster(Namespace.Obst, 2.5, 2);

        return obstacleMapper.nextTarget;
    }

    private void recordMapPoint(Position currentPosition, Step target) {
        map.addTarget(target);
        map.addPath(currentPosition.getCoordinate(), target.getCoordinate());
    }

    /**
     * Finds an open position closer to the position from the badCell
     * @param obstacle the position of an obstacle or other location not to be in.
     * @return An optional that contains the nearest open coordinates if any.
     */
    private Optional<Location> not(Position currentPosition, Location obstacle) {
        double d = currentPosition.distance(obstacle) - map.getScale();
        double theta = currentPosition.angleTo(obstacle);
        Coordinate difference = CoordUtils.fromAngle(theta,d);
        Location candidate = currentPosition.plus(difference);
        if (map.isObstacle(candidate.getCoordinate())) {
            difference = CoordUtils.fromAngle(theta,d-map.getScale());
            candidate = currentPosition.plus(difference);;
            if (map.isObstacle(candidate.getCoordinate())) {
                return Optional.empty();
            }
        }
        return Optional.of(candidate);
    }

    

    /**
     * Registers the obstacle on the map and checks if it is on the path we are currently traveling.
     * @param position the position we are currently at.
     * @param obstacle the obstacle we may hit.
     * @return true if the obstacle is on our path.
     */
    private boolean registerObstacle(Position currentPosition, Coordinate target, Coordinate obstacle) {
        map.addObstacle(obstacle);
        boolean result = currentPosition.checkCollistion(obstacle, map.getScale());
        if (result) {
            LOG.info("Possible collision from {} detected at {}", currentPosition, obstacle);
        }
        return result;
    }
    
    class ObstacleMapper {
        private Optional<Location> nextTarget = Optional.empty();
        private double d = 0.0;
        private final Position currentPosition;
        private final Coordinate target;
        
        ObstacleMapper(Position currentPosition, Coordinate target) {
            this.currentPosition = currentPosition;
            this.target = target;
        }
        
        public Optional<Location> map(Location obstacle) {
            LOG.debug("Sensed {}", obstacle);
            boolean collisionDetected = registerObstacle(currentPosition, target, obstacle.getCoordinate());
            Optional<Location> result = not(currentPosition, obstacle);
            if (collisionDetected) {
                if (result.isPresent()) {
                    if (nextTarget.isEmpty()) {
                        nextTarget = result;
                        d = result.get().distance(currentPosition);
                    } else {
                        double n = result.get().distance(currentPosition);
                        if (n<d) {
                            d = n;
                            nextTarget = result;
                        }
                    }
                }
            }
            return result;
        }
        
        public void reset() {
            nextTarget = Optional.empty();
            d = 0.0;
        }
    }
}
