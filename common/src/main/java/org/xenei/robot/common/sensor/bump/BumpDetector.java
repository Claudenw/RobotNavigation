package org.xenei.robot.common.sensor.bump;

import org.xenei.robot.common.Mover;
import org.xenei.robot.common.messages.Topic;
import org.xenei.robot.common.utils.RobutContext;
import org.xenei.robot.ml.SensorLayer;

/**
 * Reacts to a change in the bump detector ML layer and stops the step monitor.
 */
public class BumpDetector implements RobutContext.Processor<SensorLayer> {
    private final byte lastTrigger;
    private final Topic<Mover.MotorState> moveTopic;
    private final Topic<SensorLayer> bumpTopic;
    /**
     * constructor.
     * Automatically registers with the bump topic.
     * Should call {@link #unregister()}
     * @param ctxt Robut context
     */
    BumpDetector(RobutContext ctxt) {
        this(ctxt, (byte)0);
    }

    /**
     * Constructor with existing trigger state.
     * Automatically registers with the bump topic.
     * Should call {@link #unregister()}
     * @param ctxt Robut context
     * @param lastTrigger the existing trigger value.
     */
    public BumpDetector(RobutContext ctxt, byte lastTrigger) {
        this.moveTopic = ctxt.bus.motor;
        this.bumpTopic = ctxt.bus.bump;
        this.lastTrigger = lastTrigger;
        this.bumpTopic.register(this);
    }

    @Override
    public void accept(SensorLayer sensorLayer) {
        if (sensorLayer.getTrigger() != lastTrigger) {
            moveTopic.send(Mover.MotorState.PAUSE);
        }
    }

    /**
     * Unregister this detector from the bump topic.
     */
    public void unregister() {
        this.bumpTopic.unregister(this);
    }
}
