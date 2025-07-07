package org.xenei.robot.common;

import org.xenei.robot.common.utils.RobutContext;
import org.xenei.robot.ml.SensorLayer;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Creates a SensorLayer to process the bump sensor changes.
 */
public final class BumpSensorModel implements Consumer<BumpSensor.BumpState> {
    private final SensorLayer sensorLayer;
    private final CopyOnWriteArrayList<Consumer<SensorLayer>> listeners;
    private final RobutContext ctxt;

    public BumpSensorModel(RobutContext ctxt, int numNeurons) {
        this.sensorLayer = new SensorLayer(numNeurons);
        this.sensorLayer.load("bumpSensor.model");
        this.listeners = new CopyOnWriteArrayList<>();
        this.ctxt = ctxt;
    }

    public void addListener(Consumer<SensorLayer> listener) {
        listeners.add(listener);
    }

    public void removeListener(Consumer<SensorLayer> listener) {
        listeners.remove(listener);
    }

    @Override
    public void accept(BumpSensor.BumpState state) {
        byte result = sensorLayer.trigger(state.getValue());
        if (result != SensorLayer.DONT_CARE) {
            listeners.forEach( l -> ctxt.submit(() -> l.accept(sensorLayer)));
        }
    }
}
