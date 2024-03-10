package org.xenei.robot.rpi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Scanner;
import java.util.function.Supplier;

import org.locationtech.jts.geom.Coordinate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.Processor;
import org.xenei.robot.common.AbortedException;
import org.xenei.robot.common.ChassisInfo;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.Mover;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.ScaleInfo;
import org.xenei.robot.common.utils.CoordUtils;
import org.xenei.robot.common.utils.RobutContext;
import org.xenei.robot.rpi.sensors.Arduino;

public class Robut {

    private final Supplier<Position> positionSupplier;
    private final Processor processor;

    private static final Logger LOG = LoggerFactory.getLogger(Robut.class);

    public Robut(Coordinate origin) throws InterruptedException {
        RobutContext ctxt = new RobutContext(ScaleInfo.DEFAULT, new ChassisInfo(0.24, 8, 60));
        Mover mover = new RpiMover(ctxt, new CompassImpl(), origin);
        positionSupplier = mover::position;
        this.processor = new Processor(ctxt, mover, positionSupplier, new Arduino());
    }

    public void moveTo(Location relativeLocation) throws AbortedException {
        Location nextCoord = positionSupplier.get().nextPosition(relativeLocation);
        processor.moveTo(nextCoord);
    }

    static BufferedReader BUFFER;

    private static void checkContinue() {
        try {
            System.out.print("Continue: ");
            String line = BUFFER.readLine();
            if (line.toLowerCase().startsWith("y")) {
                return;
            }
        } catch (IOException e) {
            throw new RuntimeException("Input error", e);
        }
        throw new RuntimeException("Stop requested");
    }

    public static void main(String[] args) throws Exception {

        BUFFER = new BufferedReader(new InputStreamReader(System.in));

        Robut robut = new Robut(new Coordinate(0, 0));

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
            LOG.debug(String.format("Attempting %s %s\n", angle, range));
            double theta = Math.toRadians(angle);
            Location relativeLocation = Location.from(CoordUtils.fromAngle(theta, range));
            robut.moveTo(relativeLocation);
        }
    }

}
