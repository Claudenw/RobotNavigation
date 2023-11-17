package org.xenei.robot;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xenei.robot.navigation.Coordinates;
import org.xenei.robot.navigation.Point;
import org.xenei.robot.planner.SolutionTest;
import org.xenei.robot.testUtils.FakeMover;
import org.xenei.robot.testUtils.FakeSensor;
import org.xenei.robot.testUtils.MapLibrary;
import org.xenei.robot.utils.Mover;

public class ProcessorTest {
    private Processor underTest;
    private static final Coordinates finalCoord = Coordinates.fromXY(-1, 1);
    private static final Coordinates startCoord = Coordinates.fromXY(-1, -3);

    @BeforeEach
    public void setup() {
        Mover mover = new FakeMover(startCoord, 1);
        FakeSensor sensor = new FakeSensor(MapLibrary.map2('#'));
        underTest = new Processor(sensor, mover);
    }

    private void waitForStart() {
        while (!underTest.isMoving()) {
        }
    }

    private void waitForStop() {
        while (underTest.isMoving()) {
        }
    }

    @Test
    public void moveToTest() {
        underTest.moveTo(finalCoord);
        waitForStart();
        waitForStop();
        List<Point> solution = underTest.getSolution().map(Coordinates::getPoint).collect(Collectors.toList());
        assertArrayEquals(SolutionTest.expectedSimplification, solution.toArray());
        assertEquals(finalCoord.getPoint(), solution.get(solution.size() - 1));
    }

    @Test
    public void setTargetWhileMovingTest() {
        Coordinates nextCoord = Coordinates.fromXY(4, 4);
        underTest.moveTo(finalCoord);
        waitForStart();
        underTest.setTarget(nextCoord);
        waitForStop();
        List<Point> solution = underTest.getSolution().map(Coordinates::getPoint).collect(Collectors.toList());
        assertEquals(nextCoord.getPoint(), solution.get(solution.size() - 1));
    }

}
