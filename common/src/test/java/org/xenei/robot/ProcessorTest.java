package org.xenei.robot;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.xenei.robot.common.Coordinates;
import org.xenei.robot.common.Mover;
import org.xenei.robot.common.SolutionTest;
import org.xenei.robot.common.testUtils.FakeMover;
import org.xenei.robot.common.testUtils.FakeSensor;
import org.xenei.robot.common.testUtils.MapLibrary;

import mil.nga.sf.Point;


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

    @Test
    @Disabled( "Rework to use messages?")
    public void moveToTest() {
        assertTrue(underTest.moveTo(finalCoord));
        List<Point> solution = underTest.getSolution().collect(Collectors.toList());
        assertArrayEquals(SolutionTest.expectedSimplification, solution.toArray());
        assertTrue(finalCoord.equalsXY(solution.get(solution.size() - 1)));
    }

    @Test
    @Disabled( "Rework to use messages?")
    public void setTargetWhileMovingTest() {
        Coordinates nextCoord = Coordinates.fromXY(4, 4);
        underTest.moveTo(finalCoord);
        
        underTest.setTarget(nextCoord);

        List<Point> solution = underTest.getSolution().collect(Collectors.toList());
        assertTrue(nextCoord.equalsXY(solution.get(solution.size() - 1)));
    }

}
