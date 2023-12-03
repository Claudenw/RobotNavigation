package org.xenei.robot.common.planning;

import java.util.Collection;

import org.locationtech.jts.geom.Coordinate;
import org.xenei.robot.common.Position;


public interface Planner {
    Position getCurrentPosition();
    Coordinate getTarget(); 
    Collection<Coordinate> getTargets();
    void setTarget(Coordinate target);
    Solution getSolution();
    /**
     * Plans a step.  Returns the best location to move to based on the current position.
     * The target position may be updated.
     * The best position to head for is in the target.
     * @return true if the target has not been reached. (processing should continue)
     */
    boolean step();
    void changeCurrentPosition(Position position);
}
