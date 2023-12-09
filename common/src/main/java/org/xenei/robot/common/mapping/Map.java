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

    Collection<Step> getTargets();

    void addPath(Coordinate... coords);

    void recalculate(Coordinate target);

    Optional<Step> getBestTarget(Coordinate currentCoords);

    void setTemporaryCost(Step target);

    /**
     * Reset the target position. Builds a new map from all known points.
     * 
     * @param target the New target.
     */
    // void reset(Coordinate target);
    boolean isObstacle(Coordinate coord);

    void addObstacle(Coordinate obstacle);

    Set<Geometry> getObstacles();

    void cutPath(Coordinate a, Coordinate b);

    void recordSolution(Solution solution);

    ScaleInfo getScale();

}
