package org.xenei.robot.common.sensor.bump;

public interface BumpSensor extends Runnable {
    /**
     * A mask for each switch.
     */
    enum State {
        LEFT_FRONT_CORNER, RIGHT_FRONT_CORNER, LEFT_SIDE_FRONT, RIGHT_SIDE_FRONT, LEFT_SIDE_REAR, RIGHT_SIDE_REAR, LEFT_REAR_CORNER, RIGHT_REAR_CORNER;

        private byte mask;

        State() {
            mask = (byte) (1 << ordinal());
        }

        public boolean match(byte value) {
            return (mask & value) == mask;
        }

        public byte getMask() {
            return mask;
        }
    }
}
