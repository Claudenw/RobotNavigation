package org.xenei.robot.common.planning;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.xenei.robot.common.testUtils.MapLibrary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.xenei.robot.common.ChassisInfoTest;
import org.xenei.robot.common.ScaleInfo;
import org.xenei.robot.common.mapping.Map;
import org.xenei.robot.common.mapping.MapCoordinate;
import org.xenei.robot.common.mapping.MapLocation;
import org.xenei.robot.common.mapping.MapTest;
import org.xenei.robot.common.utils.RobutContext;

public class SolutionTest {

    private Solution underTest;

    public static Coordinate[] expectedSolution = {new Coordinate(-1, -3), new Coordinate(-1, -2),
            new Coordinate(-2, -2), new Coordinate(0, -2), new Coordinate(2, -2), new Coordinate(2, -1),
            new Coordinate(2, 0), new Coordinate(-1, 1)};

    public static Coordinate[] expectedSimplification = {new Coordinate(-1, -3), new Coordinate(2, -2),
            new Coordinate(2, 0), new Coordinate(-1, 1)};

    public static double expectedCost = 11.16227766016838;

    public static double expectedSimplifiedCost = 8.32455532033676;

    private Map map;

    @BeforeEach
    void setup() {
        RobutContext.Builder builder = RobutContext.builder();
        builder.setOptions(builder.defaultOptions());
        map = new Map(builder.build(), new MapTest.TestingStorage());
        MapLibrary.map2(map);
        underTest = new Solution();
        Arrays.stream(expectedSolution).forEach(coordinate -> underTest.add(map.asMapLocation(coordinate)));
    }

    @AfterEach
    void shutdown() {
        map.getContext().close();
    }

    @Test
    void testEmptyRetrieval() {
        underTest = new Solution();
        assertTrue(underTest.isEmpty());
        assertNull(underTest.end());
        assertNull(underTest.start());
        assertEquals(-1, underTest.stepCount());
        List<MapLocation> solution = underTest.stream().toList();
        assertTrue(solution.isEmpty());
        assertEquals(Double.POSITIVE_INFINITY, underTest.cost());
    }

    @Test
    void testRetrieval() {
        assertFalse(underTest.isEmpty());
        assertEquals(new Coordinate(-1, 1), underTest.end().getCoordinate());
        assertEquals(new Coordinate(-1, -3), underTest.start().getCoordinate());
        assertEquals(expectedSolution.length - 1, underTest.stepCount());
        List<Coordinate> solution = underTest.stream().map(MapCoordinate::getCoordinate)
                .collect(Collectors.toList());
        List<Coordinate> expected = Arrays.stream(expectedSolution).collect(Collectors.toList());
        assertEquals(expected, solution);
        assertEquals(expectedCost, underTest.cost());
    }

    @Test
    void simplifyTest() {
        List<Coordinate> idx = Arrays.asList(expectedSolution);
        underTest.simplify();
        assertEquals(3, underTest.stepCount());
        assertEquals(new Coordinate(-1, 1), underTest.end().getCoordinate());
        assertEquals(new Coordinate(-1, -3), underTest.start().getCoordinate());
        List<Coordinate> solution = underTest.stream().map(MapCoordinate::getCoordinate)
                .collect(Collectors.toList());
        List<Coordinate> expected = Arrays.stream(expectedSimplification).collect(Collectors.toList());
        assertEquals(expected, solution);
        assertEquals(expectedSimplifiedCost, underTest.cost());
    }

}
