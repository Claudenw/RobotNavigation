package org.xenei.robot.rpi.mover;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.locationtech.jts.geom.Coordinate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.common.BumpSensor;
import org.xenei.robot.common.ChassisInfo;
import org.xenei.robot.common.Compass;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.Mover;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.ScaleInfo;
import org.xenei.robot.common.utils.AngleUtils;
import org.xenei.robot.common.utils.CoordUtils;
import org.xenei.robot.common.utils.DoubleUtils;
import org.xenei.robot.common.utils.RobutContext;
import org.xenei.robot.ml.SensorLayer;
import org.xenei.robot.rpi.drivers.Motor;
import org.xenei.robot.rpi.drivers.Motor.SteppingStatus;
import org.xenei.robot.rpi.drivers.ULN2003;
import org.xenei.robot.rpi.drivers.ULN2003.Mode;
import org.xenei.robot.rpi.sensors.BumpSensorImpl;

public class RpiMover implements Mover, AutoCloseable {
    private final Motor[] motor = new Motor[2];
    private static final int LEFT = 0;
    private static final int RIGHT = 1;
    private Coordinate coordinates;
    private final Compass compass;
    private final DeadReckoning deadReckoning;
    private final RobutContext ctxt;
    private final BumpSensorModel bumpSensorModel;
    /** Meters traveled in one rotation. */
    private final double rotationalDistance;
    private final int rpm;

    private static final Logger LOG = LoggerFactory.getLogger(RpiMover.class);

    private static ULN2003 right() throws InterruptedException {
        //return new ULN2003(Mode.FULL_STEP, ULN2003.STEPPER_28BYJ48, 15, 18, 23, 24);
       return new ULN2003(Mode.FULL_STEP, ULN2003.STEPPER_28BYJ48, 24 , 23, 18, 15);
    }

    private static ULN2003 left() throws InterruptedException {
        return new ULN2003(Mode.FULL_STEP, ULN2003.STEPPER_28BYJ48, 12,7, 8, 25);
    }

    /**
     * @param ctxt The context for the robut.
     * @param compass the compass implementation to use.
     * @param coords the initial coordinates.
     * @throws InterruptedException on configuration error.
     */
    public RpiMover(RobutContext ctxt, Compass compass, Coordinate coords) throws InterruptedException {
        this(ctxt, compass, coords, left(), right());
    }

    /**
     * @param ctxt The context for the robut.
     * @param compass the compass implementation to use.
     * @param coords the initial coordinates.
     */
    RpiMover(RobutContext ctxt, Compass compass, Coordinate coords, Motor left, Motor right) {
        this.ctxt = ctxt;
        motor[LEFT] = left;
        motor[RIGHT] = right;
        this.deadReckoning = new DeadReckoning();
        this.compass = compass == null ? this.deadReckoning : compass;
        this.coordinates = coords;
        this.rotationalDistance = Math.PI * ctxt.chassisInfo.wheelDiameter / 100; // in meters
        // configure bump sensor
        this.bumpSensorModel = new BumpSensorModel(ctxt);
        // this.r = width/2.0; // in cm
        // meterminute / meterrotation = meterrotation/meter/minute = r/m
        this.rpm = limit((long) Math.ceil(ctxt.chassisInfo.maxSpeed / rotationalDistance), 1, motor[0].getMaxRpm());
        LOG.debug("RpiMover: {}", position());
    }

    public Consumer<BumpSensor.BumpState> getBumpSensorListener() {
        return bumpSensorModel;
    }

    private static Options getOptions() {
        return new Options().addOption(Option.builder("?").desc("This help").hasArg(false).build())
                .addOption(Option.builder("q").desc("Quit").build())
                .addOption(Option.builder("h").type(Double.class).desc("Heading").hasArg().argName("degrees").build())
                .addOption(Option.builder("m").type(Double.class).desc("Move (angle range)").numberOfArgs(2).build())
                .addOption(Option.builder("s").type(Integer.class).desc("Step (left right)").numberOfArgs(2).build())
                .addOption(Option.builder("c").desc("Compass reading").build());
    }

