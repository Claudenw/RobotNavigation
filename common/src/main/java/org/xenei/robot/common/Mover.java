package org.xenei.robot.common;

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

    void setHeading(double heading);

}
