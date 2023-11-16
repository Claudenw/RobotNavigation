package org.xenei.robot.planner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.xenei.robot.navigation.Coordinates;
import org.xenei.robot.navigation.Point;
import org.xenei.robot.navigation.Position;
import org.xenei.robot.testUtils.FakeMover;
import org.xenei.robot.testUtils.FakeSensor;
import org.xenei.robot.testUtils.MapLibrary;
import org.xenei.robot.utils.CoordinateMap;

public class PlannerTest {

    private Planner underTest;

    @Test
    public void senseTest() {
       
        CoordinateMap map = MapLibrary.map2('#');
        FakeSensor sensor = new FakeSensor(map);
        Coordinates finalCoord = Coordinates.fromXY(-1, 1);
        Coordinates startCoord = Coordinates.fromXY(-1, -3);
        Position position = new Position(startCoord);
        underTest = new Planner(sensor, position, finalCoord);
        Set<Coordinates> result = underTest.sense().collect(Collectors.toSet());
        for (Point pos : PlannerMapTest.expected) {
            Coordinates c = Coordinates.fromXY(pos);
            assertTrue(result.contains(c), () -> String.format("Missing coord %s", c));
//            Optional<PlanRecord> pr = underTest.getMap().getPlanRecord(c);
//            assertTrue(pr.isPresent(), () -> "Plan record " + c + " is missing.");
        }
        assertEquals(finalCoord, underTest.getTarget());
        System.out.println( underTest.getMap().dumpModel());
        for (Point pos : PlannerMapTest.obstacles) {
            Coordinates c = Coordinates.fromXY(pos);
            assertTrue(underTest.getMap().isObstacle(c), () -> String.format("Missing obstacle %s", c));
        }
    }

    @Test
    public void setTargetTest() {
        CoordinateMap map = MapLibrary.map1('#');
        FakeSensor sensor = new FakeSensor(map);
        Coordinates origin = Coordinates.fromXY(0, 0);
        Position position = new Position(origin, 0);
        underTest = new Planner(sensor, position);
        for (int x = 0; x <= 13; x++) {
            for (int y = 0; y <= 15; y++) {
                Coordinates c = Coordinates.fromXY(x, y);
                if (!map.isEnabled(c)) {
                    underTest.setTarget(c);
                    verifyState();
                    assertEquals(0, underTest.getSolution().stepCount());
                }
            }
        }
    }

    private CoordinateMap verifyState() {
        CoordinateMap map = MapLibrary.map1('#');
        for (Coordinates c : underTest.getMap().getObstacles()) {
            assertTrue(map.isEnabled(c), () -> c + " should have been sensed.");
        }
        CoordinateMap sensedMap = new CoordinateMap(map.scale());
        sensedMap.enable(underTest.getMap().getObstacles(), 'x');

        for (PlanRecord pr : underTest.getPlanRecords()) {
            assertFalse(sensedMap.isEnabled(pr.coordinates()),
                    () -> "Plan record " + pr + " should not have been sensed");
        }

        underTest.getSolution().apply( c -> assertFalse(sensedMap.isEnabled(c),
                    () -> "Path record " + c + " should not have been enabled"));
        return sensedMap;
    }

    @Test
    public void stepTest() {
        CoordinateMap map = MapLibrary.map2('#');
        FakeSensor sensor = new FakeSensor(map);
        
        Coordinates finalCoord = Coordinates.fromXY(-1, 1);
        Coordinates startCoord = Coordinates.fromXY(-1, -3);
        underTest = new Planner(sensor, new Position(startCoord), finalCoord);
        
        int stepCount=0;
        int maxLoops=100;
        while (underTest.step()) {
            if (maxLoops < stepCount++) {
                fail("Did not find solution in "+maxLoops+" steps");
            }
            
            double angle = underTest.getPosition().coordinates().angleTo(underTest.getTarget());
            underTest.changeCurrentPosition(new Position(underTest.getTarget(), angle));
        }
        assertEquals(17, underTest.getSolution().stepCount());
        assertEquals(startCoord, underTest.getSolution().start());
        assertEquals(finalCoord, underTest.getSolution().end());
        underTest.getSolution().apply(System.out::println);
    }
}
