package org.xenei.robot.common.mapping;

import java.util.Optional;

import org.locationtech.jts.geom.Coordinate;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.planning.Solution;

public interface Mapper {

    /**
     * Call the sensors, record obstacles, and return a stream of valid points to
     * add. Also sets the obstacleMapper if a collision with the current path was
     * detected.
     * @param currentPosition The current position.
     * @param target the target we are trying to reach.
     * @param solution the current solution state.
     * @param obstacles the list of obstacles.
     * @return the location of an non-obstacle when heading toward the target. (shortest non collision position)
     */
    Optional<Location> processSensorData(Position currentPosition, Coordinate target, Solution solution, Location[] obstacles);
}
