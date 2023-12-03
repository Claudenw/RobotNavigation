package org.xenei.robot.mapper;

import java.util.Arrays;
import java.util.Optional;

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
import org.xenei.robot.common.utils.CoordUtils;

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

    /**
     * Call the sensors, record obstacles, and return a stream of valid points to
     * add. Also sets the obstacleMapper if a collision with the current path was
     * detected.
     */
    @Override
    public void processSensorData(Position currentPosition, Coordinate target, Solution solution, Location[] obstacles) {
        // next target set if collision detected.
        ObstacleMapper obstacleMapper = new ObstacleMapper(currentPosition, target);
        LOG.trace("Sense position: {}", currentPosition);
        // get the sensor readings and add arcs to the map
        //@formatter:off
        Arrays.stream(obstacles)
                // filter out any range < 1.0
                .filter(c -> c.range() > 1.0)
                // create absolute coordinates
                .map(c -> {
                    LOG.trace( "Checking {}", c);
                    return new Location( CoordUtils.fromAngle( currentPosition.theta()+
                            c.theta(), c.range()));
                })
                .filter( new UniqueFilter<Location>() )
                .map(obstacleMapper::map)
                // filter out non entries
                .filter(c -> c.isPresent() && !c.get().equals(currentPosition))
                .map(Optional::get)
                .filter( new UniqueFilter<Location>() )
                .forEach(c -> recordMapPoint(currentPosition, new Step( c.getCoordinate(), c.distance(target), null)));
        //@formatter::on
       
        if (obstacleMapper.nextTarget.isPresent() && !solution.isEmpty()) {
            map.cutPath(solution.end(), target);
            map.addPath(solution.end(), obstacleMapper.nextTarget.get().getCoordinate());
        }
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
        Location direct = new Location(obstacle.minus(currentPosition));
        Location candidate = new Location( CoordUtils.fromAngle(direct.theta(), direct.range() - 1));
        if (map.isObstacle(candidate.getCoordinate())) {
            Location adjustment = new Location(candidate.minus(obstacle));
            int xAdj = 0;
            int yAdj = 0;
            if (adjustment.getX() == 0) {
                double signum = Math.signum(direct.getX());
                if (signum < 0) {
                    xAdj = 1;
                } else if (signum > 0) {
                    xAdj = -1;
                }
            }
            if (adjustment.getY() == 0) {
                double signum = Math.signum(direct.getY());
                if (signum < 0) {
                    yAdj = 1;
                } else if (signum > 0) {
                    yAdj = -1;
                }
            }
            if (xAdj != 0 || yAdj != 0) {
                candidate = new Location(candidate.getX() + xAdj, candidate.getY() + yAdj);
                if (map.isObstacle(candidate.getCoordinate())) {
                    return Optional.empty();
                }
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
        map.setObstacle(obstacle);
        boolean result = map.clearView(currentPosition.getCoordinate(), target);
        if (result) {
            LOG.info("Future collision from {} detected at {}", currentPosition, obstacle);
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
