package org.xenei.robot.common;

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
