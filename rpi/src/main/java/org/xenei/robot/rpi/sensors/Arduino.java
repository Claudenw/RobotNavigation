package org.xenei.robot.rpi.sensors;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
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
    private static final double TIME_TO_M = 5830.9;
    private final I2CDevice device;
    private final byte[] buffer;
    private final ShortBuffer sb;
    
    public Arduino() {
        device = new I2CDevice(CONTROLLER, ADDRESS);
        buffer = new byte[2];
        sb = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
    }

    @Override
    public double maxRange() {
        return 2.0;
    }

    @Override
    public Location[] sense() {
        device.readBytes(buffer);
        // capture parity flag
        boolean parityFlg = (buffer[1] & 0x80) != 0;
        buffer[1] &= ~0x80;
        
        int timing = sb.get(0);
        // check parity flag
        boolean parity = Integer.bitCount(timing) % 2 != 0;
        if (parity == parityFlg)
        {
            System.out.format( "%x %x %d %s%n", buffer[0], buffer[1], timing, timing/TIME_TO_M);
            if (timing > 0) {
                Location c = Location.from( CoordUtils.fromAngle(0, timing/TIME_TO_M));
                return new Location[] { c };
            }
        } else {
            System.out.println( "PARITY ERROR");
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
