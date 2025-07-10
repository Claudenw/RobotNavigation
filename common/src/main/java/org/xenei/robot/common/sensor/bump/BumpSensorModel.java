package org.xenei.robot.common.sensor.bump;

import org.xenei.robot.common.BumpSensor;
import org.xenei.robot.common.messages.Topic;
import org.xenei.robot.common.utils.RobutContext;
import org.xenei.robot.ml.SensorLayer;

import java.util.function.Consumer;

/**
 * Creates a SensorLayer to process the bump sensor changes.
 */
public final class BumpSensorModel implements Consumer<BumpSensor.BumpState> {
    private final SensorLayer sensorLayer;
    private final Topic<SensorLayer> topic;
    public BumpSensorModel(RobutContext ctxt, int numNeurons) {
        this.topic = ctxt.bus.bump;
        this.sensorLayer = new SensorLayer(numNeurons);
        this.sensorLayer.load("bumpSensor.model");
    }

    @Override
    public void accept(BumpSensor.BumpState state) {
        byte result = sensorLayer.trigger(state.getValue());
        if (result != SensorLayer.DONT_CARE) {
            topic.send(sensorLayer);
        }
    }
}
