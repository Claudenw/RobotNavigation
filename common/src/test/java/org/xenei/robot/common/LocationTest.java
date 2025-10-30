package org.xenei.robot.common;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;

import static org.xenei.robot.common.utils.AngleUtils.RADIANS_180;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.locationtech.jts.geom.Coordinate;
import org.xenei.robot.common.mapping.MapCoordinate;
import org.xenei.robot.common.testUtils.CoordinateUtils;
import org.xenei.robot.common.utils.AngleUtils;
import org.xenei.robot.common.utils.CoordUtils;
import org.xenei.robot.common.utils.CoordUtilsTest;

public class LocationTest {

    private final Location underTest = Location.asLocation(new Coordinate(4, 5));

    @Test
    void getCoordinateTest() {
        CoordinateUtils.assertEquivalent(new Coordinate(4, 5), underTest.getCoordinate());
    }

    @Test
    void getXTest() {
        assertEquals(4, underTest.getX());
    }

    @Test
    void getYTest() {
        assertEquals(5, underTest.getY());
    }

    @Test
    void equals2DTest() {
        Location other = Location.asLocation(new Coordinate(4, 5));
        assertTrue(underTest.equals2D(other));
        other = Location.asLocation(new Coordinate(4.5, 4.5));
        assertFalse(underTest.equals2D(other));
    }

    @Test
    void compareToTest() {
        Location other = Location.asLocation(new Coordinate(4, 5));
        assertEquals(0, underTest.compareTo(other));
        other = Location.asLocation(new Coordinate(4.5, 4.5));
        assertEquals(-1, underTest.compareTo(other));
        other = Location.asLocation(new Coordinate(3.5, 4.5));
        assertEquals(1, underTest.compareTo(other));
    }

    @Test
    void distanceTest() {
        Location other = Location.asLocation(new Coordinate(6, 5));
        assertEquals(2, underTest.distance(other));
    }

    @Test
    void angleBetweenTest() {
        Location other = Location.asLocation(new Coordinate(6, 6));
        assertEquals(0.463647609000806, underTest.angleBetween(other), AngleUtils.TOLERANCE);
    }

    @ParameterizedTest
    @MethodSource("coordPairParameters")
    void distanceTest(Location a, Location b, double expected, double angle) {
        assertEquals(expected, a.distance(b), AngleUtils.TOLERANCE);
        assertEquals(expected, b.distance(a), AngleUtils.TOLERANCE);
        assertEquals(0.0, a.distance(a), AngleUtils.TOLERANCE);
        assertEquals(0.0, b.distance(b), AngleUtils.TOLERANCE);
    }

    @ParameterizedTest
    @MethodSource("coordPairParameters")
    void angleBetweenTest(Location a, Location b, double expected, double angle) {
        assertEquals(AngleUtils.normalize(RADIANS_180 + angle), a.angleBetween(b), AngleUtils.TOLERANCE);
        assertEquals(angle, b.angleBetween(a), AngleUtils.TOLERANCE);
        assertEquals(0.0, a.angleBetween(a), AngleUtils.TOLERANCE);
        assertEquals(0.0, b.angleBetween(b), AngleUtils.TOLERANCE);
    }

    @ParameterizedTest
    @MethodSource("coordPairParameters")
    public void rangeAndThetaTest(Location a, Location ignored, double expected, double angle) {
        assertEquals(expected, a.range(), AngleUtils.TOLERANCE);
        assertEquals(angle, a.theta(), AngleUtils.TOLERANCE);
    }

    private static void processStream(List<Arguments> lst, double[] args) {
        Location l = Location.asLocation(new Coordinate(args[CoordUtilsTest.X], args[CoordUtilsTest.Y]));
        lst.add(Arguments.of(l, MapCoordinate.ORIGIN, args[CoordUtilsTest.RANGE], args[CoordUtilsTest.RAD]));

        l = Location.asLocation(CoordUtils.fromAngle(args[CoordUtilsTest.RAD], args[CoordUtilsTest.RANGE]));
        lst.add(Arguments.of(l, MapCoordinate.ORIGIN, args[CoordUtilsTest.RANGE], args[CoordUtilsTest.RAD]));

        l = Location
                .asLocation(CoordUtils.fromAngle(Math.toRadians(args[CoordUtilsTest.DEG]), args[CoordUtilsTest.RANGE]));
        lst.add(Arguments.of(l, MapCoordinate.ORIGIN, args[CoordUtilsTest.RANGE], args[CoordUtilsTest.RAD]));
    }

    private static Stream<Arguments> coordPairParameters() {

        List<Arguments> lst = new ArrayList<Arguments>();

        Arrays.stream(CoordUtilsTest.arguments()).forEach(s -> processStream(lst, s));

        return Stream.of(lst.toArray(new Arguments[0]));
    }

    @Test
    public void thetaTest() {
        Location underTest = Location.asLocation(new Coordinate(0, 5));
        System.out.println(underTest.theta());
    }

}
