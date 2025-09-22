package org.xenei.robot.rpi.drivers;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.HelpFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.common.MotorInfo;
import org.xenei.robot.rpi.utils.DigitalOutputDeviceFactory;

import com.diozero.api.DigitalOutputDevice;

/**
 * Stepper driver for the ULN2003 chip
 */
public class ULN2003 implements Motor {

	private static final Logger LOG = LoggerFactory.getLogger(ULN2003.class);

	public static final int MAX_RPM = 75;

	private static DigitalOutputDeviceFactory dodF = i -> new DigitalOutputDevice.Builder(i).setActiveHigh(true)
			.setInitialValue(false).build();

	/**
	 * Stride angle for the 28BYJ48 stepper motor in radians
	 */
	public static final MotorInfo STEPPER_28BYJ48 = new MotorInfo(Math.toRadians(5.625) / 64, 100);

	private final MotorBlock block;
	private final MotorInfo motorInfo;

	private static Options getOptions() {

		String modeOptions = Arrays.stream(Mode.values()).map(Enum::name).collect(Collectors.joining(", "));
		return new Options().addOption(new Option("?", "This help"))
				.addOption(Option.builder("s").type(Integer.class).desc("Number of steps").hasArg().required().build())
				.addOption(Option.builder("r").type(Integer.class).desc("RPM").hasArg().build())
				.addOption(Option.builder("g").type(Integer.class).desc("GPIO pins (must be 4 pins)").hasArgs()
						.required().build())
				.addOption(Option.builder("m").converter(s -> Mode.valueOf(s.toUpperCase()))
						.desc("Mode values: " + modeOptions).required().hasArg().build())
				.addOption(Option.builder("reverse").build());
	}

