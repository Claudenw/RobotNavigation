package org.xenei.robot.rpi.sensors;

import java.util.Arrays;

import org.xenei.robot.common.AngleUnits;
import org.xenei.robot.common.Coordinates;
import org.xenei.robot.common.DistanceSensor;
import org.xenei.robot.common.TimingUtils;

import com.diozero.api.I2CDevice;

public class Arduino implements DistanceSensor {

    private static final int CONTROLLER = 1;
    private static final int ADDRESS = 0x8;
    private final I2CDevice device;

    public Arduino() {
        device = new I2CDevice(CONTROLLER, ADDRESS);
    }

    @Override
    public double maxRange() {
        return 200;
    }

    @Override
    public Coordinates[] sense() {
        byte b = device.readByte();
        int dist = 0xFF & b;
        Coordinates c = Coordinates.fromAngle(0, AngleUnits.DEGREES, dist);
        return new Coordinates[] { c };
    }

    public static void main(String[] args) throws InterruptedException {
        Arduino sensor = new Arduino();
        while (true) {
            Arrays.stream(sensor.sense()).forEach(System.out::println);
            TimingUtils.delay(500);
        }
    }
}
