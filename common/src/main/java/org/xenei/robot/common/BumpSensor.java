package org.xenei.robot.common;

import java.util.function.Consumer;

public interface BumpSensor extends Runnable {
    enum State { LEFT_FRONT_CORNER, RIGHT_FRONT_CORNER, LEFT_SIDE_FRONT, RIGHT_SIDE_FRONT,
    LEFT_SIDE_REAR, RIGHT_SIDE_REAR, LEFT_REAR_CORNER, RIGHT_REAR_CORNER;

        boolean match(byte value) {
            byte mask = (byte) (1 << ordinal());
            return (mask & value) == mask;
        }
    };


    /**
     * Registers a consumer that will listen to this bump sensor.
     * @param consumer the consumer of the BumpState.
     */
    void register(Consumer<BumpState> consumer);

    /**
     * Unregisters a conumer from this sensor.
     * @param consumer the consumer to unregister.
     */
    void unregister(Consumer<BumpState> consumer);


    final class BumpState {
        private final byte value;
        public BumpState(byte value) {
            this.value = value;
        }

        public boolean is(State state) {
            return state.match(value);
        }

        public byte getValue() {
            return value;
        }

        public boolean equals(Object other) {
            return other instanceof BumpState && value == ((BumpState) other).value;
        }

        public int hashCode() {
            return (int) value;
        }
    }
}
