package org.xenei.robot.common.testUtils;

import org.xenei.robot.common.sensor.bump.BumpSensor;
import org.xenei.robot.common.utils.RobutContext;


public class FakeBumpSensor implements BumpSensor {
    private byte bumpReading;
    private final RobutContext.ByteTopic rawBumpSensorTopic;

    public FakeBumpSensor(RobutContext ctxt) {
        rawBumpSensorTopic = ctxt.rawBumpSensorTopic;
        bumpReading = 0;
    }

    public void trigger(byte reading) {
        bumpReading = reading;
    }

    @Override
    public void run() {
        rawBumpSensorTopic.send(bumpReading);
    }
}
