package org.xenei.robot.common.mapping;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import org.apache.jena.rdf.model.Resource;
import org.locationtech.jts.geom.Coordinate;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.ScaleInfo;
import org.xenei.robot.common.planning.Solution;
import org.xenei.robot.common.planning.Step;
import org.xenei.robot.common.utils.RobutContext;

public interface Map {

    /**
     * Clears the map layer.
     * 
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
    boolean isClearPath(Coordinate source, Coordinate dest);

    /**
     * Add the target to the planning
     *
     * If the distance is null, then the result will be empty as there can be no
     * steps to a non-declared target.
     *
     * @param target the target to add.
     * @param distance the distance to the final target.
     * @param visited true if the target has been visited.
     * @param isIndirect true if the target can not see the final target.
     * @return the Step comprising the mapped target location and the distance
     * value.
     */
    Optional<Step> addCoord(Coordinate target, Double distance, boolean visited, Boolean isIndirect);

    /**
     * Gets the collection of all steps in the planning graph that are reachable
     * from the current position.
     * 
     * @param position the coordinates of the current position.
     * @return the collection of all steps in the planning graph.
     */
    Collection<Step> getSteps(Coordinate position);

    /**
     * Gets the collection of all coordinates in the planning graph.
     * 
     * @return the collection of all coordinates in the planning graph.
     */
    Collection<MapCoord> getCoords();

    /**
     * Adds a path to the planning graph.
     * 
     * @param coords the coordinates of the path.
     */
    Coordinate[] addPath(Coordinate... coords);

    /**
     * Adds a path to the specified graph.
     * 
     * @param model the name of the graph to add the path to.
     * @param coords the coordinates of the path.
     */
    Coordinate[] addPath(Resource model, Coordinate... coords);

    /**
     * Update the planning model with new distances based on the new target
     * 
     * @param target the new target.
     */
    Coordinate recalculate(Coordinate target);

    /**
     * Find the best targets based on the costs in the graph.
     * 
     * @param currentCoords the current coordinates to search from.
     * @return An optional step as the best solution empty if there is none.
     */
    Optional<Step> getBestStep(Coordinate currentCoords);

    /**
     * Returns true if the coordinate is within an obstacle.
     * 
     * @param coord the coordinate to check.
     * @return true if the point is in an obstacle, false otherwise.
     */
    boolean isObstacle(Coordinate coord);

    /**
     * Adds an obstacle to the planning graph.
     * 
     * @param obstacle the obstacle to add.
     */
    // Coordinate addObstacle(Coordinate obstacle);
    Set<Obstacle> addObstacle(Obstacle obstacle);

    /**
     * Gets the geometry for all the known obstacles.
     * 
     * @return the set of geometries for all the knowns obstacles.
     */
    Set<Obstacle> getObstacles();

    /**
     * Breaks the path between a and b.
     * 
     * @param a the first coordinate to break the path for.
     * @param b the second coordinate to break the path for.
     */
    void cutPath(Coordinate a, Coordinate b);

    /**
     * Write the path specified by the solution in the the base model.
     * 
     * @param solution the solution containing the path.
     */
    void recordSolution(Solution solution);

    /**
     * Get the context info for this map.
     * 
     * @return the Context.
     */
    RobutContext getContext();

    /**
     * True if the two coordinates resolve to the same point on the map. This
     * comparison accounts for resolution.
     * 
     * @param a A coordinate
     * @param b A second coordinate.
     * @return true if they resolve to the same point on the map.
     */
    boolean areEquivalent(Coordinate a, Coordinate b);

    /**
     * Converts coordinate to internal mapping coordinate adjusting for scale and
     * resoluiton.
     * 
     * @param a the coordinate to adopt.
     * @return the map based coordinate.
     */
    Coordinate adopt(Coordinate a);

    /**
     * Update the map so that any Coord that was previously not indirect but is now
     * blocked by newObstacle is marked as indirect.
     * 
     * @param finalTarget The final target
     * @param newObstacles the set of new obstacles.
     */
    void updateIsIndirect(Coordinate finalTarget, Set<Obstacle> newObstacles);

    /**
     * Create an Obstacle.
     * 
     * @param startPosition The position from which we locate the obstacle.
     * @param relativeLocation the relative locaiton of the obstacle from the start
     * position.
     * @return An obstacle.
     */
    Obstacle createObstacle(Position startPosition, Location relativeLocation);

    /**
     * Sets the coordinate as visited in the map.
     * 
     * @param finalTarget the final target we are headed to.
     * @param coord the coordinate to mark as visited.
     * @return
     */
    void setVisited(Coordinate finalTarget, Coordinate coord);

    /**
     * Look in the given direction for the maximum range. if there is an obstacle
     * report the relative location. otherwise return and empty optional.
     * 
     * @param position the position on the map to look from.
     * @param heading the direction to look.
     * @param maxRange the maximum range to look.
     * @return the relative location of a located obstacle or an empty Optional.
     */
    Optional<Location> look(Position position, double heading, int maxRange);
}
