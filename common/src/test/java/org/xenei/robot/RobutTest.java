package org.xenei.robot;

import org.locationtech.jts.geom.Coordinate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.common.ChassisInfo;
import org.xenei.robot.common.DistanceSensor;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.Mover;
import org.xenei.robot.common.ScaleInfo;
import org.xenei.robot.common.mapping.Map;
import org.xenei.robot.common.testUtils.FakeBumpSensor;
import org.xenei.robot.common.testUtils.FakeDistanceSensor1;
import org.xenei.robot.common.testUtils.FakeMover;
import org.xenei.robot.common.testUtils.MapLibrary;
import org.xenei.robot.common.testUtils.TestChassisInfo;
import org.xenei.robot.common.utils.CoordUtils;
import org.xenei.robot.common.utils.RobutContext;
import org.xenei.robot.mapper.MapImpl;
import org.xenei.robot.mapper.visualization.MapViz;


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class RobutTest {

    private static Robut build(Coordinate origin) throws InterruptedException {
        ChassisInfo.Builder builder = new ChassisInfo.Builder()
                .width(0.24).wheelSize(8).stepAngle(0.1).motorFreq(100);
        RobutContext ctxt = new RobutContext(ScaleInfo.DEFAULT, builder.build());
        FakeBumpSensor bumpSensor = new FakeBumpSensor();
        Mover mover = new FakeMover(ctxt, origin);
        Map sensorMap = new MapImpl(new RobutContext(ScaleInfo.DEFAULT, TestChassisInfo.DEFAULT));
        DistanceSensor distSensor = new FakeDistanceSensor1(MapLibrary.map2(sensorMap), mover::position);

        Robut robut = new Robut(ctxt, bumpSensor, distSensor, mover);
        MapViz mapViz = new MapViz(100, robut.visualizationInitializer());
        ctxt.scheduleAtFixedRate(mapViz::redraw, 500, 250, TimeUnit.MILLISECONDS);
        return robut;
    }

    public static void main(String[] args) throws Exception {
        final Logger LOG = LoggerFactory.getLogger(RobutTest.class);
        BufferedReader BUFFER = new BufferedReader(new InputStreamReader(System.in));

        Robut robut = build(new Coordinate(0, 0));

        while (true) {
            System.out.print("Target (theta, range): ");
            String line = BUFFER.readLine();
            System.out.format("Read: %s\n", line);
            if (line == null || line.isEmpty()) {
                return;
            }
            Scanner in = new Scanner(line);
            double angle = in.nextDouble();
            double range = in.nextDouble();
            LOG.debug("Attempting {} {}", angle, range);
            double theta = Math.toRadians(angle);
            Location relativeLocation = Location.from(CoordUtils.fromAngle(theta, range));
            robut.moveTo(relativeLocation);
        }
    }

}
