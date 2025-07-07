package org.xenei.robot.rpi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.locationtech.jts.geom.Coordinate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.Processor;
import org.xenei.robot.Robut;
import org.xenei.robot.common.AbortedException;
import org.xenei.robot.common.ChassisInfo;
import org.xenei.robot.common.DistanceSensor;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.Mover;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.ScaleInfo;
import org.xenei.robot.common.utils.AngleUtils;
import org.xenei.robot.common.utils.CoordUtils;
import org.xenei.robot.common.utils.RobutContext;
import org.xenei.robot.mapper.MapDistanceSensorAdapter;
import org.xenei.robot.mapper.MapImpl;
import org.xenei.robot.mapper.MapBumpSensorAdapter;
import org.xenei.robot.mapper.RelativeLocationDistanceSensorAdapter;
import org.xenei.robot.rpi.drivers.ULN2003;
import org.xenei.robot.rpi.mover.RpiMover;
import org.xenei.robot.rpi.sensors.Arduino;
import org.xenei.robot.rpi.sensors.BumpSensorImpl;

public class RobutBuilder {
    private RobutBuilder() {

    }

    public static Robut build(Coordinate origin) throws InterruptedException {
        RobutContext ctxt = new RobutContext(ScaleInfo.DEFAULT, new ChassisInfo(0.24, 8, 60,
                ChassisInfo.metersPerStep(ULN2003.STEPPER_28BYJ48, 8)));
        BumpSensorImpl bumpSensor = new BumpSensorImpl();
        Mover mover = new RpiMover(ctxt, new CompassImpl(), origin);
        DistanceSensor distSensor = new Arduino();
        return new Robut(ctxt, bumpSensor, distSensor, mover);
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
