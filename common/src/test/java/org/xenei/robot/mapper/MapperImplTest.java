package org.xenei.robot.mapper;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Geometry;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.ScaleInfo;
import org.xenei.robot.common.mapping.CoordinateMap;
import org.xenei.robot.common.mapping.Map;
import org.xenei.robot.common.planning.Solution;
import org.xenei.robot.common.testUtils.FakeDistanceSensor1;
import org.xenei.robot.common.testUtils.FakeDistanceSensor;
import org.xenei.robot.common.testUtils.MapLibrary;

public class MapperImplTest {

    @Test
    public void processSensorTest() {
        Map map = new MapImpl(ScaleInfo.DEFAULT);
        MapperImpl underTest = new MapperImpl(map);
        CoordinateMap data = MapLibrary.map2('#');
        FakeDistanceSensor sensor = new FakeDistanceSensor1(MapLibrary.map2('#'));
        List<Geometry> obsts = sensor.map().getObstacles().collect(Collectors.toList());
        Position position = new Position(MapImplTest.p);
        Solution solution = new Solution();
        // List<Coordinate> obstacles = MapImplTest.obstacleList();
        sensor.setPosition(position);
        Location[] locs = sensor.sense();
        Arrays.stream(locs).map(l -> position.plus(l))
                .forEach(l -> MapImplTest.assertCoordinateInObstacles(obsts, l.getCoordinate()));

        underTest.processSensorData(position, MapImplTest.t, solution, locs);
        int idx[] = { 0 };
        for (Geometry g : map.getObstacles()) {
            assertTrue(testObstacleInObstacles(obsts, g.getCentroid()), () -> "missing " + idx[0]);
            idx[0]++;
        }
        // map.getObstacles().forEach( o -> assertTrue(
        // testObstacleInObstacles(obsts,o), ()->"missing obstacle"));
    }

    private static boolean testObstacleInObstacles(Collection<? extends Geometry> obsts, Geometry o) {
        boolean found = false;
        for (Geometry geom : obsts) {

            if (geom.contains(o)) {
                found = true;
                break;
            }
        }
        return found;
    }
}
