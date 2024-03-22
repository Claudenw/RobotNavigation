package org.xenei.robot.common.testUtils;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.Set;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Geometry;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.ScaleInfo;
import org.xenei.robot.common.mapping.Map;
import org.xenei.robot.common.mapping.Obstacle;
import org.xenei.robot.common.planning.Solution;
import org.xenei.robot.common.utils.RobutContext;
import org.xenei.robot.mapper.MapImpl;

public class FakeDistanceSensorTest {
    private FakeDistanceSensor underTest;

    @Test
    @Disabled
    public void map1Test() {
        TestingPositionSupplier positionSupplier = new TestingPositionSupplier(null);
        Map map = new MapImpl(new RobutContext(ScaleInfo.DEFAULT, TestChassisInfo.DEFAULT));
        underTest = new FakeDistanceSensor1(MapLibrary.map1(map), positionSupplier);
        double x = 13.5;
        double y = 15.5;
        int h = 0;

        Set<Obstacle> obstacles = underTest.map().getObstacles();
        Location[] expected = { Location.from(0.5000, 0.0000), Location.from(0.5000, 0.0000),
                Location.from(0.5000, 0.5000), Location.from(0.000, 0.5000), Location.from(0.0000, 0.5000),
                Location.from(0.0000, 0.5000), Location.from(-0.5000, 0.5000), Location.from(-1.000, 0.5000),
                Location.from(-2.5000, 0.5000), Location.from(-2.5000, -0.5000), Location.from(-1.000, -0.5000),
                Location.from(-0.5000, -0.5000), Location.from(-1.5000, -4.5000), Location.from(0.5000, -5.5000),
                Location.from(0.5000, -1.0000), Location.from(0.5000, -0.5000), Location.from(0.5000, 0.0000) };
        positionSupplier.position = Position.from(Location.from(x, y), Math.toRadians(h));

        Location[] actual = underTest.sense();
        CoordinateUtils.assertEquivalent(expected, actual, 0.000001);
        for (Location l : actual) {
            assertCoordinateInObstacles(obstacles, positionSupplier.get().nextPosition(l));
        }

        expected = new Location[] { Location.from(13.5000, 0.0000), Location.from(1.5000, 0.5000),
                Location.from(0.5000, 0.5000), Location.from(0.5000, 1.0000), Location.from(0.5000, 5.5000),
                Location.from(-0.5000, 2.000), Location.from(-0.5000, 0.5000), Location.from(-0.5000, 0.5000),
                Location.from(-0.5000, 0.000), Location.from(-0.5000, 0.0000), Location.from(-0.5000, -0.5000),
                Location.from(-0.5000, -0.5000), Location.from(0.000, -0.5000), Location.from(0.0000, -0.5000),
                Location.from(0.0000, -0.5000), Location.from(0.5000, -0.5000), Location.from(1.5000, -0.5000) };
        positionSupplier.position = Position.from(positionSupplier.get(), Math.PI);

        actual = underTest.sense();
        CoordinateUtils.assertEquivalent(expected, actual, 0.000001);
        for (Location l : actual) {
            assertCoordinateInObstacles(obstacles, positionSupplier.get().nextPosition(l));
        }
    }

    @Test
    public void map2Test() {
        Position position = Position.from(-1, -3);
        Map map = new MapImpl(new RobutContext(ScaleInfo.DEFAULT, TestChassisInfo.DEFAULT));
        underTest = new FakeDistanceSensor1(MapLibrary.map2(map), () -> position);

        Solution solution = new Solution();
        solution.add(position);
        DebugViz debugViz = new DebugViz(1, map, () -> solution, () -> position);
        debugViz.redraw(null);

        Set<Obstacle> obstacles = underTest.map().getObstacles();
        Location[] actual = underTest.sense();
        for (Location l : actual) {
            assertCoordinateInObstacles(obstacles, position.nextPosition(l));
        }
    }

    /**
     * Checks that at least oneof the geometries (obsts) contains the coordinate.
     * 
     * @param obsts the list of geometries.
     * @param c he coorindate to contain.
     */
    void assertCoordinateInObstacles(Collection<Obstacle> obsts, Location actual) {
        boolean found = false;
        Geometry point = underTest.map().getContext().geometryUtils.asPoint(actual);
        for (Obstacle obstacle : obsts) {
            if (obstacle.geom().buffer(underTest.map().getContext().scaleInfo.getHalfResolution()).contains(point)) {
                found = true;
                break;
            }
        }
        assertTrue(found, () -> "Missing coordinate " + actual);
    }

}
