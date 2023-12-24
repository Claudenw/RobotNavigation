package org.xenei.robot.common.planning;

import java.util.Collection;

import org.locationtech.jts.geom.Coordinate;
import org.xenei.robot.common.FrontsCoordinate;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.mapping.Mapper;

public interface Planner {
    /**
     * Gets the current position according to the planner.
     * 
     * @return the current position.
     */
    Position getCurrentPosition();

    /**
     * Gets the coordinates of the target.
     * 
     * @return the coordinates of the target.
     */
    Coordinate getTarget();

    Coordinate getRootTarget();

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
     */
    void setTarget(Coordinate target);

    /**
     * Set the target for the planner. Setting the target causes the current plan to
     * be cleared and a new plan started.
     * 
     * @param target The coordinates to head toward.
     */
    default void setTarget(FrontsCoordinate target) {
        setTarget(target.getCoordinate());
    }

    /**
     * Replaces the current planner target without clearing the current plan. If the
     * only one target is in the planner stack then this method adds a record.
     * 
     * @param target The coordinates to head toward.
     */
    void replaceTarget(Coordinate coordinate);

    /**
     * Replaces the current planner target without clearing the current plan. If the
     * only one target is in the planner stack then this method adds a record.
     * 
     * @param target The coordinates to head toward.
     */
    default void replaceTarget(FrontsCoordinate target) {
        replaceTarget(target.getCoordinate());
    }

    /**
     * Gets the current solution. May be incomplete.
     * 
     * @return The current solution.
     */
    Solution getSolution();

    /**
     * Plans a step. Returns the best location to move to based on the current
     * position. The target position may be updated. The best position to head for
     * is in the target.
     * 
     * @return true if the target has changed.
     */
    Diff selectTarget();

    /**
     * Change the position to the new position. Causes a recalculation of costs for
     * cells.
     * 
     * @param position the position to reset to.
     */
    void changeCurrentPosition(Position position);

    /**
     * Add a planner listener. The listener will be called when a planning move is
     * completed.
     * 
     * @param listener the listener to notify.
     */
    void addListener(Listener listener);

    /**
     * Notify listeners to reprocess data.
     */
    void notifyListeners();
    
    void recalculateCosts();
    
    /**
     * Restart from the new location using the current map.
     * 
     * @param start the new starting position.
     */
    void restart(Location start);
    
    /**
     * Convenience method to get the steps from the underlying map.
     * Equivalent to map.getTargets();
     * @return the collection of targets from the map.
     * @see Map#getTargets
     */
    Collection<Step> getPlanRecords();
    
    /**
     * Gets the Diff associatd with this planner.
     * @return the Diff associated with this planner.
     */
    Diff getDiff();
    
    /**
     * A functional interface that defines a listener to update.
     */
    @FunctionalInterface
    interface Listener {
        void update();
    }

    interface Diff {

        void reset();

        default boolean didChange() {
            return didHeadingChange() || didPositionChange() || didTargetChange();
        }

        boolean didHeadingChange();

        boolean didPositionChange();

        boolean didTargetChange();
    }
}
