package org.xenei.robot.rpi.sensors;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.common.DistanceSensor;
import org.xenei.robot.common.Position;
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
    private final CopyOnWriteArrayList<Consumer<DistanceSensor.Readings>> listeners;
    private final Supplier<Position> positionSupplier;

    private static final Logger LOG = LoggerFactory.getLogger(Arduino.class);

    public Arduino(Supplier<Position> positionSupplier) {
        this.positionSupplier = positionSupplier;
        device = new I2CDevice(CONTROLLER, ADDRESS);
        buffer = new byte[2];
        sb = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
        listeners = new CopyOnWriteArrayList<>();
    }

    @Override
    public double maxRange() {
        return 1.0;
    }

    @Override
    public void addListener(Consumer<Readings> listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(Consumer<Readings> listener) {
        listeners.remove(listener);
    }

    public void run() {
        Position position = positionSupplier.get();
        device.readBytes(buffer);
        // capture parity flag
        boolean parityFlg = (buffer[1] & 0x80) != 0;
        buffer[1] &= ~0x80;

        int timing = sb.get(0);
        // check parity flag
        boolean parity = Integer.bitCount(timing) % 2 != 0;
        if (parity == parityFlg) {
            LOG.debug(String.format("DataRead: 0:%x 1:%x %d %s", buffer[0], buffer[1], timing, timing / TIME_TO_M));
            if (timing > 0) {
                double range = timing / TIME_TO_M;
                if (range < maxRange()) {
                    DistanceReading reading = new DistanceReading(0, timing / TIME_TO_M);
                    Readings readings = new Readings(position, List.of(reading));
                    for (Consumer<Readings> listener : listeners) {
                        listener.accept(readings);
                    }
                }
            }
        } else {
            LOG.error("PARITY ERROR");
        }
    }

    public static void main(String[] args) {
        Arduino sensor = new Arduino( () -> Position.from(0, 0));
        sensor.addListener(dr -> dr.readings().forEach(System.out::println));
        while (true) {
            System.out.println("Senseing");
            sensor.run();
            TimingUtils.delay(500);
        }
    }
}
