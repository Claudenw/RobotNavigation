package org.xenei.robot.rpi.sensors;

import com.diozero.api.I2CDevice;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class BumpSensor implements Runnable {
    private static final int CONTROLLER = 1;
    private static final int ADDRESS = 0x27;
    private final I2CDevice device;
    private final CopyOnWriteArrayList<Consumer<Byte>> listeners;

    public BumpSensor() {
        device = new I2CDevice(CONTROLLER, ADDRESS);
        listeners = new CopyOnWriteArrayList<>();
    }

    public void run() {
        byte value = (byte) (0xFF & ~device.readByte());
        listeners.forEach(l -> l.accept(value));
    }

    public void addListener(Consumer<Byte> listener) {
        listeners.add(listener);
    }

    public void removeListener(Consumer<Byte> listener) {
        listeners.remove(listener);
    }

    public static void main(String[] args) {
        BumpSensor bump = new BumpSensor();
        Consumer<Byte> listener = (b) -> System.out.format("%x%n", b);
        bump.addListener(listener);
        while (true) {
            bump.run();
        }
    }

}
