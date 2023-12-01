package org.xenei.robot.common.mapping;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import org.xenei.robot.common.Coordinates;
import org.xenei.robot.common.planning.Solution;
import org.xenei.robot.common.planning.Target;

import mil.nga.sf.Point;


public interface Map {

    /**
     * Returns {@code true} if there is a clear view from {@code source} to {@code dest}.
     * @param source the coordinates to start at
     * @param dest the coordinates to end it
     * @return true if there are no obstacles between source and dest.
     */
    boolean clearView(Point source, Point dest);
    void add(Target target);
    Collection<Target> getTargets();
    boolean path(Point from, Point to);
    void recalculate(Point target);
    Optional<Target> getBestTarget(Point currentCoords);
    void setTemporaryCost(Target target);
    /**
     * Reset the target position. Builds a new map from all known points.
     * 
     * @param target the New target.
     */
    void reset(Point target);
    boolean isObstacle(Point coord);
    void setObstacle(Point obstacle);
    Set<Coordinates> getObstacles();
    void cutPath(Point a, Point b);
    void recordSolution(Solution solution);
}
