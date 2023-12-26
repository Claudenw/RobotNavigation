package org.xenei.robot.common.testUtils;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.mapping.CoordinateMap;
import org.xenei.robot.mapper.MapImplTest;

public class FakeDistanceSensorTest {

    @Test
    public void map1Test() {
        CoordinateMap map = MapLibrary.map1('#');
        FakeDistanceSensor underTest = new FakeDistanceSensor1(map);
        int x = 13;
        int y = 15;
        int h = 0;

        Location[] expected = { Location.from(1, 0), Location.from(1, 0), Location.from(1, 0), Location.from(0, 1),
                Location.from(0, 1), Location.from(0, 1), Location.from(0, 1), Location.from(-1, 1), Location.from(-3, 1),
                Location.from(-3, -1), Location.from(-1, -1), Location.from(-1, -1), Location.from(-1, -5),
                Location.from(1, -5), Location.from(1, -1), Location.from(1, 0), Location.from(1, 0) };

        Position position = Position.from(Location.from(x, y), Math.toRadians(h));
        underTest.setPosition(position);
        CoordinateUtils.assertEquivalent(expected, underTest.sense(), 0.000001);

        expected = new Location[] { Location.from(-14, 0), Location.from(-1, -1), Location.from(-1, -1),
                Location.from(-1, -1), Location.from(-1, -5), Location.from(1, -2), Location.from(1, -1),
                Location.from(1, 0), Location.from(1, 0), Location.from(1, 0), Location.from(1, 0), Location.from(0, 1),
                Location.from(0, 1), Location.from(0, 1), Location.from(0, 1), Location.from(-1, 1), Location.from(-1, 1) };
        position = Position.from(position, Math.PI);
        underTest.setPosition(position);
        CoordinateUtils.assertEquivalent(expected, underTest.sense(), 0.000001);
    }

    @Test
    public void map2Test() {
        GeometryFactory geometryFactory = new GeometryFactory();
        CoordinateMap map = MapLibrary.map2('#');
        FakeDistanceSensor underTest = new FakeDistanceSensor1(map);
        Position position = Position.from(Location.ORIGIN);
        underTest.setPosition(position);
        List<Polygon> obstacles = map.getObstacles().collect(Collectors.toList());
        for (Location l : underTest.sense()) {
            MapImplTest.assertCoordinateInObstacles(obstacles, l.getCoordinate());
        }
    }

}
