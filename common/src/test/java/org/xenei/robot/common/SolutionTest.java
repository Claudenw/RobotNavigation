package org.xenei.robot.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.xenei.robot.common.planning.Solution;

public class SolutionTest {

    private Solution underTest;

    public static Coordinate[] expectedSolution = { new Coordinate(-1, -3), new Coordinate(-1, -2),
            new Coordinate(-2, -2), new Coordinate(0, -2), new Coordinate(2, -2), new Coordinate(2, -1),
            new Coordinate(2, 0), new Coordinate(-1, 1) };

    public static Coordinate[] expectedSimplification = { new Coordinate(-1, -3), new Coordinate(2, -2),
            new Coordinate(2, 0), new Coordinate(-1, 1) };

    public static double expectedCost = 11.16227766016838;

    public static double expectedSimplifiedCost = 8.32455532033676;

    @BeforeEach
    public void setup() {
        underTest = new Solution();
        Arrays.stream(expectedSolution).forEach(p -> underTest.add(Location.from(p)));
    }

    @Test
    public void testEmptyRetrieval() {
        underTest = new Solution();
        assertTrue(underTest.isEmpty());
        assertNull(underTest.end());
        assertNull(underTest.start());
        assertEquals(-1, underTest.stepCount());
        List<Coordinate> solution = underTest.stream().collect(Collectors.toList());
        assertTrue(solution.isEmpty());
        assertEquals(Double.POSITIVE_INFINITY, underTest.cost());
    }

    @Test
    public void testRetrieval() {
        assertFalse(underTest.isEmpty());
        assertEquals(new Coordinate(-1, 1), underTest.end());
        assertEquals(new Coordinate(-1, -3), underTest.start());
        assertEquals(expectedSolution.length - 1, underTest.stepCount());
        List<Coordinate> solution = underTest.stream().collect(Collectors.toList());
        List<Coordinate> expected = Arrays.stream(expectedSolution).collect(Collectors.toList());
        assertEquals(expected, solution);
        assertEquals(expectedCost, underTest.cost());
    }

    @Test
    public void simplifyTest() {
        boolean canSee[][] = new boolean[expectedSolution.length][expectedSolution.length];

        /*                                        0     1     2     3     4     5      6      7 */
        /* -1, -3 */ canSee[0] = new boolean[] { true, true, true, true, true, false, false, false };
        /* -1, -2 */ canSee[1] = new boolean[] { true, true, true, true, true, false, false, false };
        /* -2, -2 */ canSee[2] = new boolean[] { true, true, true, true, true, false, false, false };
        /* -0, -2 */ canSee[3] = new boolean[] { true, true, true, true, true, false, false, false };
        /*  2, -2 */ canSee[4] = new boolean[] { true, true, true, true, true, true, true, false };
        /*  2, -1 */ canSee[5] = new boolean[] { false, false, false, false, true, true, true, false  };
        /*  2,  0 */ canSee[6] = new boolean[] { false, false, false, false, true, true, true, true };
        /* -1,  1 */ canSee[7] = new boolean[] { false, false, false, false, false, false, true, true };

        List<Coordinate> idx = Arrays.asList(expectedSolution);
        underTest.simplify( (x,y) -> canSee[idx.indexOf(x)][idx.indexOf(y)]);
        assertEquals(3, underTest.stepCount());
        assertEquals(new Coordinate(-1, 1), underTest.end());
        assertEquals(new Coordinate(-1, -3), underTest.start());
        List<Coordinate> solution = underTest.stream().collect(Collectors.toList());
        List<Coordinate> expected = Arrays.stream(expectedSimplification).collect(Collectors.toList());
        assertEquals(expected, solution);
        assertEquals(expectedSimplifiedCost, underTest.cost());
    }

}