	public static void main(String[] args) {
		try {
			CommandLine commandLine = DefaultParser.builder().build().parse(getOptions(), args);
			if (commandLine.hasOption("?")) {
				new HelpFormatter().printHelp(ULN2003.class.getCanonicalName(), getOptions());
				return;
			}
			int steps = commandLine.getParsedOptionValue("s");
			int rpm = commandLine.getParsedOptionValue("r", 150);
			List<Integer> gpin = Arrays.stream(commandLine.getOptionValues("g")).map(Integer::parseInt).toList();
			Mode mode = commandLine.getParsedOptionValue("m");
			boolean fwd = !commandLine.hasOption("reverse");
			int direction = fwd ? 1 : -1;
			System.out.format("Running ULN2003...%s%n", gpin);
			try (ULN2003 motor = new ULN2003(mode, ULN2003.STEPPER_28BYJ48, gpin.get(0), gpin.get(1), gpin.get(2),
					gpin.get(3))) {
				SteppingStatus steppingStatus = motor.prepareRun(steps * direction, rpm);
				while (steppingStatus.step()) {
					Thread.sleep(150);
				}
				LOG.info("Finished");
			} catch (Exception e) {
				LOG.error("failed", e);
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
	 * @param mode
	 *            The Mode of operation.
	 * @param motorInfo
	 *            The motor info.
	 * @param gpio1
	 *            the A GPIO pin
	 * @param gpio2
	 *            the B GPIO pin
	 * @param gpio3
	 *            the C GPIO pin
	 * @param gpio4
	 *            the D GPIO pin
	 * @throws InterruptedException
	 */
	public ULN2003(Mode mode, MotorInfo motorInfo, int gpio1, int gpio2, int gpio3, int gpio4)
			throws InterruptedException {
		block = new MotorBlock(mode, gpio1, gpio2, gpio3, gpio4);
		this.motorInfo = motorInfo;
		LOG.debug("Created instance {}: {}", this.hashCode(), toString());
	}

	@Override
	public int getMaxRpm() {
		return MAX_RPM;
	}

	@Override
	public String toString() {
		return "ULN2003 " + hashCode() + ":\n  " + block.toString()
				+ String.format("\n  stepsPerRotation: %s", motorInfo.stepsPerRotation());
	}

	private int limit(int value, int min, int max) {
		return (value < min) ? min : (value > max) ? max : value;
	}

	@Override
	public void close() throws Exception {
		block.stop();
	}

	/**
	 * Drive the stepper motor.
	 * 
	 * @param steps:
	 *            The number of steps to run. When steps = 0, the stepper stops.
	 *            When steps > 0, the stepper runs clockwise. When steps < 0, the
	 *            stepper runs anticlockwise.
	 * @param rpm:
	 *            Revolutions per minute, the speed of a stepper, range from 1 to
	 *            {@code maxStepsPerMinute}. Note that high rpm will lead to step
	 *            loss, so rpm should not be larger than 150.
	 */
	public SteppingStatusImpl prepareRun(int steps, int rpm) {

		int stepsPerMinute = (int) Math.round(Math.max(rpm, 1) * motorInfo.stepsPerRotation());
		stepsPerMinute = limit(stepsPerMinute, (int) motorInfo.stepsPerRotation(), motorInfo.stepsPerMinute());
		// 60000 milliseconds per minute

		long msPerStep = 60000 / stepsPerMinute;
		if (LOG.isDebugEnabled()) {
			LOG.debug("Preparing task {} steps:{} rpm:{}", this, steps,
					stepsPerMinute / (int) motorInfo.stepsPerRotation());
		}
		return new SteppingStatusImpl(steps, msPerStep);
	}

	@Override
	public double stepsPerRotation() {
		return motorInfo.stepsPerRotation();
	}

	/**
	 * Stop a stepper motor.
	 */
	public void stop() {
		block.off();
	}

	public class SteppingStatusImpl implements Motor.SteppingStatus {
		private volatile int count;
		private final int initialCounter;
		private final boolean fwd;
		private final long msPerStep;
		private long stepCompleteTime = 0;

		SteppingStatusImpl(int steps, long msPerStep) {
			initialCounter = Math.abs(steps);// Math.abs(limit(steps, Short.MIN_VALUE, Short.MAX_VALUE));
			count = initialCounter;
			fwd = steps >= 0;
			this.msPerStep = msPerStep;
			LOG.debug("SteppingStatus created for {} steps", count);
		}

		@Override
		public long millisecondsPerStep() {
			return motorInfo.freq();
		}

		@Override
		public boolean step() {
			long now = System.currentTimeMillis();
			if (count > 0) {
				if (now > stepCompleteTime) {
					block.step(fwd);
					stepCompleteTime = now + msPerStep;
				}
				return true;
			}
			return false;
		}

		@Override
		public boolean isComplete() {
			return count <= 0;
		}

		/**
		 * Gets the number of steps taken in a forward direction.
		 * 
		 * @return the number of steps taken, negative for reverse travel.
		 */
		public int fwdSteps() {
			return (initialCounter - count) * (fwd ? 1 : -1);
		}

		public double fwdRotation() {
			return fwdSteps() / motorInfo.stepsPerRotation();
		}

		@Override
		public double stepsPerRotation() {
			return motorInfo.stepsPerRotation();
		}

		@Override
		public String toString() {
			return String.format("SteppingStatus %s steps:%s rotation:%s", this.hashCode(), fwdSteps(), fwdRotation());
		}
	}

	/**
	 * @see <a href=
	 *      'https://en.wikipedia.org/wiki/Stepper_motor#/media/File:Drive.png'>drive
	 *      diagram</a>
	 */
	public enum Mode {
		FULL_STEP(2, new byte[]{ // cycle 8
				(byte) 0x9, // [1,0,0,1]
				(byte) 0xC, // [1,1,0,0]
				(byte) 0x6, // [0,1,1,0]
				(byte) 0x3, // [0,0,1,1]
		}), WAVE_DRIVE(3, new byte[]{ // cycle 12
				(byte) 0x8, // [1,0,0,0]
				(byte) 0x4, // [0,1,0,0],
				(byte) 0x2, // [0,0,1,0]
				(byte) 0x1, // [0,0,0,1]
		}), HALF_STEP(1, new byte[]{ // cycle 8
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
		private final int pulseLength;
		/** the patterns for the motor */
		private final byte[] steps;

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

	}

	static class MotorBlock {
		private final DigitalOutputDevice[] gpio;
		private int currentPulse;
		private final Mode mode;

		private static final byte[] MAP = {0x8, 0x4, 0x2, 0x1};

		public MotorBlock(final Mode mode, final int gpio1, final int gpio2, final int gpio3, final int gpio4)
				throws InterruptedException {
			System.out.format("MotorBlock...%s %s %s %s %s%n", mode, gpio1, gpio2, gpio3, gpio4);
			this.mode = mode;
			this.currentPulse = -1;
			this.gpio = new DigitalOutputDevice[]{
					new DigitalOutputDevice.Builder(gpio1).setActiveHigh(true).setInitialValue(false).build(),
					new DigitalOutputDevice.Builder(gpio2).setActiveHigh(true).setInitialValue(false).build(),
					new DigitalOutputDevice.Builder(gpio3).setActiveHigh(true).setInitialValue(false).build(),
					new DigitalOutputDevice.Builder(gpio4).setActiveHigh(true).setInitialValue(false).build()};
			// got to known state.
			step(true);
		}

		@Override
		public String toString() {
			return String.format("Motor Block: %s on pins %s %s %s %s", mode, gpio[0].getGpio(), gpio[1].getGpio(),
					gpio[2].getGpio(), gpio[3].getGpio());
		}

		/**
		 * Cause the motor to take a single step.
		 * 
		 * @param fwd
		 *            if True step forward else step backward.
		 */
		public void step(boolean fwd) {
			currentPulse = mode.adjustPulse(currentPulse, fwd);
			int pattern = mode.pattern(currentPulse);
			if (LOG.isDebugEnabled()) {
				LOG.debug("MotorBlock - Stepping {}.  Pattern: {} Pulse: {}", (fwd ? "forward" : "backward"), pattern,
						currentPulse);
			}
			for (int i = 0; i < 4; i++) {
				gpio[i].setOn((MAP[i] & pattern) == 0);
			}
		}

		/**
		 * Sets all the stepper magnets on or off. Turning them all on will lock the
		 * motor.
		 * 
		 * @param state
		 *            the state for the stepper magnets.
		 */
		private void setAll(boolean state) {
			for (int i = 0; i < 4; i++) {
				gpio[i].setOn(state);
			}
		}

		/**
		 * Turn the motor off.
		 */
		public void off() {
			setAll(false);
		}

		/**
		 * Lock the motor shaft so that it will not turn.
		 */
		public void stop() {
			setAll(true);
		}
	}
}
