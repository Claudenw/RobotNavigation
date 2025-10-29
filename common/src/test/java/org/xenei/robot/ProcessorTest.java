package org.xenei.robot;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.common.ChassisInfoTest;
import org.xenei.robot.common.DistanceSensor;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.Mover;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.ScaleInfo;
import org.xenei.robot.common.mapping.Map;
import org.xenei.robot.common.mapping.MapTest;
import org.xenei.robot.common.messages.Bus;
import org.xenei.robot.common.messages.Topic;
import org.xenei.robot.common.testUtils.FakeDistanceSensor1;
import org.xenei.robot.common.testUtils.FakeDistanceSensor2;
import org.xenei.robot.common.testUtils.FakeMover;
import org.xenei.robot.common.testUtils.MapLibrary;
import org.xenei.robot.common.utils.AngleUtils;
import org.xenei.robot.common.utils.RobutContext;
import org.xenei.robot.mapper.MapDistanceSensorAdapter;
import org.xenei.robot.mapper.visualization.MapViz;

import static org.awaitility.Awaitility.await;

public class ProcessorTest {
    private final RobutContext ctxt;

    ProcessorTest() {
        ctxt = new RobutContext(ScaleInfo.DEFAULT, ChassisInfoTest.DEFAULT);
    }


    private static class TopicConsumer<T extends Object> {
        Topic<T> topic;
        Consumer<T> consumer;

        TopicConsumer(Topic<T> topic, Consumer<T> consumer) {
            this.topic = topic;
            this.consumer = consumer;
        }

        public void unregister() {
            topic.unregister(consumer);
        }
    }

    private void doTest(Location startCoord, Location finalCoord, FakeMover mover, DistanceSensor sensor) {
        Supplier<Position> positionSupplier = mover::position;
        Map map = new Map(ctxt, new MapTest.TestingStorage());
        Consumer<DistanceSensor.Readings> adapter = MapDistanceSensorAdapter.create(map);
        Collection<Topic<?>> topics = ctxt.bus.topics();
        List<TopicConsumer<?>> topicConsumerList = new ArrayList<>();
        try {
            ctxt.bus.distance.register(adapter);
            for (Topic<?> topic : topics) {
                Consumer<?> consumer = ((Bus.TopicImpl<?>) topic).register(System.out);
                topicConsumerList.add(new TopicConsumer(topic, consumer));
            }

            Processor underTest = new Processor(mover, positionSupplier, map);
            SegmentTracker segmentTracker = new SegmentTracker(ctxt);
            Consumer<DistanceSensor.Readings> readingConsumer = underTest.getMapper().getRelativeObstacleConsumer();
            MapViz mapViz = new MapViz(100, underTest.map, underTest.planner::getSolution, positionSupplier,
                    () -> null);
            try {
                // wire the mapper to the distance sensor
                ctxt.bus.distance.register(readingConsumer);
                // schedule the sensors to sense
                ctxt.scheduleAtFixedRate(sensor, 500, 250, TimeUnit.MILLISECONDS);
                ctxt.visualizations.register(mapViz);
                ctxt.scheduleAtFixedRate(ctxt.visualizations::redraw, 500, 500, TimeUnit.SECONDS);

                underTest.moveTo(finalCoord);
                await().atMost(2, TimeUnit.HOURS)
                        .until(() -> ctxt.scaleInfo.areEquivalent(finalCoord, positionSupplier.get()));
            } finally {
                ctxt.bus.distance.unregister(readingConsumer);
                ctxt.visualizations.unregister(mapViz);
                ctxt.bus.moveTo.unregister(segmentTracker);
            }
        } finally {
            ctxt.bus.distance.unregister(adapter);
            for (TopicConsumer<?> t : topicConsumerList) {
                t.unregister();
            }
        }
    }

    @Disabled
    @Test
    public void stepTestMap2() {
        Location startCoord = Location.asLocation(new Coordinate(-1, -3));
        FakeMover mover = new FakeMover(ctxt, startCoord.getCoordinate());
        Map m = new Map(new RobutContext(ScaleInfo.DEFAULT, ChassisInfoTest.DEFAULT), new MapTest.TestingStorage());
        DistanceSensor sensor = new FakeDistanceSensor1(MapLibrary.map2(m), mover::position);
        Location finalCoord = Location.asLocation(new Coordinate(-1, 1));
        doTest(startCoord, finalCoord, mover, sensor);
    }

    @Disabled
    @Test
    public void stepTestMap3() {
        Location startCoord = Location.asLocation(new Coordinate(-1, -3));
        FakeMover mover = new FakeMover(ctxt, startCoord.getCoordinate());
        Map m = new Map(new RobutContext(ScaleInfo.DEFAULT, ChassisInfoTest.DEFAULT), new MapTest.TestingStorage());
        DistanceSensor sensor = new FakeDistanceSensor2(MapLibrary.map3(m), AngleUtils.RADIANS_45, mover::position);
        Location finalCoord = Location.asLocation(new Coordinate(-1, 1));
        doTest(startCoord, finalCoord, mover, sensor);
    }

    @Test
    public void stepTestEmptyMap() {
        Location startCoord = Location.asLocation(new Coordinate(-1, -3));
        FakeMover mover = new FakeMover(ctxt, startCoord.getCoordinate());
        Map m = new Map(new RobutContext(ScaleInfo.DEFAULT, ChassisInfoTest.DEFAULT), new MapTest.TestingStorage());
        DistanceSensor sensor = new FakeDistanceSensor1(m, mover::position);
        Location finalCoord = Location.asLocation(new Coordinate(-1, 1));
        doTest(startCoord, finalCoord, mover, sensor);
    }

    private static class SegmentTracker implements Consumer<Mover.MoveTo> {
        private int totalSegments = 0;
        private final RobutContext ctxt;

        SegmentTracker(RobutContext ctxt) {
            ctxt.bus.moveTo.register(this);
            this.ctxt = ctxt;
        }

        @Override
        public void accept(Mover.MoveTo moveTo) {
            int maxSegments = 100;
            if (++totalSegments > maxSegments) {
                ctxt.bus.motor.send(Mover.MotorState.STOP);
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
