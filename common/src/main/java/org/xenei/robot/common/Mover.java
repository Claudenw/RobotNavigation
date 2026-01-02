package org.xenei.robot.common;

import org.xenei.robot.common.mapping.MapCoordinate;
import org.xenei.robot.common.utils.SerializerDeserializer;

import java.util.concurrent.locks.Lock;

/**
 * Causes the system to move to a designated location. If the bumper sensor
 * triggers. pause, correct action, reset target to position, stop If the
 * previous target becomes visible, reset target to pos, stop. if target is
 * reached, stop.
 */
public interface Mover extends AutoCloseable {

    /**
     * The state for hte motors.
     */
    final class MotorState {
        public final static byte RUN = 0;
        public final static byte PAUSE = 1;
        public final static byte STOP = 2;

        public final static byte MAX_STATE = STOP;

        private MotorState() {
            // do not instantiate.
        }

        public static class Serde extends SerializerDeserializer.ByteSerde {
            public byte[] serialize(byte state) {
                return new byte[]{validateState(state)};
            }

            public byte deserialize(byte[] buffer) throws IllegalArgumentException {
                return validateState(buffer[0]);
            }

            private static byte validateState(byte state) {
                return switch (state) {
                    case RUN, PAUSE, STOP -> state;
                    default -> throw new IllegalArgumentException("Unknown motor state: " + state);
                };
            }
        }
    }

    /**
     * The absolute location to move to.
     * @param location the absolute location to move to.
     */
    record MoveTo(MapCoordinate location) {
    }

    /**
     * Move to the specified location
     *
     * @param location
     *            The relative location to move to.
     */
    void move(Location location);

    /**
     * @return the current absolute position.
     */
    Position position();

    /**
     * Sets the absolute heading for the mover.
     *
     * @param heading
     *            the absolute heading specified in radians.
     */
    void setHeading(double heading);

    /**
     * Register a logic module operating on this mover.
     *
     * @param logicModule
     *            the logic module to register.
     */
    void register(LogicModule logicModule);

    interface LogicModule {
        void setLock(Lock lock);
        void shutdown();
    }
}
