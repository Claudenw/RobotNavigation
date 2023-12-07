package org.xenei.robot.rpi.sensors;

import java.util.Arrays;

import org.xenei.robot.common.Location;
import org.xenei.robot.common.DistanceSensor;
import org.xenei.robot.common.utils.CoordUtils;
import org.xenei.robot.common.utils.TimingUtils;

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
    public Location[] sense() {
        byte b = device.readByte();
        int dist = 0xFF & b;
        Location c = new Location( CoordUtils.fromAngle(0, dist));
        return new Location[] { c };
    }

    public static void main(String[] args) {
        Arduino sensor = new Arduino();
        while (true) {
            Arrays.stream(sensor.sense()).forEach(System.out::println);
            TimingUtils.delay(500);
        }
    }
}
