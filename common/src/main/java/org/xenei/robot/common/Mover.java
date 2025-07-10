package org.xenei.robot.common;

import java.util.concurrent.locks.Lock;
import java.util.function.Consumer;

/**
 * Causes the system to move to a designated location.
 * If the bumper sensor triggers.  pause, correct action, reset target to position, stop
 * If the previous target becomes visible, reset target to pos, stop.
 * if target is reached, stop.
 */
public interface Mover extends Consumer<Mover.MoveTo> {
    enum MotorState {RUN, PAUSE, STOP}
    record MoveTo(Location location){};

    /**
     * Move to the specified location
     * 
     * @param location The relative location to move to.
     * @return the new unquantized absolute position.
     */
    void move(Location location);

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
     * Register a logic module operating on this mover.
     * @param logicModule the logic module to register.
     */
    void register(LogicModule logicModule);

    interface LogicModule {
        void setLock(Lock lock);
    }
}
