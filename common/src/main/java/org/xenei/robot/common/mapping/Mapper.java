package org.xenei.robot.common.mapping;

import java.util.Collection;

import org.locationtech.jts.geom.Coordinate;
import org.xenei.robot.common.FrontsCoordinate;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.planning.Step;

public interface Mapper {

    /**
     * Call the sensors, record obstacles, and return a stream of valid points to
     * add. Also sets the obstacleMapper if a collision with the current path was
     * detected.
     * 
     * @param currentPosition The current position.
     * @param buffer the buffer around the currentPosition.
     * @param target the final target
     * @param obstacles the list of obstacles.
     * @return the location of an non-obstacle when heading toward the target.
     * (shortest non collision position)
     */
    Collection<Step> processSensorData(Position currentPosition, double buffer, Coordinate target,
            Location[] obstacles);

    boolean isClearPath(Position currentPosition, Coordinate target, double buffer);

    boolean equivalent(FrontsCoordinate position, Coordinate target);
}
