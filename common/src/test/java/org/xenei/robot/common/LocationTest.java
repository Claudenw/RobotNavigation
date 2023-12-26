package org.xenei.robot.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.xenei.robot.common.utils.AngleUtils.RADIANS_180;
import static org.xenei.robot.common.utils.AngleUtils.RADIANS_45;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.xenei.robot.common.utils.AngleUtils;
import org.xenei.robot.common.utils.CoordUtils;
import org.xenei.robot.common.utils.CoordUtilsTest;

public class LocationTest {
    public static final double DELTA = 0.0000001;

    @ParameterizedTest
    @MethodSource("coordPairParameters")
    public void distanceToTest(Location a, Location b, double expected, double angle) {
        assertEquals(expected, a.distance(b), DELTA);
        assertEquals(expected, b.distance(a), DELTA);
        assertEquals(0.0, a.distance(a), DELTA);
        assertEquals(0.0, b.distance(b), DELTA);
    }

    @ParameterizedTest
    @MethodSource("coordPairParameters")
    public void angleBetweenTest(Location a, Location b, double expected, double angle) {
        assertEquals(AngleUtils.normalize(RADIANS_180 + angle), a.angleBetween(b), DELTA);
        assertEquals(angle, b.angleBetween(a), DELTA);
        assertEquals(0.0, a.angleBetween(a), DELTA);
        assertEquals(0.0, b.angleBetween(b), DELTA);
    }

    private static void processStream(List<Arguments> lst, double[] args) {
        Location l = Location.from(args[CoordUtilsTest.X], args[CoordUtilsTest.Y]);
        lst.add(Arguments.of(l, Location.ORIGIN, args[CoordUtilsTest.RANGE], args[CoordUtilsTest.RAD]));

        l = Location.from(CoordUtils.fromAngle(args[CoordUtilsTest.RAD], args[CoordUtilsTest.RANGE]));
        lst.add(Arguments.of(l, Location.ORIGIN, args[CoordUtilsTest.RANGE], args[CoordUtilsTest.RAD]));

        l = Location.from(CoordUtils.fromAngle(Math.toRadians(args[CoordUtilsTest.DEG]), args[CoordUtilsTest.RANGE]));
        lst.add(Arguments.of(l, Location.ORIGIN, args[CoordUtilsTest.RANGE], args[CoordUtilsTest.RAD]));

    }

    private static Stream<Arguments> coordPairParameters() {

        List<Arguments> lst = new ArrayList<Arguments>();

        Arrays.stream(CoordUtilsTest.arguments()).forEach(s -> processStream(lst, s));

        return Stream.of(lst.toArray(new Arguments[0]));
    }

    @ParameterizedTest
    @MethodSource("triCoordinates")
    public void plusAndMinusTest(Location a, Location b, Location c) {
        assertTrue(c.equals2D(a.plus(b), 0.00001));
        assertTrue(c.equals2D(b.plus(a), 0.00001));
        assertTrue(a.equals2D(c.minus(b), 0.00001));
        assertTrue(b.equals2D(c.minus(a), 0.00001));
    }

    private static Stream<Arguments> triCoordinates() {
        return Stream.of(Arguments.of(Location.from(-2, 3), Location.from(6, 1), Location.from(4, 4)),
                Arguments.of(Location.from(CoordUtils.fromAngle(0.7853981633974483, 2)),
                        Location.from(CoordUtils.fromAngle(0, 2.8284271247461903)),
                        Location.from(CoordUtils.fromAngle(0.3217505543966422, 4.47213595499958))),
                Arguments.of(Location.from(0, 0), Location.from(CoordUtils.fromAngle(RADIANS_45, 1)),
                        Location.from(CoordUtils.fromAngle(RADIANS_45, 1))));
    }
}
