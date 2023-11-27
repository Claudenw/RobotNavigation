package org.xenei.robot.mapper;

import java.util.Arrays;
import java.util.Optional;

import org.apache.jena.util.iterator.UniqueFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.common.AngleUnits;
import org.xenei.robot.common.Coordinates;
import org.xenei.robot.common.Map;
import org.xenei.robot.common.Mapper;
import org.xenei.robot.common.Planner;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.Target;

import mil.nga.sf.Point;

public class MapperImpl implements Mapper {
    private static final Logger LOG = LoggerFactory.getLogger(MapperImpl.class);
    private final Map map;

    public MapperImpl(Map map) {
        this.map = map;
    }

    /**
     * Call the sensors, record obstacles, and return a stream of valid points to
     * add. Also sets the obstacleMapper if a collision with the current path was
     * detected.
     */
    @Override
    public void processSensorData(Planner planner, Coordinates[] obstacles) {
        // next target set if collision detected.
        Position currentPosition = planner.getCurrentPosition();
        Point target = planner.getTarget();
        Coordinates solution = planner.getSolution().end();
        ObstacleMapper obstacleMapper = new ObstacleMapper(currentPosition, target);
        LOG.trace("Sense position: {}", currentPosition);
        // get the sensor readings and add arcs to the map
        //@formatter:off
        Arrays.stream(obstacles)
                // filter out any range < 1.0
                .filter(c -> c.getRange() > 1.0)
                // create absolute coordinates
                .map(c -> {
                    LOG.trace( "Checking {}", c);
                    return currentPosition.plus(c).quantize();
                })
                .filter( new UniqueFilter<Coordinates>() )
                .map(obstacleMapper::map)
                // filter out non entries
                .filter(c -> c.isPresent() && !c.get().equals(currentPosition))
                .map(Optional::get)
                .filter( new UniqueFilter<Coordinates>() )
                .forEach(c -> recordMapPoint(currentPosition, new Target( c, c.distanceTo(target))));
        //@formatter::on
       
        if (obstacleMapper.nextTarget.isPresent()) {
            map.cutPath(solution, target);
            map.path(solution, obstacleMapper.nextTarget.get());
        }
    }

    private void recordMapPoint(Position currentPosition, Target target) {
        map.add(target);
        map.path(currentPosition, target);
    }

    /**
     * Finds an open position closer to the position from the badCell
     * @param obstacle the position of an obstacle or other location not to be in.
     * @return An optional that contains the nearest open coordinates if any.
     */
    private Optional<Coordinates> not(Position currentPosition, Coordinates obstacle) {
        Coordinates direct0 = obstacle.minus(currentPosition);
        Coordinates direct = Coordinates.fromAngle(direct0.getTheta(AngleUnits.RADIANS), AngleUnits.RADIANS, direct0.getRange() - 1);
        Coordinates qCandidate = currentPosition.plus(direct).quantize();
        if (map.isObstacle(qCandidate)) {
            Coordinates adjustment = qCandidate.minus(obstacle);
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
                qCandidate = Coordinates.fromXY(qCandidate.getX() + xAdj, qCandidate.getY() + yAdj);
                if (map.isObstacle(qCandidate)) {
                    return Optional.empty();
                }
            }
        }
        return Optional.of(qCandidate);
    }

    

    /**
     * Registers the obstacle on the map and checks if it is on the path we are currently traveling.
     * @param position the position we are currently at.
     * @param obstacle the obstacle we may hit.
     * @return true if the obstacle is on our path.
     */
    private boolean registerObstacle(Position currentPosition, Point target, Point obstacle) {
        map.setObstacle(obstacle);
        boolean result = !currentPosition.hasClearView(target, obstacle);
        if (result) {
            LOG.info("Future collision from {} detected at {}", currentPosition, obstacle);
        }
        return result;
    }
    
    class ObstacleMapper {
        private Optional<Coordinates> nextTarget = Optional.empty();
        private double d = 0.0;
        private final Position currentPosition;
        private final Point target;
        
        ObstacleMapper(Position currentPosition, Point target) {
            this.currentPosition = currentPosition;
            this.target = target;
        }
        
        public Optional<Coordinates> map(Coordinates obstacle) {
            LOG.debug("Sensed {}", obstacle);
            boolean collisionDetected = registerObstacle(currentPosition, target, obstacle);
            Optional<Coordinates> result = not(currentPosition, obstacle);
            if (collisionDetected) {
                if (result.isPresent()) {
                    if (nextTarget.isEmpty()) {
                        nextTarget = result;
                        d = result.get().distanceTo(currentPosition);
                    } else {
                        double n = result.get().distanceTo(currentPosition);
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
