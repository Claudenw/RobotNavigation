package org.xenei.robot.rpi.utils;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import org.mockito.Mockito;

import com.diozero.api.DigitalOutputDevice;

public class TestingDigitalOutputDeviceFactory implements DigitalOutputDeviceFactory {

    private  List<Device> deviceList = new ArrayList<>();

    @Override
    public DigitalOutputDevice build(int gpio) {
        Device dev = new Device(gpio);
        deviceList.add(dev);
        return dev.dod;
    }

    public void reset() {
        deviceList.clear();
    }

    public Optional<Device> getDevice(int gpio) {
        return deviceList.stream().filter(d -> d.gpio == gpio).findFirst();
    }
    
    public List<Device> getDevices() {
        return Collections.unmodifiableList(deviceList);
    }

    public interface DeviceListener {
        void gpioStateChanged(int gpio, boolean state);
    }

    public class Device {
        private DigitalOutputDevice dod;
        boolean gpioState;
        int gpio;
        private CopyOnWriteArrayList<DeviceListener> listeners;

        Device(int gpio) {
            this.listeners = new CopyOnWriteArrayList<DeviceListener>();
            this.gpio = gpio;
            System.out.format("setting gpio %s\n", gpio);
            dod = Mockito.mock(DigitalOutputDevice.class);
            when(dod.getGpio()).thenReturn(this.gpio);
            doAnswer(invocation -> {
                boolean state = invocation.getArgument(0);
                if (state != gpioState) {
                    listeners.forEach(l -> l.gpioStateChanged(gpio, state));
                    gpioState = state;
                }
                return null;
            }).when(dod).setOn(anyBoolean());
        }

        public void register(DeviceListener listener) {
            this.listeners.add(listener);
        }
    }

}
