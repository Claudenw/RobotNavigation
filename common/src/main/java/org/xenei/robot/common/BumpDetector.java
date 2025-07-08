package org.xenei.robot.common;

import org.xenei.robot.ml.SensorLayer;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * Reacts to a change in the bump detector ML layer and stops the step monitor.
 */
public class BumpDetector implements Consumer<SensorLayer> {
    private SensorLayer sensorLayer;
    private final Consumer<Mover.MotorState> motorState;
    private final byte lastTrigger;

    /**
     * constructor.
     * @param motorState the consumer of motor state changes
     */
    BumpDetector(Consumer<Mover.MotorState> motorState) {
        this(motorState, (byte)0);
    }

    /**
     * Constructor with existing trigger state.
     * @param motorState the consumer of motor state changes.
     * @param lastTrigger the existing trigger value.
     */
    public BumpDetector(Consumer<Mover.MotorState> motorState, byte lastTrigger) {
        this.motorState = motorState;
        this.lastTrigger = lastTrigger;
    }

    @Override
    public void accept(SensorLayer sensorLayer) {
        if (sensorLayer.getTrigger() != lastTrigger) {
            this.sensorLayer = sensorLayer;
            motorState.accept(Mover.MotorState.PAUSE);
        }
    }

    /**
     * Returns the sensor layer if a bump changed stopped the stepMonitor.
     * @return the sensor layer if a bump changed stopped the stepMonitor.
     */
    public Optional<SensorLayer> getSensorLayer() {
        return Optional.ofNullable(sensorLayer);
    }

}
