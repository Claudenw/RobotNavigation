package org.xenei.robot.common.sensor.bump;

import org.xenei.robot.common.serialization.SerializationException;
import org.xenei.robot.common.utils.RobutContext;
import org.xenei.robot.common.serialization.SerializerDeserializer;
import org.xenei.robot.ml.SensorLayer;

/**
 * Creates a SensorLayer to process the bump sensor changes.  When bump sensor changes the state change is processed
 * through the sensorLayer and the resulting answer placed on the {@link RobutContext#bumpSensorTopic}.
 */
public final class BumpSensorModel {
    private final SensorLayer sensorLayer;
    private final RobutContext.Topic<BumpSensorModel.SensorResult> bumpSensorTopic;

    /**
     * Creates a sensor model comprising a sensor layer with the specified number of neurons and operating in the
     * specified RobutContext.
     * @param ctxt the contest to operate in.
     * @param numNeurons the number of neurons.
     */
    public BumpSensorModel(RobutContext ctxt, int numNeurons) {
        this.bumpSensorTopic = ctxt.bumpSensorTopic;
        this.sensorLayer = new SensorLayer(numNeurons);
        this.sensorLayer.load("bumpSensor.model");
    }

    /**
     * Processes the sensor state.
     * @param sensorMask The bit pattern representing the bump switches that are on.
     */
    public void processSensorState(byte sensorMask) {
        if (sensorMask != 0) {
            bumpSensorTopic.send(new SensorResult(sensorMask, sensorLayer.trigger(sensorMask)));
        }
    }

    /**
     * The result of a bump sensor change.
     * @param bumpState the state of the bump sensors (bit mask)
     * @param answer the original for the Answer from the sensor layer.
     */
    public record SensorResult(byte bumpState, byte answer) {
        public SensorLayer.Answer getAnswer() {
            return SensorLayer.Answer.from(answer);
        }
    }

    /**
     * Serializer / Deserializer for SensorResult data.
     */
    public static class Serde implements SerializerDeserializer<SensorResult> {
        public SensorResult deserialize(byte[] data) throws SerializationException {
            if (data.length < 2) {
                throw new SerializationException("Invalid data length: " + data.length);
            }
            return new SensorResult(data[0], data[1]);
        }

        public byte[] serialize(SensorResult data) {
            return new byte[]{data.bumpState, data.answer};
        }
    }
}
