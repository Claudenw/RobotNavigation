package org.xenei.robot.rpi;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
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

    private static final int MAX_RPM = 300;
    private final Motor[] motor = new Motor[2];
    private static final int LEFT = 0;
    private static final int RIGHT = 1;
    private Coordinate coordinates;
    private final Compass compass;
    private final RobutContext ctxt;
    private final ExecutorService executor;
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
    RpiMover(RobutContext ctxt, Compass compass, Coordinate coords) throws InterruptedException {
        this(ctxt, compass, coords,  left(), right());
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
        this.coordinates = coords;
        this.compass = compass;
        this.rotationalDistance = Math.PI * ctxt.chassisInfo.wheelDiameter / 100; // in meters
        this.executor = Executors.newFixedThreadPool(3);
        // this.r = width/2.0; // in cm
        // meterminute / meterrotation = meterrotation/meter/minute = r/m
        this.rpm = limit((long) Math.ceil(ctxt.chassisInfo.maxSpeed / rotationalDistance), 1, MAX_RPM);
        LOG.debug("RpiMover: {}", position());
    }

    private static Options getOptions() {
        return new Options().addOption(Option.builder("?").desc("This help").hasArg(false).build())
                .addOption(Option.builder("q").desc("Quit").build())
                .addOption(Option.builder("h").type(Double.class).desc("Heading").hasArg().argName("degrees").build())
                .addOption(Option.builder("m").type(Double.class).desc("Move (angle range)").numberOfArgs(2).build())
                .addOption(Option.builder("s").type(Integer.class).desc("Step (left right)").numberOfArgs(2).build())
                .addOption(Option.builder("c").desc("Compass reading").build())
                .addOption(Option.builder("t").desc("Training data").hasArg().type(Integer.class).argName("recordCount").build())
                .addOption(Option.builder("x").desc("x-ray compas test").build());

    }

    public static void main(String[] args) {
        try {
            RobutContext ctxt = new RobutContext(ScaleInfo.DEFAULT, new ChassisInfo(0.23, 3.2, 60));
            Compass compass = new DeadReconing();
            try (RpiMover mover = new RpiMover(ctxt, compass, new Coordinate(0, 0))) {
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
                        mover.steps(values.get(0), values.get(1));
                    }
                    if (commandLine.hasOption("q")) {
                        return;
                    }
                    if (commandLine.hasOption("c")) {
                        System.out.println(compass);
                        double h = mover.compassHeading();
                        System.out.format("Mover[Heading: %s %s degrees]%n", h, Math.toDegrees(h));
                    }

                    if (commandLine.hasOption("t")) {
                        int recordCount = commandLine.getParsedOptionValue("t");
                        mover.generateTrainingData(recordCount);
                    }

                    if (commandLine.hasOption("x")) {
                        mover.xrayTest();
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

    private void xrayTest() throws InterruptedException, IOException {

        Future<?> future = executor.submit(() -> {
            double oldDeg = -1;

            while (true) {
                double h = compass.heading();
                double deg = DoubleUtils.round(Math.toDegrees(h), 3);
                if (deg != oldDeg) {
                    System.out.format("Compass[Heading: %s %s degrees]%n", h, deg);
                    oldDeg = deg;
                }
                Thread.sleep(500);
            }
        });
        generateTrainingData(10);
        Thread.sleep(500);
        future.cancel(true);
    }

    private void generateTrainingData(int recordCount) throws IOException, InterruptedException {
        Random random = new Random();
        Path p = Files.createTempFile("testData", ".csv");
        try (FileWriter fos = new FileWriter(p.toFile())) {
            for (int i = 0; i < recordCount; i++) {
                double initialHeading = compass.instantaneousHeading();
                int thetaSteps = i;
                //int thetaSteps = random.nextInt(-3000, 3000);
                if (thetaSteps != 0) {
                    try (StepMonitor monitor = takeSteps(thetaSteps, -thetaSteps, MAX_RPM)) {
                        monitor.waitForComplete();
                    }
                }
                //Thread.sleep(Duration.ofSeconds(3).toMillis());
                double finalHeading = compass.instantaneousHeading();

                String result = String.format("%d,%.2f%n", thetaSteps, finalHeading-initialHeading);
                System.out.print(result);
                fos.append(result);
            }
        }
        System.out.println("Wrote to: "+p);
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

    public void steps(int left, int right) {
        takeSteps(left, right, rpm).waitForComplete();
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
        LOG.debug("Taking steps {} {} @ {} rpm", left, right, rpm);
        SteppingStatus ssLeft = motor[LEFT].prepareRun(left, rpm);
        SteppingStatus ssRight = motor[RIGHT].prepareRun(right, rpm);
        StepMonitor result = new StepMonitor(ssLeft, ssRight);
        if (compass instanceof DeadReconing) {
            ((DeadReconing) compass).track(result);
        }
        return result;
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
        int thetaSteps = DeadReconing.stepsTo(theta);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Setting heading: {} {} degrees sweeping through {} degrees of arc", heading,
                    Math.toDegrees(heading), Math.toDegrees(theta));
        }
        if (thetaSteps == 0) {
            return 0;
        }
        try (StepMonitor monitor = takeSteps(thetaSteps, -thetaSteps, MAX_RPM)) {
            monitor.waitForComplete();
        }
        LOG.debug("{}", compass);
        return thetaSteps;
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
                LOG.error("Error while waiting for steps ");
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

    private static class DeadReconing implements Compass {
        private static final double STEPS_PER_RADIAN = 640.0;
        double heading;
        StepMonitor currentMonitor;

        @Override
        public double heading() {
            return heading;
        }

        @Override
        public double instantaneousHeading() {
            return heading + (currentMonitor.ssLeft.fwdSteps() - currentMonitor.ssRight.fwdSteps()) / STEPS_PER_RADIAN;
        }

        @Override
        public double sd() {
            return 0;
        }

        @Override
        public int decimalPlaces() {
            return 2;
        }

        public void track(StepMonitor stepMonitor) {
            if (currentMonitor == null) {
                int stepDifferential = currentMonitor.ssLeft.fwdSteps() - currentMonitor.ssRight.fwdSteps();
                if (stepDifferential != 0) {
                    heading += stepDifferential / STEPS_PER_RADIAN;
                }
            }
            this.currentMonitor = stepMonitor;
        }

        public static int stepsTo(double theta) {
            return (int) Math.round(theta * STEPS_PER_RADIAN / 2);
        }
    }
}
