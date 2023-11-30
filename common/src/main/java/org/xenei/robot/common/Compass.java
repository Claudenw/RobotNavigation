package org.xenei.robot.common;

import mil.nga.sf.Point;

public interface Compass {
    /**
     * Calculates the position from a point and the heading reading.
     * 
     * @param location The location of the device.
     * @return A Position instance that is at the specified location and has the correct heading.
     */
    default Position getPosition(Point location) {
        return new Position(location, heading());
    }
    
    /**
     * the current heading.
     * @return  the current heading
     */
    double heading();
}
