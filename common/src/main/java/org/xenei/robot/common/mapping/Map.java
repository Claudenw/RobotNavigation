package org.xenei.robot.common.mapping;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import org.apache.commons.math3.util.Precision;
import org.apache.jena.rdf.model.Resource;
import org.locationtech.jts.geom.Coordinate;
import org.xenei.robot.common.FrontsCoordinate;

import org.xenei.robot.common.GeometricObject;
import org.xenei.robot.common.LocationI;
import org.xenei.robot.common.ObstacleI;
import org.xenei.robot.common.PositionI;
import org.xenei.robot.common.ScaleInfo;
import org.xenei.robot.common.planning.Solution;
import org.xenei.robot.common.planning.Segment;
import org.xenei.robot.common.utils.RobutContext;
import org.xenei.robot.mapper.map.MapObstacle;
import org.xenei.robot.mapper.rdf.Namespace;

public interface Map<L extends Map.Loc<L>, P extends Map.Pos<L, P>, M extends Map.Obstacle> {
    static Coordinate adopt(Coordinate c, ScaleInfo scaleInfo) {
        double x = scaleInfo.scale(c.getX());
        double y = scaleInfo.scale(c.getY());
        return (Precision.equals(x, c.getX(), 0) && Precision.equals(y, c.getY(), 0)) ? c : new Coordinate(x, y);
    }

    /**
     * Clears the map layer.
     *
     * @param mapLayer
     *            the name of the map layer.
     */
    void clear(String mapLayer);

    /**
     * Returns {@code true} if there is a clear view from {@code source} to
     * {@code dest}.
     *
     * @param source
     *            the coordinates to start at
     * @param dest
     *            the coordinates to end it
     * @return true if there are no obstacles between source and dest.
     */
    boolean isClearPath(FrontsCoordinate source, FrontsCoordinate dest);

    /**
     * Add the target to the planning. If the distance is null, then the result will
     * be empty as there can be no steps to a non-declared target.
     *
     * @param coord
     *            the coordinate for the coord.
     * @param target
     *            the target for planning if defined.
     * @param visited
     *            true if the target has been visited.
     * @return the Step comprising the mapped target location and the distance value
     *         or an empty optional if the target is not defined.
     */
    Optional<Segment> addCoord(FrontsCoordinate coord, FrontsCoordinate target, boolean visited);

    default Optional<Segment> addCoord(FrontsCoordinate coord, FrontsCoordinate target) {
        return addCoord(coord, target, false);
    }

    default Optional<Segment> addCoord(FrontsCoordinate coord) {
        return addCoord(coord, null, false);
    }

    Loc<L> asMapCoordinate(FrontsCoordinate coord);

    Pos<L, P> asMapPosition(PositionI<?, ?> position);

    M asMapObstacle(ObstacleI obstacle);

    /**
     * Gets the collection of all steps in the planning graph that are reachable
     * from the current position.
     *
     * @param position
     *            the coordinates of the current position.
     * @return the collection of all steps in the planning graph.
     */
    Collection<Segment> getSegments(FrontsCoordinate position);

    /**
     * Gets the collection of all coordinates in the planning graph.
     *
     * @return the collection of all coordinates in the planning graph.
     */
    CompletableFuture<Collection<L>> getCoords();

    /**
     * Adds a path to the planning graph.
     *
     * @param coords
     *            the coordinates of the path.
     */
    default CompletableFuture<? extends Path> addPath(FrontsCoordinate... coords) {
        return addPath(Namespace.PlanningModel, coords);
    }

    /**
     * Adds a path to the specified graph.
     *
     * @param model
     *            the name of the graph to add the path to.
     * @param coords
     *            the coordinates of the path.
     */
    CompletableFuture<? extends Path> addPath(Resource model, FrontsCoordinate... coords);

    /**
     * Determins if there is a path from 'a' to 'b'
     *
     * @param a
     *            one end of a path.
     * @param b
     *            the other end of a path.
     * @return {@code true} if a path exists.
     */
    boolean hasPath(FrontsCoordinate a, FrontsCoordinate b);

    /**
     * Update the planning model with new distances based on the new target
     *
     * @param target
     *            the new target.
     */
    L recalculate(FrontsCoordinate target);

    /**
     * Find the best target based on the costs in the graph.
     *
     * @param currentCoords
     *            the current coordinates to search from.
     * @return An optional step as the best solution empty if there is none.
     */
    Optional<Segment> getBestSegment(FrontsCoordinate currentCoords);

