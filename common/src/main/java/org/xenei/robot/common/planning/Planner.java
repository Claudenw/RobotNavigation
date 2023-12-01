package org.xenei.robot.common.planning;

import java.util.Collection;

import org.xenei.robot.common.Position;

import mil.nga.sf.Point;

public interface Planner {
    Position getCurrentPosition();
    Point getTarget(); 
    Collection<Point> getTargets();
    void setTarget(Point target);
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
