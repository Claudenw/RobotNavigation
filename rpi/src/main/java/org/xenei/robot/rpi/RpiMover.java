package org.xenei.robot.rpi;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.locationtech.jts.geom.Coordinate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.xenei.robot.rpi.drivers.Motor;
import org.xenei.robot.rpi.drivers.Motor.SteppingStatus;
import org.xenei.robot.rpi.drivers.ULN2003;
import org.xenei.robot.rpi.drivers.ULN2003.Mode;

public class RpiMover implements Mover, AutoCloseable {

    private static final int MAX_RPM = 150;
    private Motor[] motor = new Motor[2];
    private static final int LEFT = 0;
    private static final int RIGHT = 1;
    private Coordinate coordinates;
    private Compass compass;
    private final RobutContext ctxt;
    private final ExecutorService executor;
    /** Meters traveled in one rotation. */
    private final double rotationalDistance;
    private final int rpm;
    /**
     * what factor should be used to convert angle to rotations -- initially chassis
     * radius.
     */
    private double headingFactor;
    private double turningFactor;

    private static final Logger LOG = LoggerFactory.getLogger(RpiMover.class);

    /**
     * @param ctxt The context for the robut.
     * @param compass the compass implementation to use.
     * @param coords the initial coordinates.
     * @param width The width of the mover in CM.
     * @param wheelDiameter the wheel diameter in CM.
     * @param maxSpeed meters / minute (min 1, max 150).
     * @throws InterruptedException
     */
    RpiMover(RobutContext ctxt, Compass compass, Coordinate coords) throws InterruptedException {
        this(ctxt, compass, coords, new ULN2003(Mode.FULL_STEP, ULN2003.STEPPER_28BYJ48, 17, 27, 22, 23),
                new ULN2003(Mode.FULL_STEP, ULN2003.STEPPER_28BYJ48, 6, 24, 25, 26));
    }

    /**
     * @param ctxt The context for the robut.
     * @param compass the compass implementation to use.
     * @param coords the initial coordinates.
     * @param width The width of the mover in CM.
     * @param wheelDiameter the wheel diameter in CM.
     * @param maxSpeed meters / minute (min 1, max 150).
     * @throws InterruptedException
     */
    RpiMover(RobutContext ctxt, Compass compass, Coordinate coords, Motor left, Motor right)
            throws InterruptedException {
        this.ctxt = ctxt;
        motor[LEFT] = left;
        motor[RIGHT] = right;
        this.headingFactor = 1.0;
        this.coordinates = coords;
        this.compass = compass;
        this.rotationalDistance = Math.PI * ctxt.chassisInfo.wheelDiameter / 100; // in meters
        this.executor = Executors.newFixedThreadPool(3);
        // this.r = width/2.0; // in cm
        // meterminute / meterrotation = meterrotation/meter/minute = r/m
        this.rpm = limit((long) Math.ceil(ctxt.chassisInfo.maxSpeed / rotationalDistance), 1, MAX_RPM);
        this.turningFactor = ctxt.chassisInfo.radius * AngleUtils.PI_x_2;
        LOG.debug("RpiMover: {}", position());
    }

    private static Options getOptions() {
        return new Options().addOption(new Option("?", "This help")).addOption(new Option("q", "Quit"))
                .addOption(Option.builder("h").type(Double.class).desc("Heading").hasArg().build())
                .addOption(Option.builder("m").type(Double.class).desc("Move (angle range)").hasArgs().build())
                .addOption(new Option("c", "Compass reading"));
    }

