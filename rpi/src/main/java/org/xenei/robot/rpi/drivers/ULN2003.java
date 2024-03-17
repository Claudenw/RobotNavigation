package org.xenei.robot.rpi.drivers;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.HelpFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.common.Listener;
import org.xenei.robot.common.ListenerContainer;
import org.xenei.robot.common.ListenerContainerImpl;
import org.xenei.robot.common.planning.Planner;
import org.xenei.robot.rpi.RpiMover;
import org.xenei.robot.rpi.drivers.ULN2003.Mode;
import org.xenei.robot.rpi.utils.DigitalOutputDeviceFactory;

import com.diozero.api.DigitalOutputDevice;

/**
 * Stepper driver for the ULN2003 chip
 */
public class ULN2003 implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(ULN2003.class);
    
    private static DigitalOutputDeviceFactory dodF = i -> new DigitalOutputDevice.Builder(i)
            .setActiveHigh(true).setInitialValue(false).build();

    /**
     * Stride angle for the 28BYJ48 stepper motor.
     */
    public static final double STEPPER_28BYJ48 = 5.625/64;
    
    private final MotorBlock block;
    private SteppingStatus task;
    private final double revMilliPerStepMin;
    public final double stepsPerRotation;
    
    private static Options getOptions() {
        
        String modeOptions = Arrays.stream(Mode.values()).map( m -> m.name() ).collect(Collectors.joining(", "));
        return new Options()
                .addOption(new Option("?", "This help"))
                .addOption(new Option("M", "Excersize MotorBlock"))
                .addOption(Option.builder("s").type(Integer.class).desc("Number of steps").hasArg().required().build())
                .addOption(Option.builder("r").type(Integer.class).desc("RPM").hasArg().build())
                .addOption(Option.builder("g").type(Integer.class).desc("GPIO pins (must be 4 pins)").hasArgs().required().build())
                .addOption(Option.builder("m").converter(s -> Mode.valueOf(s.toUpperCase()))
                        .desc("Mode values: "+modeOptions)
                        .required().hasArg().build())
                .addOption(Option.builder("reverse").build())
                ;
    }
    
    public static void main(String[] args) throws InterruptedException, ParseException {
        try {
            CommandLine commandLine = DefaultParser.builder().build().parse(getOptions(), args);
            if (commandLine.hasOption("?")) {
                new HelpFormatter().printHelp(ULN2003.class.getCanonicalName(), getOptions());
                return;
            }
            int steps = commandLine.getParsedOptionValue("s");
            int rpm = commandLine.getParsedOptionValue("r");
            
            List<Integer> gpin = commandLine.getParsedOptionValues("g");
            Mode mode = commandLine.getParsedOptionValue("m");
            boolean fwd = !commandLine.hasOption("reverse");
            
            if (commandLine.hasOption("M")) {
                MotorBlock block = new MotorBlock(mode, gpin.get(0), gpin.get(1), gpin.get(2), gpin.get(3));
                for (int i=0;i<steps;i++) {
                    block.step(fwd,150);
                }
            } else {
                int direction = fwd ? 1 : -1;
                try(ULN2003 motor = new ULN2003(mode, ULN2003.STEPPER_28BYJ48, gpin.get(0), gpin.get(1), gpin.get(2), gpin.get(3))) {
                    motor.prepareRun(steps*direction, rpm).call();
                    LOG.info("Finished");
                } catch (Exception e) {
                    LOG.error("failed", e);
                }
            }
        } catch (Exception e) {
            new HelpFormatter().printHelp(ULN2003.class.getCanonicalName(), getOptions());
        }
    }

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
     * @throws InterruptedException 
     */
    public ULN2003(Mode mode, double strideAngle, int gpio1, int gpio2, int gpio3, int gpio4) throws InterruptedException {
        block = new MotorBlock(mode, gpio1, gpio2, gpio3, gpio4);
        // rev/steps * milli/min = revmilli/stepsmin
        double revolutionPerStep = strideAngle/360;
        double milliPerMin = TimeUnit.MILLISECONDS.convert(1, TimeUnit.MINUTES);
        revMilliPerStepMin = revolutionPerStep * milliPerMin;
        stepsPerRotation = 360/strideAngle;
        LOG.debug("Created instance {}: {}", this.hashCode(), toString());
    }

    @Override
    public String toString() {
        return new StringBuilder("ULN2003 ").append(hashCode()).append(":\n  " )
        .append( block.toString()).append( String.format("\n  stepsPerRotation: %s", stepsPerRotation)).toString();
    }

    private int limit(int value, int min, int max) {
        return (value < min) ? min : (value > max) ? max : value;
    }

    public boolean active() {
        return task != null && task.isRunning();
    }

    @Override
    public void close() throws Exception {
        block.stop();
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
    public SteppingStatus prepareRun(int steps, int rpm) {
        // revmilli/stepsmin * min/rev = milli/steps (min/rev = 1/rpm)
        long msPerStep = (long) Math.ceil(revMilliPerStepMin / limit(rpm, 1, 300));
        
        SteppingStatus result = new SteppingStatus(steps, msPerStep);
        
        LOG.debug("Preparing task {} steps:{} rpm:{}", this, steps, rpm);

        return result;
    }


    /**
     * Stop a stepper motor.
     */
    public void stop() {
        block.off();
    }
    

    public class SteppingStatus implements Callable<SteppingStatus> {
        private volatile int  count;
        private final int initialCounter;
        private final boolean fwd;
        private final long msPerStep;
        
        SteppingStatus(int steps, long msPerStep) {
            initialCounter = Math.abs(limit(steps, -32768, 32767));
            count = initialCounter;
            fwd = steps >= 0;
            this.msPerStep = msPerStep;
            LOG.debug("SteppingStatus created for %s steps", count);
        }

        @Override
        public SteppingStatus call() throws InterruptedException {
            while (count-- > 0) {
                block.step(fwd, msPerStep);
            }
            LOG.info("SteppingStatus complete.  count:{} initial counter:{}", count, initialCounter);
            return this;
        }
        
        public boolean isRunning() {
            return count > 0;
        }
        
        /**
         * Gets the number of steps taken in a forward direction.
         * @return the number of steps taken, negative for reverse travel.
         */
        public int fwdSteps() {
            return (initialCounter-count) * (fwd ? 1 : -1);
        }
        
        public double fwdRotation() {
            return fwdSteps() / stepsPerRotation; 
        }

        @Override
        public String toString() {
            return String.format( "SteppingStatus %s steps:%s rotation:%s", this.hashCode(), fwdSteps(), fwdRotation());
        }
    }

    /**
     * @see https://en.wikipedia.org/wiki/Stepper_motor#/media/File:Drive.png
     */
    public enum Mode {
        FULL_STEP(2, new byte[] { // cycle 8
                (byte) 0x9, // [1,0,0,1]
                (byte) 0xC, // [1,1,0,0]
                (byte) 0x6, // [0,1,1,0]
                (byte) 0x3, // [0,0,1,1]
        }), WAVE_DRIVE(3, new byte[] { // cycle 12
                (byte) 0x8, // [1,0,0,0]
                (byte) 0x4, // [0,1,0,0],
                (byte) 0x2, // [0,0,1,0]
                (byte) 0x1, // [0,0,0,1]
        }), HALF_STEP(1, new byte[] { // cycle 8
                (byte) 0x9, // [1,0,0,1]
                (byte) 0x8, // [1,0,0,0]
                (byte) 0xC, // [1,1,0,0]
                (byte) 0x4, // [0,1,0,0],
                (byte) 0x6, // [0,1,1,0]
                (byte) 0x2, // [0,0,1,0]
                (byte) 0x3, // [0,0,1,1]
                (byte) 0x1, // [0,0,0,1]
        });

        /** The number of steps for each pulse */
        private int pulseLength; 
        /** the patterns for the motor */
        private byte[] steps;
        

        Mode(int pulseLength, byte[] steps) {
            this.steps = steps;
            this.pulseLength = pulseLength;
        }
        
        int adjustPulse(int currentPulse, boolean fwd) {
            int nextPulse = currentPulse + (fwd ? 1 : -1);
            int pulsesPerCycle = steps.length * pulseLength;
            while (nextPulse < 0) {
                nextPulse += pulsesPerCycle;
            }
            return nextPulse % pulsesPerCycle;
        }
        
        int pattern(int pulse) {
            // number of steps in a complete cycle
            return steps[(pulse / pulseLength) % steps.length];
        }

    };

    static class MotorBlock {
        private DigitalOutputDevice[] gpio;
        private int currentPulse;
        private Mode mode;
        
        private static byte[] map = { 0x8, 0x4, 0x2, 0x1 };

        public MotorBlock(Mode mode, int gpio1, int gpio2, int gpio3, int gpio4) throws InterruptedException {
            this.mode = mode;
            this.currentPulse = -1;
            this.gpio = new DigitalOutputDevice[4];
            this.gpio[0] = dodF.build(gpio1);
            this.gpio[1] = dodF.build(gpio2);
            this.gpio[2] = dodF.build(gpio3);
            this.gpio[3] = dodF.build(gpio4);
            // got to known state.
            step(true, 0);
        }
        
        @Override
        public String toString() {
            return String.format( "Motor Block: %s on pins %s %s %s %s", mode, gpio[0].getGpio(),
                    gpio[1].getGpio(), gpio[2].getGpio(), gpio[3].getGpio());
        }

        public void step(boolean fwd, long delay) throws InterruptedException {
            currentPulse = mode.adjustPulse(currentPulse, fwd);
            int pattern = mode.pattern(currentPulse);
            if (LOG.isDebugEnabled()) {
                LOG.debug("MotorBlock - Stepping {}.  Pattern: {} Pulse: {}", (fwd?"forward":"backward"), pattern, currentPulse);
            }   
            for (int i = 0; i < 4; i++) {
                gpio[i].setOn((map[i] & pattern) == 0);
            }
            TimeUnit.MILLISECONDS.sleep(delay);
        }

        private void setAll(boolean state) {
            for (int i = 0; i < 4; i++) {
                gpio[i].setOn(state);
            }
        }
        public void off() {
            setAll(false);
        }
        
        public void stop() {
            setAll(true);
        }
    }

}
