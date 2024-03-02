package org.xenei.robot.rpi.drivers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xenei.robot.rpi.drivers.ULN2003.Mode;
import org.xenei.robot.rpi.utils.DigitalOutputDeviceFactory;
import org.xenei.robot.rpi.utils.TestingDigitalOutputDeviceFactory;
import org.xenei.robot.rpi.utils.TestingDigitalOutputDeviceFactory.DeviceListener;

public class ULN2003Tests {

    static DigitalOutputDeviceFactory orig;

    static TestingDigitalOutputDeviceFactory factory = new TestingDigitalOutputDeviceFactory();

    @BeforeAll
    public static void setup() {
        orig = ULN2003.setDigitalOutputDeviceFactory(factory);
    }

    @AfterAll
    public static void teardown() {
        ULN2003.setDigitalOutputDeviceFactory(orig);
    }

    @BeforeEach
    public void setupRun() {
        factory.reset();
    }

    @Test
    public void constructorTest() throws InterruptedException {
        ULN2003 motor = new ULN2003(Mode.FULL_STEP, ULN2003.STEPPER_28BYJ48, 1, 2, 3, 4);
        assertEquals(0.0, motor.rotations(), 0.001);
        assertEquals(0.0, motor.stepsTaken(), 0.001);
    }

    @Test
    public void runTest() throws InterruptedException {
        ULN2003 motor = new ULN2003(Mode.FULL_STEP, ULN2003.STEPPER_28BYJ48, 17, 27, 22, 23);
        DeviceListener listener = new DeviceListener() {

            @Override
            public void gpioStateChanged(int gpio, boolean state) {
                System.out.format("GPIO %s %s%n", gpio, state);

            }
        };
        factory.getDevices().forEach(d -> d.register(listener));
        try {
            motor.run(5, 1).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        
//        while (motor.active()) {
//            Thread.yield();
//        }
        System.out.println("Finished");
    }
}
