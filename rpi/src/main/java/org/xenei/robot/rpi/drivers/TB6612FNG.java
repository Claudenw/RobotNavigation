package org.xenei.robot.rpi.drivers;

import com.diozero.api.I2CDevice;

public class TB6612FNG {
    private static final int DEFAULT_CONTROLLER = 1;
    private static final int DEFAULT_ADDRESS = 0x14;
    private final I2CDevice device;

    private class Codes {

        private Codes() {
        }

        static final byte BRAKE = 0x00;
        static final byte STOP = 0x01;
        static final byte CW = 0x02;
        static final byte CCW = 0x03;
        static final byte STANDBY = 0x04;
        static final byte NOT_STANDBY = 0x05;
        static final byte STEPPER_RUN = 0x06;
        static final byte STEPPER_STOP = 0x07;
        static final byte STEPPER_KEEP_RUN = 0x08;
        static final byte SET_ADDR = 0x11;
    }

    public TB6612FNG() {
        this(DEFAULT_CONTROLLER, DEFAULT_ADDRESS);
    }

    public TB6612FNG(int address) {
        this(DEFAULT_CONTROLLER, address);
    }

    public TB6612FNG(int controller, int address) {
        device = new I2CDevice(controller, address);
    }
    /*
    private void setAddr(int newAddress) {
        if (newAddress == 0x00 || newAddress >= 0x80) {
            return;
        }
        new I2CDevice()
        
        
        
    
        I2Cdev::writeByte(_addr, GROVE_MOTOR_DRIVER_I2C_CMD_SET_ADDR, addr);
        _addr = addr;
        delay(100);
    }
    
    }
    */

    public void standby(boolean state) {
        device.writeWordData(state ? Codes.STANDBY : Codes.NOT_STANDBY, (short) 0);
    }

    private int limit(int value, int min, int max) {
        return (value < min) ? min : (value > max) ? max : value;
    }

    private byte asByte(int value) {
        return (byte) (0xFF & value);
    }

    public class Stepper {

        /**
         * @see https://en.wikipedia.org/wiki/Stepper_motor#/media/File:Drive.png
         */
        public enum Mode {
            FULL_STEP, WAVE_DRIVE, HALF_STEP, MICRO_STEPPING
        };

        public void standby(boolean state) {
            TB6612FNG.this.standby(state);
        }

        /**
         * 
         * Drive a stepper motor.
         * 
         * @param mode 4 driver mode: FULL_STEP,WAVE_DRIVE, HALF_STEP, MICRO_STEPPING,
         * for more information:
         * https://en.wikipedia.org/wiki/Stepper_motor#/media/File:Drive.png
         * @param steps: The number of steps to run, range from -32768 to 32767. When
         * steps = 0, the stepper stops. When steps > 0, the stepper runs clockwise.
         * When steps < 0, the stepper runs anticlockwise.
         * @param rpm: Revolutions per minute, the speed of a stepper, range from 1 to
         * 300. Note that high rpm will lead to step lose, so rpm should not be larger
         * than 150.
         */

        public void run(Mode mode, int steps, int rpm) {
            byte[] buffer = new byte[6];

            if (steps == 0) {
                stop();
            }
            steps = limit(steps, -32768, 32767);
            // clockwise or counter clockwise.
            byte cw = (byte) (steps > 0 ? 1 : 0);

            int ms_per_step = (int) (3000.0 / limit(rpm, 1, 300));

            buffer[0] = asByte(mode.ordinal());
            buffer[1] = cw;
            buffer[2] = asByte(steps);
            buffer[3] = asByte(steps >> 8);
            buffer[4] = asByte(ms_per_step);
            buffer[5] = asByte(ms_per_step >> 8);
            device.writeBlockData(Codes.STEPPER_RUN, buffer);
        }

        /**
         * Stop a stepper motor.
         */
        private void stop() {
            device.writeWordData(Codes.STEPPER_STOP, (short) 0);
        }

        /**
         * Keep a stepper motor running. Keeps moving(direction same as the last move,
         * default to clockwise). :param mode: 4 driver mode: FULL_STEP,WAVE_DRIVE,
         * HALF_STEP, MICRO_STEPPING, for more information:
         * https://en.wikipedia.org/wiki/Stepper_motor#/media/File:Drive.png :param rpm:
         * Revolutions per minute, the speed of a stepper, range from 1 to 300. Note
         * that high rpm will lead to step lose, so rpm should not be larger than 150.
         * :param is_cw: Set the running direction, true for clockwise and false for
         * anti-clockwise. :return: nothing.
         */
        private void keep_run(Mode mode, int rpm, boolean is_cw) {
            byte[] buffer = new byte[4];

            byte cw = (byte) (is_cw ? 5 : 4);

            int ms_per_step = (int) (3000.0 / limit(rpm, 1, 300));

            buffer[0] = asByte(mode.ordinal());
            buffer[1] = cw;
            buffer[2] = asByte(ms_per_step);
            buffer[3] = asByte(ms_per_step >> 8);

            device.writeBlockData(Codes.STEPPER_KEEP_RUN, buffer);
        }
    }

    public class DC {

        private byte channel;

        /**
         * Constructor
         * 
         * @param channel identifies which motor to drive. (either 1 or 0)
         */
        public DC(byte channel) {
            this.channel = asByte(limit(channel, 0, 1));
        }

        public void standby(boolean state) {
            TB6612FNG.this.standby(state);
        }

        /**
         * Output a specific amount of voltage to a specific motor. Voltage is between
         * -255 and 255. The negative voltage means motor will go counter clockwise.
         * 
         * @param speed: Speed from -255 to 255 to run this motor. (8bit voltage). Note
         * that there is always a starting speed(a starting voltage) for motor. If the
         * input voltage is 5V, the starting speed should larger than 100 or smaller
         * than -100.
         * 
         */

        public void run(int speed) {
            byte[] buffer = {

                    channel, asByte(Math.abs(limit(speed, -255, 255))) };

            device.writeBlockData(speed >= 0 ? Codes.CW : Codes.CCW, buffer);
        }

        /**
         * Brake, stop the motor immediately.
         */
        public void halt() {
            device.writeWordData(Codes.BRAKE, channel);
        }

        /**
         * Stop the motor slowly.
         */
        public void stop() {
            device.writeWordData(Codes.STOP, channel);
        }
    }
}
