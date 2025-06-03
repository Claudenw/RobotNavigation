package org.xenei.robot.rpi.mover;

import org.xenei.robot.ml.SensorLayer;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * Reacts to a change in the bump detector ML layer and stops the step monitor.
 */
class BumpDetector implements Consumer<SensorLayer> {
    private SensorLayer sensorLayer;
    private final StepMonitor stepMonitor;
    private final byte lastTrigger;

    /**
     * constructor.
     * @param stepMonitor the step monitor to stop.
     */
    BumpDetector(StepMonitor stepMonitor) {
        this(stepMonitor, (byte)0);
    }

    /**
     * Constructor with existing trigger state.
     * @param stepMonitor the step monitor to stop.
     * @param lastTrigger the existing trigger value.
     */
    BumpDetector(StepMonitor stepMonitor, byte lastTrigger) {
        this.stepMonitor = stepMonitor;
        this.lastTrigger = lastTrigger;
    }

    @Override
    public void accept(SensorLayer sensorLayer) {
        if (sensorLayer.getTrigger() != lastTrigger) {
            this.sensorLayer = sensorLayer;
            stepMonitor.stop();
        }
    }

    /**
     * Returns the sensor layer if a bump changed stopped the stepMonitor.
     * @return the sensor layer if a bump changed stopped the stepMonitor.
     */
    Optional<SensorLayer> getSensorLayer() {
        return Optional.ofNullable(sensorLayer);
    }

}
