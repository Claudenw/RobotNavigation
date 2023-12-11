package org.xenei.robot.common.mapping;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.xenei.robot.common.ScaleInfo;
import org.xenei.robot.common.planning.Solution;
import org.xenei.robot.common.planning.Step;

public interface Map {

    /**
     * Clears the map layer.
     * @param mapLayer the name of the map layer.
     */
    void clear(String mapLayer);
    
    /**
     * Returns {@code true} if there is a clear view from {@code source} to
     * {@code dest}.
     * 
     * @param source the coordinates to start at
     * @param dest the coordinates to end it
     * @return true if there are no obstacles between source and dest.
     */
    boolean clearView(Coordinate source, Coordinate dest);

    /**
     * Add the target to the planning
     * 
     * @param target the target to add.
     */
    void addTarget(Step target);

    /**
     * Gets the collection of all targets in the planning graph
     * @return the collection of all targets in the planning graph.
     */
    Collection<Step> getTargets();

    /**
     * Adds a path to the planning graph. 
     * @param coords the coordinates of the path.
     */
    void addPath(Coordinate... coords);

    /**
     * Update the planning model with new distances based on the new target
     * 
     * @param target the new target.
     */
    void recalculate(Coordinate target);

    /**
     * Find the best targets based on the costs in the graph.
     * @param currentCoords the current coordinates to search from.
     * @return An optional step as the best solution empty if there is none.
     */
    Optional<Step> getBestTarget(Coordinate currentCoords);

    /**
     * Sets the adjustment value for the point specified by the step.
     * @param target the point to update.
     */
    void setTemporaryCost(Step target);


    /**
     * Returns true if the coordinate is within an obstacle.
     * @param coord the coordinate to check.
     * @return true if the point is in an obstacle, false otherwise.
     */
    boolean isObstacle(Coordinate coord);

    /**
     * Adds an obstacle to the planning graph.
     * @param obstacle the obstacle to add.
     */
    void addObstacle(Coordinate obstacle);

    /**
     * Gets the geometry for all the known obstacles.
     * @return the set of geometries for all the knowns obstacles.
     */
    Set<Geometry> getObstacles();

    /**
     * Breaks the path between a and b.
     * @param a the first coordinate to break the path for.
     * @param b the second coordinate to break the path for.
     */
    void cutPath(Coordinate a, Coordinate b);

    /**
     * Write the path specified by the solution in the the base model.
     * @param solution the soluiton containing the path. 
     */
    void recordSolution(Solution solution);

    /**
     * Get the scale info for this map.
     * @return the ScaleInfo.
     */
    ScaleInfo getScale();

}
