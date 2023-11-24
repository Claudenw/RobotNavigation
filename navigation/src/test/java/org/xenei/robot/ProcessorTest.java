package org.xenei.robot;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.xenei.robot.common.Coordinates;
import org.xenei.robot.common.Mover;
import org.xenei.robot.common.Point;
import org.xenei.robot.common.testUtils.FakeMover;
import org.xenei.robot.common.testUtils.FakeSensor;
import org.xenei.robot.common.testUtils.MapLibrary;
import org.xenei.robot.planner.SolutionTest;


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
    @Disabled( "Rework to use messages?")
    public void moveToTest() {
        underTest.moveTo(finalCoord);
       //waitForStart();
        waitForStop();
        List<Point> solution = underTest.getSolution().map(Coordinates::getPoint).collect(Collectors.toList());
        assertArrayEquals(SolutionTest.expectedSimplification, solution.toArray());
        assertEquals(finalCoord.getPoint(), solution.get(solution.size() - 1));
    }

    @Test
    @Disabled( "Rework to use messages?")
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
