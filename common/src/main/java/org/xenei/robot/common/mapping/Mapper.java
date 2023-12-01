package org.xenei.robot.common.mapping;

import org.xenei.robot.common.Coordinates;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.planning.Solution;

import mil.nga.sf.Point;

public interface Mapper {
    default void processSensorData(Position currentPosition, Coordinates[] obstacles) {
        processSensorData( currentPosition, currentPosition, obstacles);
    }
    default void processSensorData(Position currentPosition, Point target, Coordinates[] obstacles) {
        processSensorData( currentPosition, target, new Solution(), obstacles);
    }

    void processSensorData(Position currentPosition, Point target, Solution solution, Coordinates[] obstacles);
}
