package org.xenei.robot.rpi.mover;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.locationtech.jts.geom.Coordinate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.common.BaseMover;
import org.xenei.robot.common.BumpDetector;
import org.xenei.robot.common.BumpSensorModel;
import org.xenei.robot.common.ChassisInfo;
import org.xenei.robot.common.Compass;
import org.xenei.robot.common.DeadReckoning;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.Mover;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.ScaleInfo;
import org.xenei.robot.common.utils.CoordUtils;
import org.xenei.robot.common.utils.RobutContext;
import org.xenei.robot.ml.SensorLayer;
import org.xenei.robot.rpi.drivers.Motor;
import org.xenei.robot.rpi.drivers.Motor.SteppingStatus;
import org.xenei.robot.rpi.drivers.ULN2003;
import org.xenei.robot.rpi.drivers.ULN2003.Mode;
import org.xenei.robot.rpi.sensors.BumpSensorImpl;

public class RpiMover extends BaseMover implements Mover, AutoCloseable {
    private final Motor[] motor = new Motor[2];
    private static final int LEFT = 0;
    private static final int RIGHT = 1;
    private Coordinate coordinates;
    private final DeadReckoning deadReckoning;
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
        super(ctxt, compass == null ? new DeadReckoning(ctxt, Position.from(coords)) : compass, new BumpSensorModel(ctxt, 8) );
        motor[LEFT] = left;
        motor[RIGHT] = right;
        this.deadReckoning = compass == null ? (DeadReckoning) this.compass :  new DeadReckoning(ctxt, Position.from(coords));
        this.rotationalDistance = Math.PI * ctxt.chassisInfo.wheelDiameter / 100; // in meters
        // this.r = width/2.0; // in cm
        // meterminute / meterrotation = meterrotation/meter/minute = r/m
        this.rpm = limit((long) Math.ceil(ctxt.chassisInfo.maxSpeed / rotationalDistance), 1, motor[0].getMaxRpm());
        LOG.debug("RpiMover: {}", position());
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
            RobutContext ctxt = new RobutContext(ScaleInfo.DEFAULT, new ChassisInfo(0.23, 3.2, 60,
                    ChassisInfo.metersPerStep(ULN2003.STEPPER_28BYJ48, 3.2)));
            BumpSensorImpl bumpSensor = new BumpSensorImpl();
            ctxt.scheduleAtFixedRate(bumpSensor, 500, 42, TimeUnit.MILLISECONDS);
            try (RpiMover mover = new RpiMover(ctxt, null, new Coordinate(0, 0))) {
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
                        mover.takeSteps(values.get(0), values.get(1), (byte)0);
                    }
                    if (commandLine.hasOption("q")) {
                        return;
                    }
                    if (commandLine.hasOption("c")) {
                        System.out.println(mover.compass);
                        double h = mover.position().getHeading();
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
    protected int steps(double range) {
        long steps = Math.round(motor[LEFT].stepsPerRotation() * range / rotationalDistance);
        return limit(steps, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    @Override
    protected int stepsForArc(double theta) {
        return DeadReckoning.stepsTo(theta);
    }

    @Override
    protected Optional<SensorLayer> takeSteps(int left, int right, byte lastSensor) {
        LOG.debug("Taking steps {} {} @ {} rpm", left, right, rpm);
        SteppingStatus ssLeft = motor[LEFT].prepareRun(left, rpm);
        SteppingStatus ssRight = motor[RIGHT].prepareRun(right, rpm);
        StepMonitor result = new StepMonitor(ssLeft, ssRight);
        BumpDetector bumpChangeDetector = new BumpDetector(result::stop, lastSensor);
        bumpSensorModel.addListener(bumpChangeDetector);
        try {
            deadReckoning.track(result);
            ctxt.submit(result).join();
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
}
