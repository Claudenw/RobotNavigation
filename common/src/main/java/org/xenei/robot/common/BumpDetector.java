package org.xenei.robot.common;

import org.xenei.robot.ml.SensorLayer;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * Reacts to a change in the bump detector ML layer and stops the step monitor.
 */
public class BumpDetector implements Consumer<SensorLayer> {
    private SensorLayer sensorLayer;
    private final StopSwitch stopSwitch;
    private final byte lastTrigger;

    /**
     * constructor.
     * @param stopSwitch the stop switch.
     */
    BumpDetector(StopSwitch stopSwitch) {
        this(stopSwitch, (byte)0);
    }

    /**
     * Constructor with existing trigger state.
     * @param stopSwitch the step monitor to stop.
     * @param lastTrigger the existing trigger value.
     */
    public BumpDetector(StopSwitch stopSwitch, byte lastTrigger) {
        this.stopSwitch = stopSwitch;
        this.lastTrigger = lastTrigger;
    }

    @Override
    public void accept(SensorLayer sensorLayer) {
        if (sensorLayer.getTrigger() != lastTrigger) {
            this.sensorLayer = sensorLayer;
            stopSwitch.stop();
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
