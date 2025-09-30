package org.xenei.robot.common.planning;

import java.util.Collection;
import java.util.Optional;

import org.xenei.robot.common.FrontsCoordinate;
import org.xenei.robot.common.NavigationSnapshot;

public interface Planner {

    /**
     * Gets the coordinates of the target.
     *
     * @return the coordinates of the target., or {@code null} if there is no
     *         target.
     */
    FrontsCoordinate getTarget();

    /**
     * Gets the final target that this planner is working toward.
     *
     * @return the Final target.
     */
    FrontsCoordinate getFinalTarget();

    /**
     * Gets the planning targets. This is a stack of targets where bottom of the
     * stack is the final target and all other entries are intermediate targets on
     * the path to the final target.
     *
     * @return the Target stack.
     */
    Collection<? extends FrontsCoordinate> getTargets();

    /**
     * Set the target for the planner. Setting the target causes the current plan to
     * be cleared and a new plan started.
     *
     * @param target
     *            The coordinates to head toward.
     * @return the heading to the target.
     */
    double setTarget(FrontsCoordinate target);

    /**
     * Replaces the current planner target without clearing the current plan. If the
     * only one target is in the planner stack then this method adds a record.
     *
     * @param target
     *            The coordinates to head toward.
     */
    void replaceTarget(FrontsCoordinate target);

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
     * Finds a {@link Segment}. Returns the best location to move to based on the
     * current position.
     *
     * @return The Segment toward the target.
     */
    Optional<Segment> selectSegment();

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
