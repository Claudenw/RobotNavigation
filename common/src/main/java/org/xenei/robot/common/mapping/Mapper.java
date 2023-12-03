package org.xenei.robot.common.mapping;

import org.locationtech.jts.geom.Coordinate;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.planning.Solution;

public interface Mapper {
    default void processSensorData(Position currentPosition, Location[] obstacles) {
        processSensorData( currentPosition, currentPosition.getCoordinate(), obstacles);
    }
    default void processSensorData(Position currentPosition, Coordinate target, Location[] obstacles) {
        processSensorData( currentPosition, target, new Solution(), obstacles);
    }

    void processSensorData(Position currentPosition, Coordinate target, Solution solution, Location[] obstacles);
}
