package org.xenei.robot.rpi;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import org.locationtech.jts.geom.Coordinate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.Robut;
import org.xenei.robot.common.ChassisInfo;
import org.xenei.robot.common.DistanceSensor;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.ScaleInfo;
import org.xenei.robot.common.utils.CoordUtils;
import org.xenei.robot.common.utils.RobutContext;
import org.xenei.robot.mover.BaseMover;
import org.xenei.robot.mover.BumpSensorLogicModule;
import org.xenei.robot.rpi.drivers.ULN2003;
import org.xenei.robot.rpi.mover.RpiMover;
import org.xenei.robot.rpi.sensors.Arduino;
import org.xenei.robot.rpi.sensors.BumpSensorImpl;

public class RobutBuilder {
    private RobutBuilder() {
    }

    public static ChassisInfo chassisInfo() {
        return ChassisInfo.builder().width(0.24).wheelSize(3.2)
                .motorInfo(ULN2003.STEPPER_28BYJ48)
                .build();
    }

    public static Robut build(Coordinate origin) throws InterruptedException {
        RobutContext ctxt = new RobutContext(ScaleInfo.DEFAULT, chassisInfo());
        BaseMover mover = new RpiMover(ctxt, new CompassImpl(), origin);
        BumpSensorImpl bumpSensor = new BumpSensorImpl(ctxt);
        BumpSensorLogicModule bumpSensorLogicModule = new BumpSensorLogicModule(ctxt, mover);
        DistanceSensor distSensor = new Arduino(ctxt.bus.distance, mover::position);
        try {
            return new Robut(ctxt, distSensor, mover);
        } finally {
            ctxt.scheduleAtFixedRate(bumpSensor, 500, 42, TimeUnit.MILLISECONDS);
        }
    }


    public static void main(String[] args) throws Exception {
        final Logger log = LoggerFactory.getLogger(Robut.class);
        final BufferedReader BUFFER = new BufferedReader(new InputStreamReader(System.in));
        final Robut robut = build(new Coordinate(0, 0));

        while (true) {
            System.out.print("Target (theta, range): ");
            String line = BUFFER.readLine();
            System.out.format("Read: %s\n", line);
            if (line == null || line.length() == 0) {
                return;
            }
            Scanner in = new Scanner(line);
            double angle = in.nextDouble();
            double range = in.nextDouble();
            log.debug(String.format("Attempting %s %s\n", angle, range));
            double theta = Math.toRadians(angle);
            Location relativeLocation = Location.from(CoordUtils.fromAngle(theta, range));
            robut.moveTo(relativeLocation);
        }
    }
}
