package org.xenei.robot.common.testUtils;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.xenei.robot.common.ChassisInfoTest;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.Obstacle;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.ScaleInfo;
import org.xenei.robot.common.mapping.Map;
import org.xenei.robot.common.mapping.MapObstacle;
import org.xenei.robot.common.mapping.MapTest;
import org.xenei.robot.common.planning.Solution;
import org.xenei.robot.common.utils.RobutContext;
import org.xenei.robot.mapper.MapDistanceSensorAdapter;
import org.xenei.robot.mapper.visualization.MapViz;

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
    public void map1Test() {
        TestingPositionSupplier positionSupplier = new TestingPositionSupplier(null);
        Map map = new Map(
                new RobutContext(ScaleInfo.DEFAULT, ChassisInfoTest.DEFAULT),
                new MapTest.TestingStorage());
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
        map.getContext().bus.distance
                .register(readings -> actual.addAll(readings.readings()));
        underTest.run();
        CoordinateUtils.assertEquivalent(expected, actual, 0.000001);
        for (Location l : actual) {
            assertCoordinateInObstacles(obstacles, positionSupplier.get().nextPosition(l));
        }

        expected = new Location[]{makeLoc(13.5000, 0.0000), makeLoc(1.5000, 0.5000), makeLoc(0.5000, 0.5000),
                makeLoc(0.5000, 1.0000), makeLoc(0.5000, 5.5000), makeLoc(-0.5000, 2.000), makeLoc(-0.5000, 0.5000),
                makeLoc(-0.5000, 0.5000), makeLoc(-0.5000, 0.000), makeLoc(-0.5000, 0.0000), makeLoc(-0.5000, -0.5000),
                makeLoc(-0.5000, -0.5000), makeLoc(0.000, -0.5000), makeLoc(0.0000, -0.5000), makeLoc(0.0000, -0.5000),
                makeLoc(0.5000, -0.5000), makeLoc(1.5000, -0.5000)};
        positionSupplier.position = Position.asPosition(positionSupplier.get().getCoordinate(), Math.PI);

        actual.clear();
        underTest.run();
        CoordinateUtils.assertEquivalent(expected, actual, 0.000001);
        for (Location l : actual) {
            assertCoordinateInObstacles(obstacles, positionSupplier.get().nextPosition(l));
        }
    }

    @Test
    public void map2Test() throws InterruptedException {
        Supplier<Position> positionSupplier = new TestingPositionSupplier(makePosition(-1, -3, 0));
        Solution solution = new Solution();
        Map map = new Map(new RobutContext(ScaleInfo.DEFAULT, ChassisInfoTest.DEFAULT), new MapTest.TestingStorage());
        solution.add(map.asMapPosition(positionSupplier.get()));

        underTest = new FakeDistanceSensor1(MapLibrary.map2(map), positionSupplier);
        MapViz mapViz = new MapViz(1, underTest.map(), () -> solution, positionSupplier, () -> null);
        map.getContext().scheduleAtFixedRate(mapViz::redraw, 0, 500, TimeUnit.MILLISECONDS);
        map.getContext().bus.distance.register(MapDistanceSensorAdapter.create(map));
        underTest.run();

        DebugViz debugViz = new DebugViz(1, map, () -> solution, positionSupplier, () -> null);
        debugViz.redraw();
        Thread.sleep(1000);
        // Set<Obstacle> obstacles = underTest.map().getObstacles().join();
        // underTest.run();
        // for (Location l : actual) {
        // assertCoordinateInObstacles(obstacles, position.nextPosition(l));
        // }
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
