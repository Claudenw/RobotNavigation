package org.xenei.robot.common.mapping;

import java.util.Collection;

import org.locationtech.jts.geom.Coordinate;
import org.xenei.robot.common.FrontsCoordinate;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.planning.Step;
import org.xenei.robot.common.NavigationSnapshot;

public interface Mapper {

    /**
     * Call the sensors, record obstacles, and return a stream of valid points to
     * add. Also sets the obstacleMapper if a collision with the current path was
     * detected.
     * 
     * @param currentPosition The current position.
     * @param snapshot The current NavigationSnapshot.
     * @param obstacles the relative location of obstacles.
     * @return the location of an non-obstacle when heading toward the target.
     * (shortest non collision position)
     */
    Collection<Step> processSensorData(Coordinate finalTarget, NavigationSnapshot snapshot,
            Location[] obstacles);

    boolean isClearPath(Position currentPosition, Coordinate target);

    boolean equivalent(FrontsCoordinate position, Coordinate target);
    
    interface Visualization {
        /**
         * Redraw the visualization 
         * @param target The target the planner is heading toward.
         */
        public void redraw(Coordinate target);
    }
}
