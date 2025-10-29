package org.xenei.robot.common.mapping;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.xenei.robot.common.GeometricObject;
import org.xenei.robot.common.Obstacle;

import java.util.Collection;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public interface MapStorage {
    /**
     * Get the reports instance for this storage.
     * @return the Reports instance.
     */
    Reports<?> getReports();

    /**
     * Save the map location in storage.
     * @param mapLocation the map location to store.
     * @return a future with the locaton.
     */
    CompletableFuture<MapLocation> saveLocation(MapLocation mapLocation);

    /**
     * get the list of coordinates within the bounding box.
     * If the bounding box is null, return all the coordinates.
     * @param boundingBox the bounding box. {@code may be null}.
     * @return the list of coordinates.
     */
    CompletableFuture<Stream<Coordinate>> getLocations(Geometry boundingBox);

    /**
     * Determine the shortest path between {@code start} and {@code end}.
     * @param start the starting location.
     * @param end the endint location.
     * @return the shortest path.
     */
    CompletableFuture<Map<Coordinate, Set<Coordinate>>> getPath(MapLocation start, MapLocation end);

    /**
     * Remove all the data from the storage.
     */
    void clear();

    /**
     * Add a path to the storage.
     * @param locations the locations (in order) for the path.
     * @return a CompletableFuture.
     */
    CompletableFuture<?> addPath(Collection<MapLocation> locations);

    /**
     * Finds the obstacles that are contained in or intersect with the geometric object.
     * @param geometricObject the geometry to search for.
     * @return a stream of Obstacle instances that were found.
     */
    CompletableFuture<Stream<Obstacle>> findTouchingObstacles(GeometricObject geometricObject);

    /**
     * Remove the obstacles associated with the UUIDs from storage.
     * @param obstacleIds the obstacle UUIDs to remove.
     * @return a CompletableFuture.
     */
    CompletableFuture<?> removeObstacles(Stream<UUID> obstacleIds);

    /**
     * Add an obstacle to storage.
     * @param mapObstacle the obstacle to store.
     * @return a CompletableFuture.
     */
    CompletableFuture<?> addObstacle(MapObstacle mapObstacle);

    /**
     * Get all the obstacles from storage.
     * @return a steam of all the obstacles in storage.
     */
    CompletableFuture<Stream<Obstacle>> getObstacles();

    /**
     * Remove the MapLocations within the bounding box.
     * @param boundingBox the bounding box.
     * @return an empty completable future.
     */
    CompletableFuture<?> removeCoordinates(GeometricObject boundingBox);

    /**
     * Determines if there is a path from location a to location b.
     * @param a the starting location.
     * @param b the ending location.
     * @return CompletableFuture will return {@code true} if there is a path.
     */
    CompletableFuture<Boolean> hasPath(MapLocation a, MapLocation b);

    /**
     * The reports for the map storage
     * @param <N> the name for any sub models.
     */
    interface Reports<N> {

        default String dumpModel() {
            return dumpModel(null);
        }
        /**
         * Dumps the internal model for the storage.
         * @param subModel the sub model to dump. if {@code null} all submodels are dumped.
         * @return the string representation of the model.
         */
        String dumpModel(N subModel);

        default String dumpObstacles() {
            return dumpObstacles(null);
        }
        /**
         * Dump the string representation of all the obstacles in the model.
         * @param subModel the sub model to dump. if {@code null} all submodels are dumped.
         * @return the string representation of all the obstacles in the model
         */
        String dumpObstacles(N subModel);

        /**
         * Dump all the distances between the obstacles.
         * @return a string representation of all the distances between the obstacles.
         */
        default String dumpObstacleDistance() {
            return dumpObstacleDistance(null);
        }

        /**
         * Dump all the distances between the obstacles.
         * @param subModel the sub model to dump. if {@code null} all submodels are dumped.
         * @return a string representation of all the distances between the obstacles.
         */
        String dumpObstacleDistance(N subModel);
    }
}
