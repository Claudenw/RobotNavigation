package org.xenei.robot.planner;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.xenei.robot.common.CoordinateMap;
import org.xenei.robot.common.Coordinates;
import org.xenei.robot.common.Map;
import org.xenei.robot.common.Point;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.SolutionTest;
import org.xenei.robot.common.Target;
import org.xenei.robot.common.testUtils.FakeSensor;
import org.xenei.robot.common.testUtils.MapLibrary;
import org.xenei.robot.mapper.MapperImpl;
import org.xenei.robot.mapper.PlannerMap;

public class PlannerTest {

    private PlannerImpl underTest;
//
//    @Test
//    public void senseTest() {
//
//       
//        FakeSensor sensor = new FakeSensor(MapLibrary.map2('#'));
//        Map map = new PlannerMap();
//        Mapper mapper = new MapperImpl(map);
//        Coordinates finalCoord = Coordinates.fromXY(-1, 1);
//        Coordinates startCoord = Coordinates.fromXY(-1, -3);
//        underTest = new PlannerImpl(map, startCoord, finalCoord);
//        mapper.processSensorData(underTest, sensor.sense(underTest.getCurrentPosition()));
//        Set<Coordinates> result = underTest.sense().collect(Collectors.toSet());
//        for (Point point : PlannerMapTest.expected) {
//            Coordinates c = Coordinates.fromXY(point);
//            assertTrue(result.contains(c), () -> String.format("Missing coord %s", c));
//        }
//        assertEquals(finalCoord, underTest.getTarget());
//        for (Point pos : PlannerMapTest.obstacles) {
//            Coordinates c = Coordinates.fromXY(pos);
//            assertTrue(underTest.getMap().isObstacle(c), () -> String.format("Missing obstacle %s", c));
//        }
//    }

    @Test
    public void setTargetTest() {
        CoordinateMap cmap = MapLibrary.map2('#');
        Map map = new PlannerMap();
        Coordinates origin = Coordinates.fromXY(0, 0);
        underTest = new PlannerImpl(map, origin);
        for (int x = 0; x <= 13; x++) {
            for (int y = 0; y <= 15; y++) {
                Coordinates c = Coordinates.fromXY(x, y);
                if (!cmap.isEnabled(c)) {
                    underTest.setTarget(c);
                    verifyState(map, cmap);
                    assertEquals(0, underTest.getSolution().stepCount());
                }
            }
        }
    }

    private CoordinateMap verifyState(Map map, CoordinateMap cmap) {
        for (Coordinates c : map.getObstacles()) {
            assertTrue(cmap.isEnabled(c), () -> c + " should have been sensed.");
        }
        CoordinateMap sensedMap = new CoordinateMap(cmap.scale());
        sensedMap.enable(map.getObstacles(), 'x');

        for (Target pr : map.getTargets()) {
            assertFalse(sensedMap.isEnabled(pr.coordinates()),
                    () -> "Plan record " + pr + " should not have been sensed");
        }

        underTest.getSolution().stream().forEach(
                c -> assertFalse(sensedMap.isEnabled(c), () -> "Path record " + c + " should not have been enabled"));
        return sensedMap;
    }

    @Test
    public void stepTest() {
        FakeSensor sensor = new FakeSensor(MapLibrary.map2('#'));
        Map map = new PlannerMap();
        MapperImpl mapper = new MapperImpl(map);

        Coordinates finalCoord = Coordinates.fromXY(-1, 1);
        Coordinates startCoord = Coordinates.fromXY(-1, -3);

        underTest = new PlannerImpl(map, startCoord, finalCoord);

        int stepCount = 0;
        int maxLoops = 100;
        mapper.processSensorData(underTest, sensor.sense(underTest.getCurrentPosition()));
        while (underTest.step()) {
            if (maxLoops < stepCount++) {
                fail("Did not find solution in " + maxLoops + " steps");
            }
            mapper.processSensorData(underTest, sensor.sense(underTest.getCurrentPosition()));
            double angle = underTest.getCurrentPosition().coordinates().angleTo(underTest.getTarget());
            underTest.changeCurrentPosition(new Position(underTest.getTarget(), angle));
        }
        assertEquals(SolutionTest.expectedSolution.length - 1, underTest.getSolution().stepCount());
        assertEquals(startCoord, underTest.getSolution().start());
        assertEquals(finalCoord, underTest.getSolution().end());
        List<Point> solution = underTest.getSolution().stream().map(Coordinates::getPoint).collect(Collectors.toList());
        assertArrayEquals(SolutionTest.expectedSolution, solution.toArray());
    }
}
