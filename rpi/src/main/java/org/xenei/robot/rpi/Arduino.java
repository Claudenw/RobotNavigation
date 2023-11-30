package org.xenei.robot.rpi;

import org.xenei.robot.common.AngleUnits;
import org.xenei.robot.common.Coordinates;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.DistanceSensor;

import com.diozero.api.I2CDevice;

public class Arduino {
    
    private static final int CONTROLLER = 1;
    private static final int ADDRESS = 0x8;
    private final I2CDevice device;

    Arduino() {
        device = new I2CDevice(CONTROLLER, ADDRESS);
    }

    public double maxRange() {
        return 200;
    }

    public Coordinates[] range() {
        byte b = device.readByte();
        int dist = 0xFF & b;
        Coordinates c = Coordinates.fromAngle(0, AngleUnits.DEGREES,  dist);
        return new Coordinates[] { c };
    }

    public static void main(String[] args) throws InterruptedException {
        Arduino sensor = new Arduino();
        while (true) {
        System.out.println( sensor.range()[0]);
        Thread.sleep(500);
        }
    }
}
