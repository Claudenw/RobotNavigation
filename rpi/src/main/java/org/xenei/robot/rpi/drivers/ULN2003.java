package org.xenei.robot.rpi.drivers;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.xenei.robot.rpi.utils.DigitalOutputDeviceFactory;

import com.diozero.api.DigitalOutputDevice;

/**
 * Stepper driver for the ULN2003 chip
 */
public class ULN2003 implements AutoCloseable {
    
    private static DigitalOutputDeviceFactory dodF = i -> new DigitalOutputDevice.Builder(i)
            .setActiveHigh(true).setInitialValue(false).build();

    /**
     * Stride angle for the 28BYJ48 stepper motor.
     */
    public static final double STEPPER_28BYJ48 = 5.625/64;
    
    private final MotorBlock block;
    private final ExecutorService executor;
    private SteppingStatus task;
    private final double revMilliPerStepMin;
    private final double stepsPerRotation;

    public static DigitalOutputDeviceFactory setDigitalOutputDeviceFactory(DigitalOutputDeviceFactory factory) {
        DigitalOutputDeviceFactory old = dodF;
        dodF = factory;
        return old;
    }
    
    /**
     * 
     * @param mode The Mode of operation.
     * @param strideAngle number of degrees advanced on one step.
     * @param gpio1 the A GPIO pin
     * @param gpio2 the B GPIO pin
     * @param gpio3 the C GPIO pin
     * @param gpio4 the D GPIO pin
     */
    public ULN2003(Mode mode, double strideAngle, int gpio1, int gpio2, int gpio3, int gpio4) {
        block = new MotorBlock(mode, gpio1, gpio2, gpio3, gpio4);
        executor = Executors.newFixedThreadPool(1);;
        // rev/steps * milli/min = revmilli/stepsmin
        double revolutionPerStep = strideAngle/360;
        double milliPerMin = TimeUnit.MILLISECONDS.convert(1, TimeUnit.MINUTES);
        revMilliPerStepMin = revolutionPerStep * milliPerMin;
        stepsPerRotation = 360/strideAngle;
    }

    private int limit(int value, int min, int max) {
        return (value < min) ? min : (value > max) ? max : value;
    }

    /**
     * Calculates the number of steps taken from the number of rotations.
     * @param rotations the number of rotations taken
     * @return the number of steps taken.
     */
    public long steps(double rotations) {
        return (long) Math.ceil(rotations*stepsPerRotation);
    }
    
    /**
     * Get the number of rotations thus far for this command.
     * @return the number of rotations.
     */
    public double rotations() {
        return stepsTaken()/stepsPerRotation;
    }
    
    /**
     * Gets the number of steps taken thus far for this command.
     * @return the number of steps
     */
    public long stepsTaken() {
        if (task == null) {
            return 0;
        }
        return task.stepsTaken();
    }
    
    public boolean active() {
        return task != null && task.isRunning();
    }
    

    @Override
    public void close() throws Exception {
        executor.shutdownNow();
        block.off();
    }

    /**
     * @see https://en.wikipedia.org/wiki/Stepper_motor#/media/File:Drive.png
     */

    /**
     * Drive the stepper motor.
     * 
     * @param steps: The number of steps to run, range from -32768 to 32767. When
     * steps = 0, the stepper stops. When steps > 0, the stepper runs clockwise.
     * When steps < 0, the stepper runs anticlockwise.
     * @param rpm: Revolutions per minute, the speed of a stepper, range from 1 to
     * 300. Note that high rpm will lead to step loss, so rpm should not be larger
     * than 150.
     */
    public FutureTask<?> run(int steps, int rpm) {

        SteppingStatus status = new SteppingStatus(steps);
        
        // revmilli/stepsmin * min/rev = milli/steps (min/rev = 1/rpm)
        long ms_per_step = (long) Math.ceil(revMilliPerStepMin / limit(rpm, 1, 300));
        System.out.format( "Scheduling task %s revMilliPerStepMin:%s rpm:%s\n", ms_per_step, revMilliPerStepMin, rpm);
        FutureTask<SteppingStatus> result = new FutureTask<SteppingStatus>(status);
        executor.execute(result);
        return result;
    }

    /**
     * Stop a stepper motor.
     */
    private void stop() {
        executor.shutdownNow();
        block.off();
    }
    
    private class SteppingStatus implements Callable<SteppingStatus> {
        private volatile int  count;
        final int initialCounter;
        final boolean fwd;
        
        SteppingStatus(int steps) {
            initialCounter = Math.abs(limit(steps, -32768, 32767));
            count = initialCounter;
            fwd = steps > 0;
        }

        @Override
        public SteppingStatus call() {
            while (count-- >= 0) {
                block.step(fwd);
            }
            return this;
        }
        
        public boolean isRunning() {
            return count > 0;
        }
        
        /**
         * Gets the number of steps taken thus far for the command.
         * @return the number of steps taken.
         */
        public int stepsTaken() {
            return initialCounter-count;
        }
    };

    /**
     * @see https://en.wikipedia.org/wiki/Stepper_motor#/media/File:Drive.png
     */
    public enum Mode {
        FULL_STEP(2, new byte[] { //
                (byte) 0x9, // [1,0,0,1]
                (byte) 0xC, // [1,1,0,0]
                (byte) 0x6, // [0,1,1,0]
                (byte) 0x3, // [0,0,1,1]
        }), WAVE_DRIVE(3, new byte[] { //
                (byte) 0x8, // [1,0,0,0]
                (byte) 0x4, // [0,1,0,0],
                (byte) 0x2, // [0,0,1,0]
                (byte) 0x1, // [0,0,0,1]
        }), HALF_STEP(1, new byte[] { //
                (byte) 0x9, // [1,0,0,1]
                (byte) 0x8, // [1,0,0,0]
                (byte) 0xC, // [1,1,0,0]
                (byte) 0x4, // [0,1,0,0],
                (byte) 0x6, // [0,1,1,0]
                (byte) 0x2, // [0,0,1,0]
                (byte) 0x3, // [0,0,1,1]
                (byte) 0x1, // [0,0,0,1]
        });

        private int offset;
        private byte[] steps;

        Mode(int offset, byte[] steps) {
            this.steps = steps;
            this.offset = offset;
        }

        public int offset() {
            return offset;
        }

        public byte[] steps() {
            return steps;
        }
    };

    class MotorBlock {
        private DigitalOutputDevice[] gpio;
        int step;
        Mode mode;
        
        static byte[] map = { 0x8, 0x4, 0x2, 0x1 };

        public MotorBlock(Mode mode, int gpio1, int gpio2, int gpio3, int gpio4) {
            this.mode = mode;
            this.step = -1;
            this.gpio = new DigitalOutputDevice[4];
            this.gpio[0] = dodF.build(gpio1);
            this.gpio[1] = dodF.build(gpio2);
            this.gpio[2] = dodF.build(gpio3);
            this.gpio[3] = dodF.build(gpio4);
        }

        public void step(boolean fwd) {
            byte[] steps = mode.steps();
            int len = steps.length * mode.offset;
            if (fwd) {
                step = Math.floorMod(step + 1, len);
            } else {
                step--;
                if (step<0) {
                    step = len-1;
                }
            }
            
            int patternNumber = step / mode.offset();
            byte pattern = steps[patternNumber];
            for (int i = 0; i < 4; i++) {
                gpio[i].setOn((map[i] & pattern) == 0);
            }
        }

        public void off() {
            for (int i = 0; i < 4; i++) {
                gpio[i].setOn(false);
            }
        }
    }

}
