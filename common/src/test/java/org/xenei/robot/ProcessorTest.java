package org.xenei.robot;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

import io.nats.client.Options;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.xenei.robot.common.ChassisInfoTest;
import org.xenei.robot.common.sensor.distance.DistanceSensor;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.Mover;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.ScaleInfo;
import org.xenei.robot.common.mapping.Map;
import org.xenei.robot.common.mapping.MapTest;
import org.xenei.robot.common.testUtils.FakeDistanceSensor1;
import org.xenei.robot.common.testUtils.FakeDistanceSensor2;
import org.xenei.robot.common.testUtils.FakeMover;
import org.xenei.robot.common.testUtils.MapLibrary;
import org.xenei.robot.common.utils.AngleUtils;
import org.xenei.robot.common.utils.RobutContext;
import org.xenei.robot.mapper.visualization.MapViz;

import static org.awaitility.Awaitility.await;

public class ProcessorTest {
    ProcessorTest() {
    }

    private void doTest(RobutContext ctxt, Location finalCoord, FakeMover mover, DistanceSensor sensor) throws InterruptedException {
        Supplier<Position> positionSupplier = mover::position;
        Thread mapVizThread;
        try (MapViz mapViz = new MapViz(ctxt.getConnectionOptions(), ctxt.vizName, ctxt.scaleInfo.getResolution(), 100, ctxt.geometryUtils)) {
            mapVizThread = new Thread(mapViz);
            mapVizThread.start();

            Map map = new Map(ctxt, new MapTest.TestingStorage());
            map.registerDistanceSensors();
            Processor underTest = new Processor(mover, positionSupplier, map);
            map.getContext().enableRemoteVisualization(map, ()->null, underTest.getPositionSupplier(), underTest.getPlanner()::getTarget);
            SegmentTracker segmentTracker = new SegmentTracker(ctxt);
            ctxt.scheduleAtFixedRate(sensor, 0, 250, TimeUnit.MILLISECONDS);
            Thread.sleep(500);
            underTest.moveTo(finalCoord);
            await().atMost(2, TimeUnit.HOURS)
                    .until(() -> ctxt.scaleInfo.compare(ScaleInfo.OP.EQ, finalCoord, positionSupplier.get()));
            Thread.sleep(Duration.ofMinutes(5).toMillis());
        }
    }

    @Test
    void stepTestMap2() throws InterruptedException {
        Coordinate startCoord = new Coordinate(-1, -3);
        Options.Builder optionsBuilder = Options.builder()
                .userInfo("demo", "demo") // Set a user and plain text password
                .connectionName("RobutContext:ProcessorTest");
        RobutContext.Builder builder = RobutContext.builder()
                .setId("ProcessorTest")
                .setOptions(optionsBuilder)
                .setChassisInfo(ChassisInfoTest.DEFAULT);
        try (RobutContext ctxt = builder.build();
             RobutContext fakeSensorContext = builder.build()) {
            FakeMover mover = new FakeMover(ctxt, startCoord);
            Map m = new Map(fakeSensorContext, new MapTest.TestingStorage());
            DistanceSensor sensor = new FakeDistanceSensor1(MapLibrary.map2(m), mover::position);
            Location finalCoord = Location.asLocation(new Coordinate(-1, 1));
            doTest(ctxt, finalCoord, mover, sensor);
        }
    }

    @Disabled
    @Test
    void stepTestMap3() throws InterruptedException {
        Location startCoord = Location.asLocation(new Coordinate(-1, -3));
        RobutContext.Builder builder = RobutContext.builder();
        builder.setOptions(builder.defaultOptions());
        try (RobutContext ctxt = builder.build()) {
            FakeMover mover = new FakeMover(ctxt, startCoord.getCoordinate());
            Map m = new Map(RobutContext.builder().build(), new MapTest.TestingStorage());
            DistanceSensor sensor = new FakeDistanceSensor2(MapLibrary.map3(m), AngleUtils.RADIANS_45, mover::position);
            Location finalCoord = Location.asLocation(new Coordinate(-1, 1));
            doTest(ctxt, finalCoord, mover, sensor);
        }
    }

    @Test
    void stepTestEmptyMap() throws InterruptedException {
        Location startCoord = Location.asLocation(new Coordinate(-1, -3));
        Options.Builder optionsBuilder = Options.builder()
                .userInfo("demo", "demo") // Set a user and plain text password
                .connectionName("RobutContext:ProcessorTest");
        RobutContext.Builder builder = RobutContext.builder()
                .setId("ProcessorTest")
                .setOptions(optionsBuilder)
                .setChassisInfo(ChassisInfoTest.DEFAULT);
        try (RobutContext ctxt = builder.build()) {
            FakeMover mover = new FakeMover(ctxt, startCoord.getCoordinate());
            Map m = new Map(RobutContext.builder().build(), new MapTest.TestingStorage());
            DistanceSensor sensor = new FakeDistanceSensor1(m, mover::position);
            Location finalCoord = Location.asLocation(new Coordinate(-1, 1));
            doTest(ctxt, finalCoord, mover, sensor);
        }
    }

    /**
     * Builds Segments from Mover messages.
     */
    private static class SegmentTracker implements Consumer<Location> {
        private int totalSegments = 0;
        private final RobutContext ctxt;

        SegmentTracker(RobutContext ctxt) {
            ctxt.moveToTopic.listen(this);
            this.ctxt = ctxt;
        }

        @Override
        public void accept(Location p) {
            int maxSegments = 100;
            if (++totalSegments > maxSegments) {
                ctxt.motorStateTopic.send(Mover.MotorState.STOP);
                throw new RuntimeException("Did not find solution in " + maxSegments + " steps");
            }
        }
    }

    // public static void main(String[] args) throws InterruptedException {
    // RobutContext ctxt = new RobutContext(ScaleInfo.DEFAULT,
    // ChassisInfoTest.DEFAULT);
    // Location startCoord = Location.from(-1, -3);
    // FakeMover mover = new FakeMover(ctxt, startCoord.getCoordinate());
    // Map m = new MapImpl(new RobutContext(ScaleInfo.DEFAULT,
    // ChassisInfoTest.DEFAULT));
    // DistanceSensor sensor = new FakeDistanceSensor1(MapLibrary.map2(m),
    // mover::position);
    // Location finalCoord = Location.from(-1, 1);
    // Supplier<Position> positionSupplier = mover::position;
    // MapImpl map = new MapImpl(ctxt);
    // MapLibrary.map2(map);
    // Processor underTest = new Processor(mover, positionSupplier, map);
    // MapViz mapViz = new MapViz(100, underTest.map,
    // underTest.planner::getSolution, positionSupplier, () -> null);
    // ctxt.visualizations.register(mapViz);
    // ctxt.visualizations.redraw();
    //
    // while (true) {
    // Thread.sleep(1000);
    // }
    // }
}
