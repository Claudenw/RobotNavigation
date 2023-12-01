package org.xenei.robot.mapper;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.xenei.robot.common.mapping.CoordinateMap;
import org.xenei.robot.common.mapping.Map;
import org.xenei.robot.common.planning.Solution;
import org.xenei.robot.common.testUtils.FakeDistanceSensor;
import org.xenei.robot.common.testUtils.MapLibrary;

import mil.nga.sf.Point;

import org.xenei.robot.common.DistanceSensor;
import org.xenei.robot.common.Position;

public class MapperImplTest {
    
    
    @Test
    public void processSensorTest() {
        Map map = new MapImpl();
        MapperImpl underTest = new MapperImpl(map);
        CoordinateMap data = MapLibrary.map2('#');
        FakeDistanceSensor sensor = new FakeDistanceSensor(MapLibrary.map2('#'));
        Position position = new Position( MapImplTest.p);
        Solution solution = new Solution();
        List<Point> obstacles = MapImplTest.obstacleList();
        sensor.setPosition(position);
        Arrays.stream(sensor.sense()).forEach(o-> assertTrue(obstacles.contains(o.asPoint()), ()-> String.format( "%s not found", o)));
        underTest.processSensorData(position, MapImplTest.t, solution, sensor.sense());
        map.getObstacles().forEach( o -> assertTrue( obstacles.contains(o.asPoint()), ()-> String.format( "%s not found", o)));
    }
}