    /**
     * Returns true if the coordinate is within an obstacle.
     *
     * @param coord
     *            the coordinate to check.
     * @return true if the point is in an obstacle, false otherwise.
     */
    boolean isObstacle(FrontsCoordinate coord);

//    /**
//     * Adds an obstacle to the planning graph.
//     *
//     * @param obstacle
//     *            the obstacle to add.
//     * @return the set of new obstacles.
//     */
//    CompletableFuture<M> addObstacle(ObstacleI obstacle);

    /**
     * Gets the geometry for all the known obstacles.
     *
     * @return the set of geometries for all the knowns obstacles.
     */
    CompletableFuture<Set<M>> getObstacles();

    /**
     * Breaks the path between a and b.
     *
     * @param a
     *            the first coordinate to break the path for.
     * @param b
     *            the second coordinate to break the path for.
     * @return
     */
    default CompletableFuture<?> cutPath(FrontsCoordinate a, FrontsCoordinate b) {
        return cutPath(Namespace.PlanningModel, a, b);
    }

    /**
     * Breaks the path between a and b.
     *
     * @param a
     *            the first coordinate to break the path for.
     * @param b
     *            the second coordinate to break the path for.
     * @return
     */
    CompletableFuture<?> cutPath(Resource modelName, FrontsCoordinate a, FrontsCoordinate b);

    /**
     * Write the path specified by the solution in the the base model.
     *
     * @param solution
     *            the solution containing the path.
     */
    CompletableFuture<? extends Path> recordSolution(Solution solution);

    /**
     * Get the context info for this map.
     *
     * @return the Context.
     */
    RobutContext getContext();

    /**
     * Update the map so that any Coord that was previously not indirect but is now
     * blocked by newObstacle is marked as indirect.
     *
     * @param finalTarget
     *            The final target
     * @param newObstacles
     *            the set of new obstacles.
     * @return
     */
    CompletableFuture<Void> updateIsIndirect(FrontsCoordinate finalTarget, Set<Obstacle> newObstacles);

    /**
     * Create an Obstacle.
     *
     * @param location    The position of the object.
     * @param relativeLocation the relative location of the obstacle from the start position.
     * @return An obstacle.
     */
    CompletableFuture<M> createObstacle(Coordinate location);


    /**
     * Create an Obstacle.
     *
     * @param startPosition    The position from which we locate the obstacle.
     * @param relativeLocation the relative location of the obstacle from the start position.
     * @return An obstacle.
     */
    CompletableFuture<M> createObstacle(PositionI<?, ?> startPosition, FrontsCoordinate relativeLocation);

    /**
     * Create an Obstacle.
     *
     * @param startPosition
     *            The position from which we locate the obstacle.
     * @param relativeStart
     *            the relative starting location of the obstacle.
     * @param relativeEnd
     *            the relative ending location of the obstacle.
     * @return An obstacle.
     */
    CompletableFuture<M> createObstacle(PositionI<?, ?> startPosition, FrontsCoordinate relativeStart, FrontsCoordinate relativeEnd);

    /**
     * Sets the coordinate as visited in the map.
     *
     * @param coord
     *            the coordinate to mark as visited.
     * @return
     */
    void setVisited(FrontsCoordinate coord);

    /**
     * Look in the given direction for the maximum range. if there is an obstacle
     * report the relative location. otherwise return and empty optional.
     *
     * @param position
     *            the position on the map to look from.
     * @param heading
     *            the direction to look.
     * @param maxRange
     *            the maximum range to look.
     * @return the relative location of a located obstacle or an empty Optional.
     */
    CompletableFuture<Optional<FrontsCoordinate>> look(PositionI<?, ?> position, double heading, int maxRange);

    /**
     * A Visualization of a map.
     */
    @FunctionalInterface
    interface Visualization {
        void redraw();
    }

    interface VisualizationInitializer {
        Map<?, ?, ?> map();
        Supplier<Solution> solutionSupplier();
        Supplier<PositionI<?, ?>> positionSupplier();
        Supplier<FrontsCoordinate> targetSupplier();
    }

    interface TargetData {
        Coordinate getTarget();
        double distance();
        boolean indirect();
    }


    /**
     * A coordinate that has been quantized into a map coordinate.
     */
    interface Loc<L extends Loc<L>> extends LocationI<L>, GeometricObject {
        TargetData addTarget(FrontsCoordinate target);
        boolean isIndirect(FrontsCoordinate target);
    }

    interface Pos<L extends Loc<L>, P extends Pos<L, P>> extends Loc<L>, PositionI<L, P> {
    }

    interface Obstacle extends ObstacleI {
    }

    interface Path extends GeometricObject {
    }
}
