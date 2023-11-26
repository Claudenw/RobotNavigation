package org.xenei.robot.common;

import java.util.Collection;

public interface Planner {
    Position getCurrentPosition();
    Coordinates getTarget(); 
    Collection<Coordinates> getTargets();
    void setTarget(Coordinates target);
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
