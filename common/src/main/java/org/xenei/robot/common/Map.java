package org.xenei.robot.common;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;


public interface Map {

    /**
     * Returns {@code true} if there is a clear view from {@code source} to {@code dest}.
     * @param source the coordinates to start at
     * @param dest the coordinates to end it
     * @return true if there are no obstacles between source and dest.
     */
    boolean clearView(Coordinates source, Coordinates dest);
    void add(Target target);
    Collection<Target> getTargets();
    boolean path(Coordinates from, Coordinates to);
    void recalculate(Coordinates target);
    Optional<Target> getBestTarget(Coordinates currentCoords);
    void setTemporaryCost(Target target);
    /**
     * Reset the target position. Builds a new map from all known points.
     * 
     * @param target the New target.
     */
    void reset(Coordinates target);
    boolean isObstacle(Coordinates coord);
    void setObstacle(Coordinates obstacle);
    Set<Coordinates> getObstacles();
    void cutPath(Coordinates a, Coordinates b);
    void recordSolution(Solution solution);
}
