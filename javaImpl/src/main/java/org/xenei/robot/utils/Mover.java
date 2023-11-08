package org.xenei.robot.utils;

import org.xenei.robot.navigation.Coordinates;
import org.xenei.robot.navigation.Position;

public interface Mover {
    /**
     * Move to the specified location
     * @param location The relative location to move to.
     * @return the new unquantized absolute position.
     */
    Position move(Coordinates location);

    /**
     * @return the current unquantized absolute position.
     */
    Position position();
}
