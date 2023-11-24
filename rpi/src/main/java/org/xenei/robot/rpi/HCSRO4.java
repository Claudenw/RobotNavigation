package org.xenei.robot.rpi;

import org.xenei.robot.common.AngleUnits;
import org.xenei.robot.common.Coordinates;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.Sensor;

import com.diozero.api.I2CDevice;

public class HCSRO4 implements Sensor {
    
    private static final int CONTROLLER = 1;
    private static final int ADDRESS = 0x8;
    private final I2CDevice device;

    HCSRO4() {
        device = new I2CDevice(CONTROLLER, ADDRESS);
    }
    @Override
    public double maxRange() {
        return 200;
    }

    @Override
    public Coordinates[] sense(Position position) {
        byte b = device.readByte();
        int dist = 0xFF & b;
        Coordinates c = Coordinates.fromAngle(position.getHeading(AngleUnits.RADIANS), AngleUnits.RADIANS,  dist);
        return new Coordinates[] { c };
    }

    public static void main(String[] args) throws InterruptedException {
        HCSRO4 sensor = new HCSRO4();
        while (true) {
        System.out.println( sensor.sense(null)[0]);
        Thread.sleep(500);
        }
    }
}
