package org.xenei.robot.rpi.sensors;

import com.diozero.api.I2CDevice;
import org.xenei.robot.common.BumpSensor;
import org.xenei.robot.common.messages.Topic;
import org.xenei.robot.common.utils.RobutContext;

import java.util.function.Consumer;

public class BumpSensorImpl implements BumpSensor {
    private static final int CONTROLLER = 1;
    private static final int ADDRESS = 0x27;
    private final I2CDevice device;
    private final Topic<BumpState> topic;

    public BumpSensorImpl(RobutContext ctxt) {
        super();
        topic = ctxt.bus.new TopicImpl<>();
        device = new I2CDevice(CONTROLLER, ADDRESS);
    }

    @Override
    public void run() {
        BumpState value = new BumpState((byte) (0xFF & ~device.readByte()));
        topic.send(value);
    }

    @Override
    public void register(Consumer<BumpState> consumer) {
        topic.register(consumer);
    }

    @Override
    public void unregister(Consumer<BumpState> consumer) {
        topic.unregister(consumer);
    }
}
