package org.xenei.robot.common;

import org.locationtech.jts.geom.Coordinate;


public interface Compass {
    /**
     * Calculates the position from a point and the heading reading.
     * 
     * @param location The location of the device.
     * @return A Position instance that is at the specified location and has the correct heading.
     */
    default Position getPosition(Coordinate location) {
        return Position.from(location, heading());
    }
    
    default Position getPosition(FrontsCoordinate location) {
        return Position.from(location, heading());
    }
    
    /**
     * the current heading.
     * @return  the current heading
     */
    double heading();
}
