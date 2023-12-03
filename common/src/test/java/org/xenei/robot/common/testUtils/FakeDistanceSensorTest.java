package org.xenei.robot.common.testUtils;

import static org.junit.Assert.assertArrayEquals;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.RETURNS_DEFAULTS;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.mapping.CoordinateMap;
import org.xenei.robot.mapper.MapImplTest;

public class FakeDistanceSensorTest {

    @Test
    public void map1Test() {
        CoordinateMap map = MapLibrary.map1('#');
        FakeDistanceSensor underTest = new FakeDistanceSensor(map);
        int x = 13;
        int y = 15;
        int h = 0;

        Location[] expected = { new Location(1.000000, 0.000000), new Location(0.932472, 0.361242),
                new Location(0.739009, 0.673696), new Location(0.445738, 0.895163),
                new Location(0.092268, 0.995734), new Location(-0.273663, 0.961826),
                new Location(-0.602635, 0.798017), new Location(-0.850217, 0.526432),
                new Location(-2.948919, 0.551249), new Location(-2.948919, -0.551249),
                new Location(-0.850217, -0.526432), new Location(-0.602635, -0.798017),
                new Location(-1.368315, -4.809128), new Location(0.553610, -5.974405),
                new Location(0.891477, -1.790327), new Location(0.739009, -0.673696),
                new Location(0.932472, -0.361242) };

        Position position = new Position(new Location(x, y), Math.toRadians(h));
        underTest.setPosition(position);
        CoordinateUtils.assertEquivalent(expected, underTest.sense(), 0.000001);

        expected = new Location[] { new Location(-14.000000, 0.000000),
                new Location(-1.864944, -0.722483), new Location(-0.739009, -0.673696),
                new Location(-2.674430, -5.370980), new Location(-0.738147, -7.965873),
                new Location(0.547326, -1.923651), new Location(0.602635, -0.798017),
                new Location(0.850217, -0.526432), new Location(0.982973, -0.183750),
                new Location(0.982973, 0.183750), new Location(0.850217, 0.526432),
                new Location(0.602635, 0.798017), new Location(0.273663, 0.961826),
                new Location(-0.092268, 0.995734), new Location(-0.445738, 0.895163),
                new Location(-0.739009, 0.673696), new Location(-1.864944, 0.722483) };
        position.setHeading(Math.PI);
        underTest.setPosition(position);
        CoordinateUtils.assertEquivalent(expected, underTest.sense(), 0.000001);
    }
    
    @Test
    public void map2Test() {
        GeometryFactory geometryFactory = new GeometryFactory();
        CoordinateMap map = MapLibrary.map2('#');
        FakeDistanceSensor underTest = new FakeDistanceSensor(map);
        Position position = new Position();
        underTest.setPosition(position);
        List<Polygon> obstacles = map.getObstacles().collect(Collectors.toList());  
        for (Location l : underTest.sense() ) {
            boolean found = false;
            Point p = geometryFactory.createPoint(l.getCoordinate());
            for (Polygon poly : obstacles) {
                if (poly.contains(p)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                fail(String.format( "%s not found", l));
            }
        }
    }
    
}
