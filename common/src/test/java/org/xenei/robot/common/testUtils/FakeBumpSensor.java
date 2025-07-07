package org.xenei.robot.common.testUtils;

import org.xenei.robot.common.BumpSensor;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class FakeBumpSensor implements BumpSensor {
    private byte bumpReading;
    private final CopyOnWriteArrayList<Consumer<BumpSensor.BumpState>> listeners;

    public FakeBumpSensor() {
        listeners = new CopyOnWriteArrayList<>();
        bumpReading = 0;
    }

    public void trigger(byte reading) {
        bumpReading = reading;
    }

    @Override
    public void run() {
        BumpSensor.BumpState value = new BumpSensor.BumpState(bumpReading);
        listeners.forEach(l -> l.accept(value));
    }

    @Override
    public void addListener(Consumer<BumpSensor.BumpState> listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(Consumer<BumpSensor.BumpState> listener) {
        listeners.remove(listener);
    }
}
