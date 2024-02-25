package org.xenei.robot.rpi.sensors;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;

import org.xenei.robot.common.Location;
import org.xenei.robot.common.DistanceSensor;
import org.xenei.robot.common.utils.CoordUtils;
import org.xenei.robot.common.utils.TimingUtils;

import com.diozero.api.I2CDevice;

public class Arduino implements DistanceSensor {

    private static final int CONTROLLER = 1;
    private static final int ADDRESS = 0x8;
    // 343 m/s convert to 2 * m/um (2 x for time out and back)
    private static final double TIME_TO_M = 58.309;
    private final I2CDevice device;
    private final byte[] buffer;
    private final ShortBuffer sb;
    
    public Arduino() {
        device = new I2CDevice(CONTROLLER, ADDRESS);
        buffer = new byte[2];
        sb = ByteBuffer.wrap(buffer).asShortBuffer();
    }

    @Override
    public double maxRange() {
        return 2.0;
    }

    @Override
    public Location[] sense() {
        device.readBytes(buffer);
        short timing = sb.get(0);
        if (timing > 0) {
            Location c = Location.from( CoordUtils.fromAngle(0, TIME_TO_M / timing));
            return new Location[] { c };
        }
        return new Location[] {};
    }

    public static void main(String[] args) {
        Arduino sensor = new Arduino();
        while (true) {
            Arrays.stream(sensor.sense()).forEach(System.out::println);
            TimingUtils.delay(500);
        }
    }
}
