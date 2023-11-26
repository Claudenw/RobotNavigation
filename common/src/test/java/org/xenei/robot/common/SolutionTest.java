package org.xenei.robot.common;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xenei.robot.common.testUtils.MapLibrary;

public class SolutionTest {

    private Solution underTest;

    public static Point[] expectedSolution = { new Point(-1, -3), new Point(-1, -2), new Point(-2, -2),
            new Point(0, -2), new Point(2, -2), new Point(2, -1), new Point(2, 0), new Point(-1, 1) };

    public static Point[] expectedSimplification = { new Point(-1, -3), new Point(2, -2), new Point(2, 0),
            new Point(-1, 1) };

    @BeforeEach
    public void setup() {
        underTest = new Solution();
        Arrays.stream(expectedSolution).forEach(p -> underTest.add(Coordinates.fromXY(p)));
    }

    @Test
    public void testEmptyRetrieval() {
        underTest = new Solution();
        assertTrue(underTest.isEmpty());
        assertNull(underTest.end());
        assertNull(underTest.start());
        assertEquals(-1, underTest.stepCount());
        List<Coordinates> solution = underTest.stream().collect(Collectors.toList());
        assertTrue(solution.isEmpty());
    }

    @Test
    public void testRetrieval() {
        assertFalse(underTest.isEmpty());
        assertEquals(Coordinates.fromXY(-1, 1), underTest.end());
        assertEquals(Coordinates.fromXY(-1, -3), underTest.start());
        assertEquals(expectedSolution.length - 1, underTest.stepCount());
        List<Point> solution = underTest.stream().map(Coordinates::getPoint).collect(Collectors.toList());
        assertArrayEquals(expectedSolution, solution.toArray());
    }

    @Test
    public void simplifyTest() {
        CoordinateMap cmap = MapLibrary.map2('#');
        underTest.simplify(cmap::clearView);
        assertEquals(3, underTest.stepCount());
        assertEquals(Coordinates.fromXY(-1, 1), underTest.end());
        assertEquals(Coordinates.fromXY(-1, -3), underTest.start());
        List<Point> solution = underTest.stream().map(Coordinates::getPoint).collect(Collectors.toList());
        assertArrayEquals(expectedSimplification, solution.toArray());
    }

}