    public static void main(String[] args) {
        try {
            RobutContext ctxt = new RobutContext(ScaleInfo.DEFAULT, new ChassisInfo(0.23, 3.2, 60));
            BumpSensorImpl bumpSensor = new BumpSensorImpl();
            ctxt.scheduleAtFixedRate(bumpSensor, 500, 42, TimeUnit.MILLISECONDS);
            Compass compass = new DeadReckoning();
            try (RpiMover mover = new RpiMover(ctxt, compass, new Coordinate(0, 0))) {
                bumpSensor.addListener(mover.getBumpSensorListener());
                Options options = getOptions();
                BufferedReader bufferReader = new BufferedReader(new InputStreamReader(System.in));
                new HelpFormatter().printHelp(RpiMover.class.getCanonicalName(), getOptions());
                while (true) {
                    System.out.print("Command: ");
                    String line = bufferReader.readLine();
                    System.out.format("Read: %s\n", line);
                    if (line == null || line.isEmpty()) {
                        return;
                    }
                    String[] cmd = Arrays.stream(line.split("\\s")).filter(s -> !s.isBlank()).toArray(String[]::new);
                    if (!cmd[0].startsWith("-")) {
                        cmd[0] = "-" + cmd[0];
                    }
                    CommandLine commandLine = DefaultParser.builder().build().parse(options, cmd);
                    if (commandLine.hasOption("?")) {
                        new HelpFormatter().printHelp(RpiMover.class.getCanonicalName(), getOptions());
                    }
                    if (commandLine.hasOption("h")) {
                        mover.setHeading(Math.toRadians(commandLine.getParsedOptionValue("h")));
                    }
                    if (commandLine.hasOption("m")) {
                        List<Double> values = Arrays.stream(commandLine.getOptionValues("m")).map(Double::parseDouble).toList();
                        double angle = Math.toRadians(values.get(0));
                        double range = values.get(1);
                        Location l = Location.from(CoordUtils.fromAngle(angle, range));
                        System.out.println("Moving to " + l);
                        mover.move(l);
                    }
                    if (commandLine.hasOption("s")) {
                        List<Integer> values = Arrays.stream(commandLine.getOptionValues("s")).map(Integer::parseInt).toList();
                        mover.takeSteps(values.get(0), values.get(1), mover.rpm);
                    }
                    if (commandLine.hasOption("q")) {
                        return;
                    }
                    if (commandLine.hasOption("c")) {
                        System.out.println(compass);
                        double h = mover.compassHeading();
                        System.out.format("Mover[Heading: %s %s degrees]%n", h, Math.toDegrees(h));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            new HelpFormatter().printHelp(RpiMover.class.getCanonicalName(), getOptions());
        }
        LOG.debug("Exiting");
        System.exit(0);
    }

    private int limit(long value, int min, int max) {
        return (value < min) ? min : (value > max) ? max : (int) value;
    }

    @Override
    public void close() {
        try {
            motor[LEFT].close();
        } catch (Exception e) {
            LOG.error("Error closing left motor", e);
        }
        try {
            motor[RIGHT].close();
        } catch (Exception e) {
            LOG.error("Error closing right motor", e);
        }
        LOG.debug("RpiMover shut down complete");
    }

    @Override
    public Position move(Location location) {
        Position currentPosition = position();
        Position nxt = currentPosition.nextPosition(location);
        setHeading(currentPosition.headingTo(nxt));
        int rangeSteps = steps(location.range());
        takeSteps(rangeSteps, rangeSteps, rpm).ifPresent(this::fixBumpSensor);
        return position();
    }

    private void fixBumpSensor(SensorLayer sensorLayer) {
        int rangeSteps = steps(0.01);
        Optional<SensorLayer> nextLayer = Optional.empty();
        while (sensorLayer != null) {
            switch (sensorLayer.getAnswer()) {
                case FF -> {
                    nextLayer = takeSteps(rangeSteps, rangeSteps, rpm, sensorLayer.getTrigger());
                }
                case FS -> {
                    nextLayer = takeSteps(rangeSteps, 0, rpm, sensorLayer.getTrigger());
                }
                case FR -> {
                    nextLayer = takeSteps(rangeSteps, -rangeSteps, rpm, sensorLayer.getTrigger());
                }
                case SF -> {
                    nextLayer = takeSteps(0, rangeSteps, rpm, sensorLayer.getTrigger());
                }
                case SS -> {
                    nextLayer = takeSteps(0, 0, rpm, sensorLayer.getTrigger());
                }
                case SR -> {
                    nextLayer = takeSteps(0, -rangeSteps, rpm, sensorLayer.getTrigger());
                }
                case RF -> {
                    nextLayer = takeSteps(-rangeSteps, rangeSteps, rpm, sensorLayer.getTrigger());
                }
                case RS -> {
                    nextLayer = takeSteps(-rangeSteps, 0, rpm, sensorLayer.getTrigger());
                }
                case RR -> {
                    nextLayer = takeSteps(-rangeSteps, -rangeSteps, rpm, sensorLayer.getTrigger());
                }
                case DONT_CARE -> {
                    LOG.error("Invalid SensorLayer answer: DONT_CARE ");
                    return;
                }
            }
            if (nextLayer.isPresent()) {
                sensorLayer.feedback(nextLayer.get().getTrigger());
            }
            sensorLayer = nextLayer.orElse(null);
        }
    }

    private int steps(double range) {
        long steps = Math.round(motor[LEFT].stepsPerRotation() * range / rotationalDistance);
        return limit(steps, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    /**
     * Starts the motors and returns the StepMonitor protected for testing.
     * 
     * @param left the number of steps to take with the left motor.
     * @param right the number of steps to take with the right motor.
     * @param rpm the speed to travel at.
     * @return the StepMonitor.
     */
    private Optional<SensorLayer> takeSteps(int left, int right, int rpm)  {
        return takeSteps(left, right, rpm, (byte)0);
    }

    /**
     * Recover from a sensor collision detection.
     *
     * @param left the number of steps to take with the left motor.
     * @param right the number of steps to take with the right motor.
     * @param rpm the speed to travel at.
     * @param lastTrigger the initial value of the bump detector.
     * @return the completed step monitor
     */
    private Optional<SensorLayer> takeSteps(int left, int right, int rpm, byte lastTrigger)  {
        LOG.debug("Taking steps {} {} @ {} rpm", left, right, rpm);
        SteppingStatus ssLeft = motor[LEFT].prepareRun(left, rpm);
        SteppingStatus ssRight = motor[RIGHT].prepareRun(right, rpm);
        StepMonitor result = new StepMonitor(ssLeft, ssRight);
        BumpDetector bumpChangeDetector = new BumpDetector(new StepMonitor(ssLeft, ssRight), lastTrigger);
        bumpSensorModel.addListener(bumpChangeDetector);
        try {
            result = ctxt.submit(result).get();
            deadReckoning.track(result);
        }
        catch (InterruptedException | ExecutionException e) {
            LOG.error("Error taking steps", e);
        } finally {
            bumpSensorModel.removeListener(bumpChangeDetector);
            deadReckoning.track(null);
        }
        return bumpChangeDetector.getSensorLayer();
    }

    @Override
    public Position position() {
        return Position.from(coordinates, compass.heading());
    }

    double compassHeading() {
        return compass.heading();
    }

    @Override
    public void setHeading(double heading) {
        double headingDiff = compass.heading() - heading;

        makeInternalHeading(heading);
        double newHeadingDiff = compass.heading() - heading;
        LOG.debug("old diff heading {} - new diff heading {} = {}", headingDiff, newHeadingDiff,
                headingDiff - newHeadingDiff);


        int escape=5;
        while (!DoubleUtils.inRange(Math.abs(newHeadingDiff), compass.accuracy())) {
            if (escape-- == 0) { 
                break;
            }
            LOG.debug("Heading difference: {} accuracy: {}", newHeadingDiff, compass.accuracy());

            // heading / (heading - newheading) = 1 when we are
           /* double ratio = newHeadingDiff / headingDiff;
            LOG.debug("Changing heading factor from {} to {}", this.headingFactor, ratio);
            this.headingFactor = DoubleUtils.round(ratio, compass.decimalPlaces());
            */
            makeInternalHeading(heading);
            headingDiff = newHeadingDiff;
            newHeadingDiff = compass.instantaneousHeading() - heading;
        }
        LOG.debug("Heading {} achieved. {}", heading, compass);
    }

    /**
     * Change our heading to {@code heading}
     * @param heading the heading to achieve.
     */
    private int makeInternalHeading(double heading) {
        // theta r is the distance the wheel has to move to pass through the arc from
        // to make the direction change.
        double theta = AngleUtils.normalize(compass.instantaneousHeading()-heading)*-1;
        int thetaSteps = DeadReckoning.stepsTo(theta);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Setting heading: {} {} degrees sweeping through {} degrees of arc", heading,
                    Math.toDegrees(heading), Math.toDegrees(theta));
        }
        if (thetaSteps == 0) {
            return 0;
        }
        takeSteps(thetaSteps, -thetaSteps, motor[0].getMaxRpm());
        LOG.debug("{}", compass);
        return thetaSteps;
    }

}
