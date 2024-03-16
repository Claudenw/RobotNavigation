package org.xenei.robot.common.planning;

import java.util.Collection;

import org.locationtech.jts.geom.Coordinate;
import org.xenei.robot.common.FrontsCoordinate;
import org.xenei.robot.common.ListenerContainer;
import org.xenei.robot.common.NavigationSnapshot;

public interface Planner extends ListenerContainer {

    /**
     * Gets the coordinates of the target.
     * 
     * @return the coordinates of the target.
     */
    Coordinate getTarget();

    /**
     * Gets the final target that this planner is working toward.
     * 
     * @return the Final target.
     */
    Coordinate getFinalTarget();

    /**
     * Gets the planning targets. This is a stack of targets where bottom of the
     * stack is the final target and all other entries are intermediate targets on
     * the path to the final target.
     * 
     * @return the Target stack.
     */
    Collection<Coordinate> getTargets();

    /**
     * Set the target for the planner. Setting the target causes the current plan to
     * be cleared and a new plan started.
     * 
     * @param target The coordinates to head toward.
     * @return the heading to the target.
     */
    double setTarget(Coordinate target);

    /**
     * Set the target for the planner. Setting the target causes the current plan to
     * be cleared and a new plan started.
     * 
     * @param target The coordinates to head toward.
     * @return the heading to the target.
     */
    default double setTarget(FrontsCoordinate target) {
        return setTarget(target.getCoordinate());
    }

    /**
     * Replaces the current planner target without clearing the current plan. If the
     * only one target is in the planner stack then this method adds a record.
     * 
     * @param target The coordinates to head toward.
     * @return the heading to the new target.
     */
    double replaceTarget(Coordinate coordinate);

    /**
     * Replaces the current planner target without clearing the current plan. If the
     * only one target is in the planner stack then this method adds a record.
     * 
     * @param target The coordinates to head toward.
     * @return the heading to the new target.
     */
    default double replaceTarget(FrontsCoordinate target) {
        return replaceTarget(target.getCoordinate());
    }

    /**
     * Gets the current solution. May be incomplete.
     * 
     * @return The current solution.
     */
    Solution getSolution();

    /**
     * Records the current solution on the map.
     */
    void recordSolution();

    /**
     * Plans a step. Returns the best location to move to based on the current
     * position. The target position may be updated. The best position to head for
     * is in the target.
     * 
     * @return The snapshot from the target selecton.
     */
    NavigationSnapshot selectTarget();

    /**
     * Sets the registers the current position as part of the solution.
     */
    void registerPositionChange(NavigationSnapshot snapshot);

    /**
     * Recalculate all the costs for movement.
     */
    void recalculateCosts();

    /**
     * Gets the current NavigationSnapshot the planner is working with.
     * 
     * @return the NavigationSnapshot the planner is working with.
     */
    NavigationSnapshot getSnapshot();

}
