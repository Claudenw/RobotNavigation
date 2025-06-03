package org.xenei.robot.common.mapping;

import java.util.Collection;
import java.util.function.Consumer;

import org.locationtech.jts.geom.Coordinate;
import org.xenei.robot.common.FrontsCoordinate;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.planning.Step;
import org.xenei.robot.common.NavigationSnapshot;
import org.xenei.robot.common.utils.DoubleUtils;
import org.xenei.robot.mapper.MapperImpl;

public interface Mapper {

    boolean isClearPath(Position currentPosition, Coordinate target);

    boolean equivalent(FrontsCoordinate position, Coordinate target);

    /**
     * Creates a consumer of relative obstacles (unscaled).
     *
     * @return a consumer of relative obstacles (unscaled).
     */
    Consumer<Location> getRelativeObstacleConsumer();

    /** A visulation of the map */
    interface Visualization {
        /**
         * Redraw the visualization
         *
         * @param target The target the planner is heading toward.
         */
        public void redraw(Coordinate target);
    }
}
