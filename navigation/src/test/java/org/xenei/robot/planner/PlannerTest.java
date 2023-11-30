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
import org.xenei.robot.common.Position;
import org.xenei.robot.common.SolutionTest;
import org.xenei.robot.common.Target;
import org.xenei.robot.common.testUtils.FakeSensor;
import org.xenei.robot.common.testUtils.MapLibrary;
import org.xenei.robot.mapper.MapperImpl;

import mil.nga.sf.Point;

import org.xenei.robot.mapper.MapImpl;

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
        Map map = new MapImpl();
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
            assertFalse(sensedMap.isEnabled(pr),
                    () -> "Plan record " + pr + " should not have been sensed");
        }

        underTest.getSolution().stream().forEach(
                c -> assertFalse(sensedMap.isEnabled(c), () -> "Path record " + c + " should not have been enabled"));
        return sensedMap;
    }

    @Test
    public void stepTestMap2() {
        FakeSensor sensor = new FakeSensor(MapLibrary.map2('#'));
        Map map = new MapImpl();
        MapperImpl mapper = new MapperImpl(map);

        Coordinates finalCoord = Coordinates.fromXY(-1, 1);
        Coordinates startCoord = Coordinates.fromXY(-1, -3);

        underTest = new PlannerImpl(map, startCoord, finalCoord);

        int stepCount = 0;
        int maxLoops = 100;
        sensor.setPosition(underTest.getCurrentPosition());
        mapper.processSensorData(underTest.getCurrentPosition(), underTest.getTarget(), sensor.sense());
        while (underTest.step()) {
            if (maxLoops < stepCount++) {
                fail("Did not find solution in " + maxLoops + " steps");
            }
            double angle = underTest.getCurrentPosition().angleTo(underTest.getTarget());
            underTest.changeCurrentPosition(new Position(underTest.getTarget(), angle));
            sensor.setPosition(underTest.getCurrentPosition());
            mapper.processSensorData(underTest.getCurrentPosition(), underTest.getTarget(), sensor.sense());
        }
        assertEquals(SolutionTest.expectedSolution.length - 1, underTest.getSolution().stepCount());
        assertTrue(startCoord.equalsXY(underTest.getSolution().start()));
        assertTrue(finalCoord.equalsXY(underTest.getSolution().end()));
        List<Point> solution = underTest.getSolution().stream().collect(Collectors.toList());
        assertEquals(SolutionTest.expectedSolution.length, solution.size());
        for (int i=0;i<solution.size();i++) {
            assertTrue(SolutionTest.expectedSolution[i].equalsX(solution.get(i)));
        }
    }
    
    @Test
    public void stepTestMap3() {
        Point[] expectedSimpleSolution = { new Point(-1,-3), new Point(-3, -2), new Point(-4,-1), new Point(-4,0), 
                new Point(-1,1)
        };
        FakeSensor sensor = new FakeSensor(MapLibrary.map3('#'));
        Map map = new MapImpl();
        MapperImpl mapper = new MapperImpl(map);

        Coordinates finalCoord = Coordinates.fromXY(-1, 1);
        Coordinates startCoord = Coordinates.fromXY(-1, -3);

        underTest = new PlannerImpl(map, startCoord, finalCoord);

        int stepCount = 0;
        int maxLoops = 100;
        sensor.setPosition(underTest.getCurrentPosition());
        mapper.processSensorData(underTest.getCurrentPosition(), underTest.getTarget(), sensor.sense());
        while (underTest.step()) {
            if (maxLoops < stepCount++) {
                fail("Did not find solution in " + maxLoops + " steps");
            }
            double angle = underTest.getCurrentPosition().angleTo(underTest.getTarget());
            underTest.changeCurrentPosition(new Position(underTest.getTarget(), angle));
            sensor.setPosition(underTest.getCurrentPosition());
            mapper.processSensorData(underTest.getCurrentPosition(), underTest.getTarget(), sensor.sense());
        }
        assertEquals(22, underTest.getSolution().stepCount());
        assertEquals(33.29126786466034, underTest.getSolution().cost());
        assertTrue(startCoord.equalsXY(underTest.getSolution().start()));
        assertTrue(finalCoord.equalsXY(underTest.getSolution().end()));
        underTest.getSolution().simplify(map::clearView);
        assertEquals(7.812559200041265, underTest.getSolution().cost());
        assertTrue(startCoord.equalsXY(underTest.getSolution().start()));
        assertTrue(finalCoord.equalsXY(underTest.getSolution().end()));
        List<Point> solution = underTest.getSolution().stream().collect(Collectors.toList());
        assertEquals(expectedSimpleSolution.length, solution.size());
        for (int i=0;i<solution.size();i++) {
            assertTrue(expectedSimpleSolution[i].equalsX(solution.get(i)));
        }
    }
}
