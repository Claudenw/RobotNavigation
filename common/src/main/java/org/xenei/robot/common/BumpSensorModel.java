package org.xenei.robot.common;

import org.xenei.robot.common.utils.RobutContext;
import org.xenei.robot.ml.SensorLayer;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Creates a SensorLayer to process the bump sensor changes.
 */
public final class BumpSensorModel extends Listeners.ListenersImpl<SensorLayer> implements Consumer<BumpSensor.BumpState> {
    private final SensorLayer sensorLayer;

    public BumpSensorModel(RobutContext ctxt, int numNeurons) {
        super(ctxt);
        this.sensorLayer = new SensorLayer(numNeurons);
        this.sensorLayer.load("bumpSensor.model");
    }

    @Override
    public void accept(BumpSensor.BumpState state) {
        byte result = sensorLayer.trigger(state.getValue());
        if (result != SensorLayer.DONT_CARE) {
            trigger(sensorLayer);
        }
    }
}
