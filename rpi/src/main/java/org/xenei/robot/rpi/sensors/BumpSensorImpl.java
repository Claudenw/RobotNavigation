package org.xenei.robot.rpi.sensors;

import com.diozero.api.I2CDevice;
import org.xenei.robot.common.BumpSensor;
import org.xenei.robot.common.Listeners;
import org.xenei.robot.common.utils.RobutContext;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class BumpSensorImpl extends Listeners.ListenersImpl<BumpSensor.BumpState> implements BumpSensor {
    private static final int CONTROLLER = 1;
    private static final int ADDRESS = 0x27;
    private final I2CDevice device;

    public BumpSensorImpl(RobutContext ctxt) {
        super(ctxt);
        device = new I2CDevice(CONTROLLER, ADDRESS);
    }

    public void run() {
        BumpState value = new BumpState((byte) (0xFF & ~device.readByte()));
        trigger(value);

    }
}
