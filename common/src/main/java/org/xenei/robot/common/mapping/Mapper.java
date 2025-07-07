package org.xenei.robot.common.mapping;

import java.util.Collection;
import java.util.function.Consumer;

import org.locationtech.jts.geom.Coordinate;
import org.xenei.robot.common.DistanceSensor;
import org.xenei.robot.common.FrontsCoordinate;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.planning.Step;
import org.xenei.robot.common.NavigationSnapshot;
import org.xenei.robot.common.utils.DoubleUtils;
import org.xenei.robot.mapper.MapperImpl;

public interface Mapper {

    /**
     * Determines if there is a clear path from the current position to the target.
     * @param currentPosition the current position.
     * @param target the target.
     * @return {@code true} if the path has no obstacles, {@code false} otherwise.
     */
    boolean isClearPath(Position currentPosition, Coordinate target);

    /**
     * Determines if the two positions are equivalent in the context of the map.
     * @param position the first position,
     * @param target the target.
     * @return {@code true} if the positions are indistinguishable within the resolution of the map.
     */
    boolean equivalent(FrontsCoordinate position, Coordinate target);

    /**
     * Creates a consumer of relative obstacles (unscaled).
     *
     * @return a consumer of relative obstacles (unscaled).
     */
    Consumer<DistanceSensor.Readings> getRelativeObstacleConsumer();

    /** A visulation of the map */
    interface Visualization {
        /**
         * Redraw the visualization
         */
        public void redraw();
    }
}
