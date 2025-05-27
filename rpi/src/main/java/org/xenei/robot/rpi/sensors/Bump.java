package org.xenei.robot.rpi.sensors;

import com.diozero.api.I2CDevice;

public class Bump {
    private static final int CONTROLLER = 1;
    private static final int ADDRESS = 0x27;
    private final I2CDevice device;

    public Bump() {
        device = new I2CDevice(CONTROLLER, ADDRESS);
    }

    byte read() {
        return device.readByte();
    }

    public static void main(String[] args) {
        Bump bump = new Bump();
        while (true) {
            System.out.format("%x%n", bump.read());
        }
    }
}
