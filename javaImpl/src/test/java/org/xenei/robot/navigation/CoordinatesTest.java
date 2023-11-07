package org.xenei.robot.navigation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.xenei.robot.testUtils.DoubleUtils.DELTA;
import static org.xenei.robot.testUtils.DoubleUtils.SQRT2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class CoordinatesTest {

    private static double radian45 = Math.PI / 4;
    private static double radian90 = 2 * radian45;
    private static double radian135 = 3 * radian45;
    private static double radian225 = -radian135;
    private static double radian270 = -radian90;
    private static double radian315 = -radian45;

    private static final int INPUT = 0;
    private static final int DEG = 1;
    private static final int RAD = 2;
    private static final int RANGE = 3;
    private static final int X = 4;
    private static final int Y = 5;

    private static double[][] arguments() {
        double range45 = SQRT2 / 2;
        return new double[][] {
                // @format:off
                // input deg rad range X Y number
                { 0, 0, 0.0, 1.0, 1.0, 0.0 }, // 1
                { 45, 45, radian45, 1.0, range45, range45 }, // 2
                { 90, 90, radian90, 1.0, 0.0, 1.0 }, // 3
                { 135, 135, radian135, 1.0, -range45, range45 }, // 4
                { 180, 180, Math.PI, 1.0, -1.0, 0.0 }, // 5
                { 225, -135, radian225, 1.0, -range45, -range45 }, // 6
                { 270, -90, radian270, 1.0, 0.0, -1.0 }, // 7
                { 315, -45, radian315, 1.0, range45, -range45 }, // 8
                { 360, 0, 0, 1.0, 1.0, 0.0 }, // 9

                { -360, 0, 0.0, 1.0, 1.0, 0.0 }, // 10
                { -315, 45, radian45, 1.0, range45, range45 }, // 11
                { -270, 90, radian90, 1.0, 0.0, 1.0 }, // 12
                { -225, 135, radian135, 1.0, -range45, range45 }, // 13
                { -180, -180, -Math.PI, 1.0, -1.0, 0.0 }, // 14
                { -135, -135, radian225, 1.0, -range45, -range45 }, // 15
                { -90, -90, radian270, 1.0, 0.0, -1.0 }, // 16
                { -45, -45, radian315, 1.0, range45, -range45 }, // 17
                { 0, 0, 0, 1.0, 1.0, 0.0 } // 18
        };
    }

    @ParameterizedTest
    @MethodSource("degreeParameters")
    public void fromDegreesTest(double degrees, double thetaD, double thetaR, double range, double x, double y) {
        Coordinates underTest = Coordinates.fromDegrees(degrees, range);
        assertEquals(range, underTest.getRange(), DELTA);
        assertEquals(x, underTest.getX(), DELTA);
        assertEquals(y, underTest.getY(), DELTA);
        assertEquals(thetaD, underTest.getThetaDegrees(), DELTA);
        assertEquals(thetaR, underTest.getThetaRadians(), DELTA);
    }

    private static Stream<Arguments> degreeParameters() {
        return Arrays.stream(arguments())
                .map(ary -> Arguments.of(ary[INPUT], ary[DEG], ary[RAD], ary[RANGE], ary[X], ary[Y]));
    }

    @ParameterizedTest
    @MethodSource("degreeParameters")
    public void fromRadiansTest(double degrees, double thetaD, double thetaR, double range, double x, double y) {
        double radians = Math.toRadians(degrees);
        Coordinates underTest = Coordinates.fromRadians(radians, range);
        assertEquals(range, underTest.getRange(), DELTA);
        assertEquals(x, underTest.getX(), DELTA);
        assertEquals(y, underTest.getY(), DELTA);
        assertEquals(thetaD, underTest.getThetaDegrees(), DELTA);
        assertEquals(thetaR, underTest.getThetaRadians(), DELTA);
    }

    @ParameterizedTest
    @MethodSource("degreeParameters")
    public void fromXYTest(double degrees, double thetaD, double thetaR, double range, double x, double y) {
        Coordinates underTest = Coordinates.fromXY(x, y);
        // fromXY never generates -180 so if degrees = -180 expect 180 instead
        if (thetaD == -180.0) {
            thetaD = 180;
            thetaR = Math.PI;
        }
        assertEquals(range, underTest.getRange(), DELTA);
        assertEquals(x, underTest.getX(), DELTA);
        assertEquals(y, underTest.getY(), DELTA);
        assertEquals(thetaD, underTest.getThetaDegrees(), DELTA);
        assertEquals(thetaR, underTest.getThetaRadians(), DELTA);
    }

    @ParameterizedTest
    @MethodSource("angleParameters")
    public void normalizeTest(double arg, double expected) {
        assertEquals(expected, Coordinates.normalize(expected), DELTA);
    }

    private static Stream<Arguments> angleParameters() {
        double incr = Math.PI / 7;
        double position = -2 * Math.PI;
        double expected = 0;
        List<Arguments> lst = new ArrayList<Arguments>();
        lst.add(Arguments.of(0, 0));
        lst.add(Arguments.of(-0, -0));
        lst.add(Arguments.of(-Math.PI, -Math.PI));
        while (position <= 2 * Math.PI) {
            lst.add(Arguments.of(position, expected));
            position += incr;
            expected += incr;
            if (expected > Math.PI) {
                expected -= 2 * Math.PI;
            }
        }
        do {
            lst.add(Arguments.of(position, expected));
            position -= incr;
            expected -= incr;
            if (expected < -Math.PI) {
                expected += 2 * Math.PI;
            }
        } while (position > -2 * Math.PI);

        return Stream.of(lst.toArray(new Arguments[0]));
    }

    @ParameterizedTest
    @MethodSource("coordPairParameters")
    public void distanceToTest(Coordinates a, Coordinates b, double expected, double angle) {
        assertEquals(expected, a.distanceTo(b), DELTA);
        assertEquals(expected, b.distanceTo(a), DELTA);
        assertEquals(0.0, a.distanceTo(a), DELTA);
        assertEquals(0.0, b.distanceTo(b), DELTA);
    }

    @ParameterizedTest
    @MethodSource("coordPairParameters")
    public void angleToTest(Coordinates a, Coordinates b, double expected, double angle) {
        assertEquals(expected, a.distanceTo(b), DELTA);
        assertEquals(expected, b.distanceTo(a), DELTA);
        assertEquals(0.0, a.distanceTo(a), DELTA);
        assertEquals(0.0, b.distanceTo(b), DELTA);
    }

    private static Stream<Arguments> coordPairParameters() {
        Coordinates origin = Coordinates.fromXY(0, 0);

        double[][] args = arguments();
        List<Arguments> lst = new ArrayList<Arguments>();

        List<Coordinates> coords = Arrays.stream(args).map(ary -> Coordinates.fromXY(ary[X], ary[Y]))
                .collect(Collectors.toList());
        for (int i = 0; i < args.length; i++) {
            lst.add(Arguments.of(coords.get(i), origin, args[i][RANGE], args[i][RAD]));
        }

        coords = Arrays.stream(args).map(ary -> Coordinates.fromRadians(ary[RAD], ary[RANGE]))
                .collect(Collectors.toList());
        for (int i = 0; i < args.length; i++) {
            lst.add(Arguments.of(coords.get(i), origin, args[i][RANGE], args[i][RAD]));
        }

        coords = Arrays.stream(args).map(ary -> Coordinates.fromDegrees(ary[DEG], ary[RANGE]))
                .collect(Collectors.toList());
        for (int i = 0; i < args.length; i++) {
            lst.add(Arguments.of(coords.get(i), origin, args[i][RANGE], args[i][RAD]));
        }

        double[][] sets = {
                // @format:off
                // x1 y1 x2 y2 r theta
                { 0, 0, -4, 0, 4, -180 }, { 0, 0, 3, 0, 3, 0 }, { 0, 0, -4, 4, Math.sqrt(32), 135 },
                { 0, 0, 0, 4, 4, 90 }, { 0, 0, 3, 4, Math.sqrt(25), 45 }, { 0, 0, -4, -3, Math.sqrt(25), -135 },
                { 0, 0, 0, -3, 3, -90 }, { 0, 0, 3, -3, Math.sqrt(18), -45 } };
        // @format:on

        Consumer<double[]> addToList = ary -> {
            Coordinates a = Coordinates.fromXY(ary[0], ary[1]);
            Coordinates b = Coordinates.fromXY(ary[2], ary[3]);
            lst.add(Arguments.of(a, b, ary[4], ary[5]));
        };

        // at origin
        Arrays.stream(sets).forEach(addToList);

        // shifting the sets should not change anything;

        Arrays.stream(sets).map(s -> adjustSet(s, 1, 0)).forEach(addToList);
        Arrays.stream(sets).map(s -> adjustSet(s, -1, 0)).forEach(addToList);
        Arrays.stream(sets).map(s -> adjustSet(s, 0, 1)).forEach(addToList);
        Arrays.stream(sets).map(s -> adjustSet(s, 0, -1)).forEach(addToList);
        Arrays.stream(sets).map(s -> adjustSet(s, 1, 1)).forEach(addToList);
        Arrays.stream(sets).map(s -> adjustSet(s, -1, 1)).forEach(addToList);
        Arrays.stream(sets).map(s -> adjustSet(s, -1, -1)).forEach(addToList);
        Arrays.stream(sets).map(s -> adjustSet(s, 1, -1)).forEach(addToList);
        // first quad
        Arrays.stream(sets).map(s -> adjustSet(s, 5, 5)).forEach(addToList);
        // second quad
        Arrays.stream(sets).map(s -> adjustSet(s, -12, 6)).forEach(addToList);
        // 3rd quad
        Arrays.stream(sets).map(s -> adjustSet(s, -15, -7)).forEach(addToList);
        // 4th quad
        Arrays.stream(sets).map(s -> adjustSet(s, 7, -9)).forEach(addToList);

        return Stream.of(lst.toArray(new Arguments[0]));
    }

    private static double[] adjustSet(double[] set, double deltaX, double deltaY) {
        return new double[] { set[0] + deltaX, set[1] + deltaY, set[2] + deltaX, set[3] + deltaY, set[4], set[5] };
    }

    @Test
    public void equalsTest() {
        // create a bunch of corrdinates and then increment the ranges until they are
        // all outside the
        // quantized space.
        Coordinates[] data = { Coordinates.fromDegrees(0, .1), Coordinates.fromDegrees(45, .1),
                Coordinates.fromDegrees(90, .1), Coordinates.fromDegrees(135, .1), Coordinates.fromDegrees(180, .1),
                Coordinates.fromDegrees(225, .1), Coordinates.fromDegrees(270, .1), Coordinates.fromDegrees(315, .1),
                Coordinates.fromDegrees(360, .1) };

        Coordinates origin = Coordinates.fromXY(0, 0);

        for (Coordinates c : data) {
            assertEquals(origin, c);
            assertEquals(origin.hashCode(), c.hashCode());
        }

        List<Coordinates> lst = Arrays.asList(data);
        for (int i = 0; i < 3; i++) {
            lst = lst.stream().map(c -> Coordinates.fromDegrees(c.getThetaDegrees(), c.getRange() + 0.1))
                    .collect(Collectors.toList());
            for (Coordinates c : lst) {
                assertEquals("fails at " + i, origin, c);
                assertEquals("fails at " + i + " " + c, origin.hashCode(), c.hashCode());
            }
        }
        lst = lst.stream().map(c -> Coordinates.fromDegrees(c.getThetaDegrees(), c.getRange() + 0.1))
                .collect(Collectors.toList());
        for (int i = 0; i < lst.size(); i++) {
            Coordinates c = lst.get(i);
            // negatvie coordinates of .5 distance are in.
            // positive coordinates of .5 distance are out.
            if (i == 0 || i == 2 || i == 8) {
                assertNotEquals(origin, c);
            } else {
                assertEquals("fails at " + i, origin, c);
                assertEquals("fails at " + i + " " + c, origin.hashCode(), c.hashCode());
            }
        }
        lst = lst.stream().map(c -> Coordinates.fromDegrees(c.getThetaDegrees(), c.getRange() + 0.1))
                .collect(Collectors.toList());

        for (int j = 0; j < 2; j++) {
            for (int i = 0; i < lst.size(); i++) {
                Coordinates c = lst.get(i);
                // negative coordinates of .5 distance are in.
                // positive coordinates of .5 distance are out.
                if (i % 2 == 0) {
                    assertNotEquals(origin, c);
                } else {
                    assertEquals("fails at " + i, origin, c);
                    assertEquals("fails at " + i + " " + c, origin.hashCode(), c.hashCode());
                }
            }

            lst = lst.stream().map(c -> Coordinates.fromDegrees(c.getThetaDegrees(), c.getRange() + 0.1))
                    .collect(Collectors.toList());
        }
        for (int i = 0; i < lst.size(); i++) {
            assertNotEquals(origin, lst.get(i));
        }
    }

    @ParameterizedTest
    @MethodSource("triCoordinates")
    public void plusAndMinusTest(Coordinates a, Coordinates b, Coordinates c) {
        assertEquals(c, a.plus(b));
        assertEquals(c, b.plus(a));
        assertEquals(a, c.minus(b));
        assertEquals(b, c.minus(a));
    }

    private static Stream<Arguments> triCoordinates() {
        return Stream.of(Arguments.of(Coordinates.fromXY(-2, 3), Coordinates.fromXY(6, 1), Coordinates.fromXY(4, 4)));
    }

}
