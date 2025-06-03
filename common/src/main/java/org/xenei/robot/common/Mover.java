package org.xenei.robot.common;

import java.util.function.Consumer;

public interface Mover {
    /**
     * Move to the specified location
     * 
     * @param location The relative location to move to.
     * @return the new unquantized absolute position.
     */
    Position move(Location location);

    /**
     * @return the current absolute position.
     */
    Position position();

    /**
     * Sets the heading for the mover.
     * @param heading the absolute heading specified in radians.
     */
    void setHeading(double heading);

    /**
     * Gets a BumpSensor listener.
     * @return a BumpSensor listener.
     */
    Consumer<BumpSensor.BumpState> getBumpSensorListener();
}
