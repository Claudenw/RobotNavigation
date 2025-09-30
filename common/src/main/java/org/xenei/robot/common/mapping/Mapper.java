package org.xenei.robot.common.mapping;

import java.util.function.Consumer;

import org.xenei.robot.common.DistanceSensor;
import org.xenei.robot.common.FrontsCoordinate;
import org.xenei.robot.common.PositionI;

public interface Mapper {

    /**
     * Determines if there is a clear path from the current position to the target.
     *
     * @param currentPosition
     *            the current position.
     * @param target
     *            the target.
     * @return {@code true} if the path has no obstacles, {@code false} otherwise.
     */
    boolean isClearPath(PositionI<?, ?> currentPosition, FrontsCoordinate target);

    /**
     * Creates a consumer of relative obstacles (unscaled).
     *
     * @return a consumer of relative obstacles (unscaled).
     */
    Consumer<DistanceSensor.Readings> getRelativeObstacleConsumer();

}
