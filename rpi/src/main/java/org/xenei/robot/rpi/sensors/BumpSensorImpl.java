package org.xenei.robot.rpi.sensors;

import com.diozero.api.I2CDevice;
import org.xenei.robot.common.BumpSensor;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class BumpSensorImpl implements Runnable, BumpSensor {
    private static final int CONTROLLER = 1;
    private static final int ADDRESS = 0x27;
    private final I2CDevice device;
    private final CopyOnWriteArrayList<Consumer<BumpState>> listeners;

    public BumpSensorImpl() {
        device = new I2CDevice(CONTROLLER, ADDRESS);
        listeners = new CopyOnWriteArrayList<>();
    }

    public void run() {
        BumpState value = new BumpState((byte) (0xFF & ~device.readByte()));
        listeners.forEach(l -> l.accept(value));
    }

    @Override
    public void addListener(Consumer<BumpState> listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(Consumer<BumpState> listener) {
        listeners.remove(listener);
    }

    public static void main(String[] args) {
        BumpSensorImpl bump = new BumpSensorImpl();
        Consumer<BumpState> listener = (b) -> System.out.format("%x%n", b.getValue());
        bump.addListener(listener);
        while (true) {
            bump.run();
        }
    }

}
