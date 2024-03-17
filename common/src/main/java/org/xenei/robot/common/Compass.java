package org.xenei.robot.common;

import org.locationtech.jts.geom.Coordinate;

public interface Compass {
    /**
     * Calculates the position from a point and the heading reading.
     * 
     * @param location The location of the device.
     * @return A Position instance that is at the specified location and has the
     * correct heading.
     */
    default Position getPosition(Coordinate location) {
        return Position.from(location, heading());
    }

    /**
     * Sets calculates a position (sets the heading) from a location.
     * @param location the location.
     * @return the Position
     */
    default Position getPosition(FrontsCoordinate location) {
        return Position.from(location, heading());
    }

    /**
     * the current heading.  This may be an averaged value over several readings and may 
     * differ slightly from the current absolute heading reading.
     * 
     * @return the current heading
     */
    double heading();
    
    /**
     * The heading read from the device at this instant.  This may differ from the {@code heading()} 
     * results as this is an instantaneous value and that one may be averaged.
     * @return the instantaneous heading measurement.
     */
    double instantHeading();
}
