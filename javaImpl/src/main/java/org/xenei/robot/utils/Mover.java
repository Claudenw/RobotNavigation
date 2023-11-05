package org.xenei.robot.utils;

import org.xenei.robot.navigation.Coordinates;
import org.xenei.robot.navigation.Position;

public interface Mover {
    Position move(Coordinates location);

    Position position();
}
