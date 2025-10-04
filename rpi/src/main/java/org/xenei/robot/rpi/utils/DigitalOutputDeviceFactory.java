package org.xenei.robot.rpi.utils;

import com.diozero.api.DigitalOutputDevice;

@FunctionalInterface
public interface DigitalOutputDeviceFactory {
    DigitalOutputDevice build(int gpio);
}