package org.xenei.robot.common.testUtils;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringWriter;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import io.nats.client.Options;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.xenei.robot.common.ChassisInfoTest;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.Obstacle;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.mapping.Map;
import org.xenei.robot.common.mapping.MapCoordinate;
import org.xenei.robot.common.mapping.MapObstacle;
import org.xenei.robot.common.mapping.MapTest;
import org.xenei.robot.common.planning.Solution;
import org.xenei.robot.common.utils.RobutContext;
import org.xenei.robot.mapper.visualization.TextViz;

public class FakeDistanceSensorTest {
    private FakeDistanceSensor underTest;

    private Location makeLoc(double x, double y) {
        return Location.asLocation(new Coordinate(x, y));
    }

    private Position makePosition(double x, double y, double heading) {
        return Position.asPosition(new Coordinate(x, y), heading);
    }

    @Test
    @Disabled
    void map1Test() {
        TestingPositionSupplier positionSupplier = new TestingPositionSupplier(null);
        RobutContext.Builder builder = RobutContext.builder();
        builder.setOptions(builder.defaultOptions());
        try (RobutContext ctxt = builder.build()) {
            Map map = new Map(ctxt, new MapTest.TestingStorage());
            underTest = new FakeDistanceSensor1(MapLibrary.map1(map), positionSupplier);
            double x = 13.5;
            double y = 15.5;
            int h = 0;

            Set<MapObstacle> obstacles = underTest.map().getObstacles().join().collect(Collectors.toSet());
            Location[] expected = {makeLoc(0.5000, 0.0000), makeLoc(0.5000, 0.0000), makeLoc(0.5000, 0.5000),
                    makeLoc(0.0000, 0.5000), makeLoc(0.0000, 0.5000), makeLoc(0.0000, 0.5000), makeLoc(-0.5000, 0.5000),
                    makeLoc(-1.000, 0.5000), makeLoc(-2.5000, 0.5000), makeLoc(-2.5000, -0.5000), makeLoc(-1.000, -0.5000),
                    makeLoc(-0.5000, -0.5000), makeLoc(-1.5000, -4.5000), makeLoc(0.5000, -5.5000),
                    makeLoc(0.5000, -1.0000), makeLoc(0.5000, -0.5000), makeLoc(0.5000, 0.0000)};
            positionSupplier.position = makePosition(x, y, Math.toRadians(h));
            final List<Location> actual = new ArrayList<>();
            ctxt.distanceSensorTopic.listen(readings -> actual.addAll(readings.readings()));
            underTest.run();
            CoordinateUtils.assertEquivalent(expected, actual, 0.000001);
            for (Location l : actual) {
                assertCoordinateInObstacles(obstacles, positionSupplier.get().nextPosition(l));
            }

            expected = new Location[]{
                    makeLoc(13.5000, 0.0000), makeLoc(1.5000, 0.5000), makeLoc(0.5000, 0.5000),
                    makeLoc(0.5000, 1.0000), makeLoc(0.5000, 5.5000), makeLoc(-0.5000, 2.000), makeLoc(-0.5000, 0.5000),
                    makeLoc(-0.5000, 0.5000), makeLoc(-0.5000, 0.000), makeLoc(-0.5000, 0.0000), makeLoc(-0.5000, -0.5000),
                    makeLoc(-0.5000, -0.5000), makeLoc(0.000, -0.5000), makeLoc(0.0000, -0.5000), makeLoc(0.0000, -0.5000),
                    makeLoc(0.5000, -0.5000), makeLoc(1.5000, -0.5000)
            };
            positionSupplier.position = Position.asPosition(positionSupplier.get().

                    getCoordinate(), Math.PI);

            actual.clear();
            underTest.run();
            CoordinateUtils.assertEquivalent(expected, actual, 0.000001);
            for (Location l : actual) {
                assertCoordinateInObstacles(obstacles, positionSupplier.get().nextPosition(l));
            }
        }
    }

    @Test
    void map2Test() throws InterruptedException, IOException {
        Supplier<Position> positionSupplier = new TestingPositionSupplier(makePosition(-1, -3, 0));
        Options.Builder optionsBuilder = Options.builder()
                .userInfo("demo", "demo") // Set a user and plain text password
                .connectionName("RobutContext:Map2Text");
        RobutContext.Builder builder = RobutContext.builder().setId("Map2Test")
                .setOptions(optionsBuilder)
                .setChassisInfo(ChassisInfoTest.DEFAULT);

        Thread vizThread = null;
        StringWriter sw = new StringWriter();
        try (RobutContext ctxt = builder.build();
             TextViz textViz = new TextViz(1, ctxt.getConnectionOptions(), ctxt.vizName, sw)) {
            vizThread = new Thread(textViz);
            vizThread.start();
            Solution solution = new Solution();
            Map map = new Map(ctxt, new MapTest.TestingStorage());
            solution.add(map.asMapPosition(positionSupplier.get()));

            underTest = new FakeDistanceSensor1(MapLibrary.map2(map), positionSupplier);
            ctxt.enableRemoteVisualization(underTest.map(), () -> solution, positionSupplier, () -> null);
            map.registerDistanceSensors();
            underTest.run();
            MapCoordinate mc = map.asMapCoordinate(new Coordinate(1, -3));
            Awaitility.await().atMost(Duration.ofSeconds(1)).untilAsserted(() -> map.isObstacle(mc));
            Awaitility.await().atMost(Duration.ofSeconds(1)).untilAsserted(() -> sw.toString().contains("# # @ # #"));
        }
    }

    /**
     * Checks that at least oneof the geometries (obsts) contains the coordinate.
     *
     * @param obsts
     *            the list of geometries.
     * @param actual
     *            he location to contain.
     */
    void assertCoordinateInObstacles(Collection<? extends Obstacle> obsts, Location actual) {
        boolean found = false;
        Geometry point = underTest.map().getContext().geometryUtils.asPoint(actual);
        for (Obstacle obstacle : obsts) {
            if (obstacle.getGeometry().buffer(underTest.map().getContext().scaleInfo.getResolution() / 2)
                    .contains(point)) {
                found = true;
                break;
            }
        }
        assertTrue(found, () -> "Missing coordinate " + actual);
    }

}
