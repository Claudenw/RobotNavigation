package org.xenei.robot.planner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.xenei.robot.navigation.Coordinates;
import org.xenei.robot.navigation.Point;
import org.xenei.robot.navigation.Position;
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
        underTest = new Planner(sensor, finalCoord);
        Set<Coordinates> result = underTest.sense(position);
        for (Point pos : PlannerMapTest.expected) {
            Coordinates c = Coordinates.fromXY(pos);
            assertTrue(result.contains(c), () -> String.format("Missing coord %s", c));
            Optional<PlanRecord> pr = underTest.getMap().getPlanRecord(c);
            assertTrue(pr.isPresent(), () -> "Plan record " + c + " is missing.");
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
        for (int x = 0; x <= 13; x++) {
            for (int y = 0; y <= 15; y++) {
                Coordinates c = Coordinates.fromXY(x, y);
                if (!map.isEnabled(c)) {
                    underTest = new Planner(sensor, origin);
                    underTest.setTarget(c, position);
                    verifyState();
                    assertEquals(position.coordinates(), underTest.getPath().get(0).coordinates());
                }
            }
        }
    }

    private CoordinateMap verifyState() {
        CoordinateMap map = MapLibrary.map1('#');
        FakeSensor sensor = new FakeSensor(map);
        for (Coordinates c : underTest.getSensed()) {
            assertTrue(map.isEnabled(c), () -> c + " should have been sensed.");
        }
        CoordinateMap sensedMap = new CoordinateMap(map.scale());
        sensedMap.enable(underTest.getSensed(), 'x');

        for (PlanRecord pr : underTest.getPlanRecords()) {
            assertFalse(sensedMap.isEnabled(pr.coordinates()),
                    () -> "Plan record " + pr + " should not have been sensed");
        }

        for (PlanRecord pr : underTest.getPath()) {
            assertFalse(sensedMap.isEnabled(pr.coordinates()),
                    () -> "Path record " + pr + " should not have been enabled");
        }
        return sensedMap;
    }

    @Test
    public void stepTest() {
        CoordinateMap map = MapLibrary.map2('#');
        FakeSensor sensor = new FakeSensor(map);
        Coordinates finalCoord = Coordinates.fromXY(-1, 1);
        Coordinates startCoord = Coordinates.fromXY(-1, -3);
        Position position = new Position(startCoord);
        System.out.println(map);
        underTest = new Planner(sensor);
        underTest.setTarget(finalCoord, position);
        boolean cont = underTest.step(position);
        assertTrue(cont);
    }
}
