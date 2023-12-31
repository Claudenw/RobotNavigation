package org.xenei.robot.common.testUtils;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.utils.GeometryUtils;

public class FakeDistanceSensorTest {
    private FakeDistanceSensor underTest;

    @Test
    public void map1Test() {
        underTest = new FakeDistanceSensor1(MapLibrary.map1('#'));
        double x = 13.5;
        double y = 15.5;
        int h = 0;

        List<Polygon> obstacles = underTest.map().getObstacles().collect(Collectors.toList());
        Location[] expected = { Location.from(0.5000, 0.0000), Location.from(0.5000, 0.0000),
                Location.from(0.5000, 0.5000), Location.from(0.000, 0.5000), Location.from(0.0000, 0.5000),
                Location.from(0.0000, 0.5000), Location.from(-0.5000, 0.5000), Location.from(-1.000, 0.5000),
                Location.from(-2.5000, 0.5000), Location.from(-2.5000, -0.5000), Location.from(-1.000, -0.5000),
                Location.from(-0.5000, -0.5000), Location.from(-1.5000, -4.5000), Location.from(0.5000, -5.5000),
                Location.from(0.5000, -1.0000), Location.from(0.5000, -0.5000), Location.from(0.5000, 0.0000) };
        Position position = Position.from(Location.from(x, y), Math.toRadians(h));
        underTest.setPosition(position);
        Location[] actual = underTest.sense();
        CoordinateUtils.assertEquivalent(expected, actual, 0.000001);
        for (Location l : actual) {
            assertCoordinateInObstacles(obstacles, position.nextPosition(l));
        }

        expected = new Location[] { Location.from(13.5000, 0.0000), Location.from(1.5000, 0.5000),
                Location.from(0.5000, 0.5000), Location.from(0.5000, 1.0000), Location.from(0.5000, 5.5000),
                Location.from(-0.5000, 2.000), Location.from(-0.5000, 0.5000), Location.from(-0.5000, 0.5000), 
                Location.from(-0.5000, 0.000), Location.from(-0.5000, 0.0000), Location.from(-0.5000, -0.5000), 
                Location.from(-0.5000, -0.5000), Location.from(0.000, -0.5000), Location.from(0.0000, -0.5000),
                Location.from(0.0000, -0.5000), Location.from(0.5000, -0.5000), Location.from(1.5000, -0.5000) };
        position = Position.from(position, Math.PI);
        underTest.setPosition(position);
        actual = underTest.sense();
        CoordinateUtils.assertEquivalent(expected, actual, 0.000001);
        for (Location l : actual) {
            assertCoordinateInObstacles(obstacles, position.nextPosition(l));
        }
    }

    @Test
    public void map2Test() {
        underTest = new FakeDistanceSensor1(MapLibrary.map2('#'));
        Position position = Position.from(0.5, 0.5);
        underTest.setPosition(position);
        List<Polygon> obstacles = underTest.map().getObstacles().collect(Collectors.toList());
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
    void assertCoordinateInObstacles(Collection<? extends Geometry> obsts, Location actual) {
        boolean found = false;
        Geometry point = GeometryUtils.asPoint(actual);
        for (Geometry geom : obsts) {
            if (geom.buffer(underTest.map().scaleInfo().getBuffer()).contains(point)) {
                found = true;
                break;
            }
        }
        assertTrue(found, () -> "Missing coordinate " + actual);
    }

}
