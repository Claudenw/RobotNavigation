package org.xenei.robot.common.mapping;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import org.apache.jena.rdf.model.Literal;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
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
    boolean isClearPath(Coordinate source, Coordinate dest, double buffer);

    /**
     * Add the target to the planning
     * 
     * @param target the target to add.
     * @param distance the distance to the final target
     * @param visited true if the target has been visited.
     * @param isIndirect true if the target can not see the final target.
     * @return the Step comprising the mapped target locaton and the distance value.
     */
    Step addCoord(Coordinate target, double distance, boolean visited, boolean isIndirect);

    /**
     * Gets the collection of all targets in the planning graph
     * 
     * @return the collection of all targets in the planning graph.
     */
    Collection<Step> getTargets();

    /**
     * Adds a path to the planning graph.
     * 
     * @param coords the coordinates of the path.
     */
    Coordinate[] addPath(Coordinate... coords);

    /**
     * Update the planning model with new distances based on the new target
     * 
     * @param target the new target.
     */
    Coordinate recalculate(Coordinate target, double buffer);

    /**
     * Find the best targets based on the costs in the graph.
     * 
     * @param currentCoords the current coordinates to search from.
     * @param buffer the buffer required around object. (size of objects / 2 )
     * @return An optional step as the best solution empty if there is none.
     */
    Optional<Step> getBestStep(Coordinate currentCoords, double buffer);

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
    //Coordinate addObstacle(Coordinate obstacle);
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
     * @param solution the soluiton containing the path.
     */
    void recordSolution(Solution solution, double buffer);

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
     * @param target The final target
     * @param buffer the buffer
     * @param newObstacles the set of new obstacles.
     */
    void updateIsIndirect(Coordinate target, double buffer, Set<Obstacle> newObstacles);
    
    /**
     * Create an Obstacle.
     * @param startPosition  The position from which we locate the obstacle.
     * @param relativeLocation the relative locaiton of the obstacle from the start position.
     * @return An obstacle.
     */
    Obstacle createObstacle(Position startPosition, Location relativeLocation);

}
