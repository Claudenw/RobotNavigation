package org.xenei.robot;

import org.locationtech.jts.geom.Coordinate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.common.ChassisInfoTest;
import org.xenei.robot.common.DistanceSensor;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.ScaleInfo;
import org.xenei.robot.common.mapping.Map;
import org.xenei.robot.common.testUtils.FakeBumpSensor;
import org.xenei.robot.common.testUtils.FakeDistanceSensor1;
import org.xenei.robot.common.testUtils.FakeMover;
import org.xenei.robot.common.testUtils.MapLibrary;
import org.xenei.robot.common.utils.CoordUtils;
import org.xenei.robot.common.utils.RobutContext;
import org.xenei.robot.mapper.MapImpl;
import org.xenei.robot.mapper.visualization.MapViz;
import org.xenei.robot.mover.BumpSensorLogicModule;


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class RobutTest {

    private static Robut build(Coordinate origin) throws InterruptedException {
        RobutContext ctxt = new RobutContext(ScaleInfo.DEFAULT, ChassisInfoTest.DEFAULT);
        FakeMover mover = new FakeMover(ctxt, origin);
        FakeBumpSensor bumpSensor = new FakeBumpSensor();
        BumpSensorLogicModule bumpSensorLogicModule = new BumpSensorLogicModule(ctxt, mover);
        Map sensorMap = new MapImpl(ctxt);
        DistanceSensor distSensor = new FakeDistanceSensor1(MapLibrary.map2(sensorMap), mover::position);

        Robut robut = new Robut(ctxt, distSensor, mover);
        MapViz mapViz = new MapViz(100, robut.visualizationInitializer());
        ctxt.visualizations.register(mapViz);
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