    public static void main(String[] args) {
        try {
            RobutContext ctxt = new RobutContext(ScaleInfo.DEFAULT, new ChassisInfo(0.22, 8, 60));
            Compass compass = new CompassImpl();
            try (RpiMover mover = new RpiMover(ctxt, compass, new Coordinate(0, 0))) {

                BufferedReader bufferReader = new BufferedReader(new InputStreamReader(System.in));
                new HelpFormatter().printHelp(RpiMover.class.getCanonicalName(), getOptions());
                while (true) {
                    System.out.print("Command: ");
                    String line = bufferReader.readLine();
                    System.out.format("Read: %s\n", line);
                    if (line == null || line.length() == 0) {
                        return;
                    }
                    CommandLine commandLine = DefaultParser.builder().build().parse(getOptions(), line.split("\s"));
                    if (commandLine.hasOption("?")) {
                        new HelpFormatter().printHelp(RpiMover.class.getCanonicalName(), getOptions());
                    }

                    if (commandLine.hasOption("h")) {
                        mover.setHeading(Math.toRadians(commandLine.getParsedOptionValue("h")));
                    }
                    if (commandLine.hasOption("m")) {
                        List<Object> values = commandLine.getParsedOptionValues("m");
                        double angle = Math.toRadians(((Double) values.get(0)).doubleValue());
                        double range = ((Double) values.get(1)).doubleValue();
                        Location l = Location.from(CoordUtils.fromAngle(angle, range));
                        mover.move(l);
                    }
                    if (commandLine.hasOption("q")) {
                        return;
                    }
                    if (commandLine.hasOption("c")) {
                        System.out.println(compass);
                        double h = mover.compassHeading();
                        System.out.println(String.format("Mover[Heading: %s %s degrees hf:%s]", h, Math.toDegrees(h),
                                mover.headingFactor));
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

    public double getHeadingFactor() {
        return headingFactor;
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
        takeSteps(rangeSteps, rangeSteps, rpm).waitForComplete();
        return position();
    }

    private int steps(double range) {
        long steps = Math.round(motor[LEFT].stepsPerRotation() * range / rotationalDistance);
        return limit(steps, Short.MIN_VALUE, Short.MAX_VALUE);
    }

    /**
     * Starts the motors and returns the StepMonitor protected for testing.
     * 
     * @param left the number of steps to take with the left motor.
     * @param right the number of steps to take with the right motor.
     * @param rpm
     * @return
     */
    private StepMonitor takeSteps(int left, int right, int rpm) {
        LOG.debug(String.format("Taking steps %s %s @ %s rpm", left, right, rpm));
        SteppingStatus ssLeft = motor[LEFT].prepareRun(left, rpm);
        SteppingStatus ssRight = motor[RIGHT].prepareRun(right, rpm);
        return new StepMonitor(ssLeft, ssRight);
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

    private void makeInternalHeading(double heading) {
        // theta r is the distance the wheel has to move to pass through the arc from
        // to make the direction change.
        double theta = AngleUtils.normalize(compass.instantaneousHeading()-heading)*-1;
        double range = turningFactor * theta / AngleUtils.PI_x_2;
        int thetaSteps = steps(range * this.headingFactor);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Setting heading: {} {} degrees sweeping through {} degrees of arc", heading,
                    Math.toDegrees(heading), Math.toDegrees(theta));
        }
        if (thetaSteps == 0) {
            return;
        }
        try (StepMonitor monitor = takeSteps(thetaSteps, -thetaSteps, MAX_RPM)) {
            while (!monitor.complete()) {
                double d = compass.instantaneousHeading() - heading;
                if (DoubleUtils.inRange(d, compass.accuracy())) {
                    LOG.debug("In range so stopping: {} <= {}", d, compass.accuracy());
                    monitor.stop();
                }
            }
        }
        LOG.debug("{}", compass);
    }

    public class StepMonitor implements AutoCloseable {

        SteppingStatus ssLeft;
        SteppingStatus ssRight;

        Future<SteppingStatus> leftFuture;
        Future<SteppingStatus> rightFuture;

        StepMonitor(SteppingStatus ssLeft, SteppingStatus ssRight) {
            this.ssLeft = ssLeft;
            this.ssRight = ssRight;
            leftFuture = executor.submit(ssLeft);
            rightFuture = executor.submit(ssRight);
        }

        public boolean complete() {
            return leftFuture.isDone() && rightFuture.isDone();
        }

        public void stop() {
            if (!leftFuture.isDone()) {
                leftFuture.cancel(true);
            }
            if (!rightFuture.isDone()) {
                rightFuture.cancel(true);
            }
        }

        public void waitForComplete() {
            try {
                leftFuture.get();
                rightFuture.get();
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("Error while whating for steps ");
                stop();
            }
        }

        @Override
        public void close() {
            stop();
            double range = rotationalDistance * (ssLeft.fwdRotation() + ssRight.fwdRotation());
            Coordinate shift = ctxt.scaleInfo.round(CoordUtils.fromAngle(compass.heading(), range));
            coordinates = CoordUtils.add(coordinates, shift);
            LOG.debug("steps result: range:{} shift:{} position:{}", range, shift, position());
        }
    }
}
