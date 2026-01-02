package org.xenei.robot.rpi.sensors;

import com.diozero.api.I2CDevice;
import org.xenei.robot.common.sensor.bump.BumpSensor;
import org.xenei.robot.common.sensor.bump.BumpSensorModel;
import org.xenei.robot.common.utils.RobutContext;

import java.util.concurrent.TimeUnit;

/**
 * Implements a bump sensor reading the bump sensor I2Cdevice
 */
public class BumpSensorImpl implements BumpSensor {
    private static final int CONTROLLER = 1;
    private static final int ADDRESS = 0x27;
    private final I2CDevice device;
  private final BumpSensorModel bumpSensorModel;

    /**
     * Constructs a bump sensor that reads the I2Cdefice and sends
     * messages on {@link RobutContext#bumpSensorTopic}.
     * @param ctxt the robut context to work within.
     * @param initialDelay how long, in milliseconds, to delay before first reading.
     * @param delay how long, in milliseconds, to delay between readings.
     */
    public BumpSensorImpl(RobutContext ctxt, long initialDelay, long delay) {
        super();
        device = new I2CDevice(CONTROLLER, ADDRESS);
        bumpSensorModel = new BumpSensorModel(ctxt, 8);
        ctxt.scheduleAtFixedRate(this, initialDelay, delay, TimeUnit.MILLISECONDS);
    }

    @Override
    public void run() {
        byte deviceState = (byte)(0xFF & ~device.readByte());
        if (deviceState != 0) {
            bumpSensorModel.processSensorState(deviceState);
        }
    }
}
