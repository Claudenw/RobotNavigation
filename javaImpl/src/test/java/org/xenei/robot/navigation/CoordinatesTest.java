package org.xenei.robot.navigation;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.xenei.robot.testUtils.DoubleUtils.DELTA;
import static org.xenei.robot.testUtils.DoubleUtils.RADIANS_135;
import static org.xenei.robot.testUtils.DoubleUtils.RADIANS_225;
import static org.xenei.robot.testUtils.DoubleUtils.RADIANS_270;
import static org.xenei.robot.testUtils.DoubleUtils.RADIANS_315;
import static org.xenei.robot.testUtils.DoubleUtils.RADIANS_45;
import static org.xenei.robot.testUtils.DoubleUtils.RADIANS_90;
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

    private static final int INPUT = 0;
    private static final int DEG = 1;
    private static final int RAD = 2;
    private static final int RANGE = 3;
    private static final int X = 4;
    private static final int Y = 5;

    private static double[][] arguments() {
        double range45 = SQRT2 / 2;
        return new double[][] {
                // @formatter:off
                //  input   deg     rad             range   X           Y          number
                {   0,      0,      0.0,            1.0,    1.0,        0.0 },      // 1
                {   45,     45,     RADIANS_45,     1.0,    range45,    range45 },  // 2
                {   90,     90,     RADIANS_90,     1.0,    0.0,        1.0 },      // 3
                {   135,    135,    RADIANS_135,    1.0,    -range45,   range45 },  // 4
                {   180,    180,    Math.PI,        1.0,   -1.0,        0.0 },      // 5
                {   225,    -135,   RADIANS_225,    1.0,    -range45,   -range45 }, // 6
                {   270,    -90,    RADIANS_270,    1.0,    0.0,        -1.0 },     // 7
                {   315,    -45,    RADIANS_315,    1.0,    range45,    -range45 }, // 8
                {   360,    0,      0.0,            1.0,    1.0,        0.0 },      // 9

                {   -360,   0,      0.0,            1.0,    1.0,        0.0 },      // 10
                {   -315,   45,     RADIANS_45,     1.0,    range45,    range45 },  // 11
                {   -270,   90,     RADIANS_90,     1.0,    0.0,        1.0 },      // 12
                {   -225,   135,    RADIANS_135,    1.0,    -range45,   range45 },  // 13
                {   -180,   -180,   -Math.PI,       1.0,    -1.0,       0.0 },      // 14
                {   -135,   -135,   RADIANS_225,    1.0,    -range45,   -range45 }, // 15
                {   -90,    -90,    RADIANS_270,    1.0,    0.0,        -1.0 },     // 16
                {   -45,    -45,    RADIANS_315,    1.0,    range45,    -range45 }, // 17
                {   0,      0,      0.0,            1.0,    1.0,        0.0 }       // 18
                // @formatter:on
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
                // @formatter:off
                // x1   y1  x2  y2  r               theta
                { 0,    0,  -4, 0,  4,              -180 }, 
                { 0,    0,  3,  0,  3,              0 }, 
                { 0,    0,  -4, 4,  Math.sqrt(32),  135 },
                { 0,    0,  0,  4,  4,              90 }, 
                { 0,    0,  3,  4,  Math.sqrt(25),  45 }, 
                { 0,    0,  -4, -3, Math.sqrt(25),  -135 },
                { 0,    0,  0,  -3, 3,              -90 }, 
                { 0,    0,  3,  -3, Math.sqrt(18),  -45 } };
        // @formatter:on

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
            final int idx = i;
            lst = lst.stream().map(c -> Coordinates.fromDegrees(c.getThetaDegrees(), c.getRange() + 0.1))
                    .collect(Collectors.toList());
            for (Coordinates c : lst) {
                assertEquals(origin, c, ()->"fails at " + idx);
                assertEquals(origin.hashCode(), c.hashCode(), ()->"fails at " + idx + " " + c);
            }
        }
        lst = lst.stream().map(c -> Coordinates.fromDegrees(c.getThetaDegrees(), c.getRange() + 0.1))
                .collect(Collectors.toList());
        for (int i = 0; i < lst.size(); i++) {
            final int idx = i;
            Coordinates c = lst.get(i);
            // negatvie coordinates of .5 distance are in.
            // positive coordinates of .5 distance are out.
            if (i == 0 || i == 2 || i == 8) {
                assertNotEquals(origin, c);
            } else {
                assertEquals(origin, c, ()->"fails at " + idx);
                assertEquals(origin.hashCode(), c.hashCode(), ()->"fails at " + idx + " " + c);
            }
        }
        lst = lst.stream().map(c -> Coordinates.fromDegrees(c.getThetaDegrees(), c.getRange() + 0.1))
                .collect(Collectors.toList());

        for (int j = 0; j < 2; j++) {
            for (int i = 0; i < lst.size(); i++) {
                final int idx = i;
                Coordinates c = lst.get(i);
                // negative coordinates of .5 distance are in.
                // positive coordinates of .5 distance are out.
                if (i % 2 == 0) {
                    assertNotEquals(origin, c);
                } else {
                    assertEquals(origin, c, ()->"fails at " + idx);
                    assertEquals(origin.hashCode(), c.hashCode(), ()->"fails at " + idx + " " + c);
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
        return Stream.of(Arguments.of(Coordinates.fromXY(-2, 3), Coordinates.fromXY(6, 1), Coordinates.fromXY(4, 4)),
                Arguments.of(Coordinates.fromRadians(0.7853981633974483, 2),
                        Coordinates.fromRadians(0, 2.8284271247461903),
                        Coordinates.fromRadians(0.3217505543966422, 4.47213595499958)),
                Arguments.of(Coordinates.fromXY(0, 0), Coordinates.fromDegrees(45, 1), Coordinates.fromDegrees(45, 1)));
    }

    @ParameterizedTest
    @MethodSource("overlapCoordinates")
    public void overlapTest(boolean state, Coordinates a, Coordinates b, double range) {
        if (state) {
            assertTrue( a.overlap(b, range), () -> "Should not overlap "+b);
        } else {
            assertFalse( a.overlap(b, range), () -> "Should overlap "+b);
        }
    }
    
    private static final int X1=0;
    private static final int Y1=1;
    private static final int X2=2;
    private static final int Y2=3;
    private static final int R=4;
    private static Stream<Arguments> overlapCoordinates() {
        double[][] trueSet = {
                {0, 0, -.5, 0, 1}, {0, 0, 0, 0, 1}, {0, 0, .5, 0, 1},
                {0, 0, 0, -.5, 1}, {0, 0, 0, .5, 1}};
        double[][] falseSet = {{0, 0, -1.5, 0, 1}, {0, 0, 1.5, 0, 1},
                {0, 0, 0, -1.5, 1}, {0, 0, 0, 1.5, 1}};
        
        List<Arguments> args = new ArrayList<>();
        for (int x = -1; x<2;x++) {
            for (int y = -1; y<2; y++){
                for (double[] pattern : trueSet)
                {
                    args.add( Arguments.of( true, 
                            Coordinates.fromXY(pattern[X1]+x, pattern[Y1]+y),
                            Coordinates.fromXY(pattern[X2]+x, pattern[Y2]+y),
                            pattern[R]));
                  
                }
                for (double[] pattern : falseSet)
                {
                    args.add( Arguments.of( false, 
                            Coordinates.fromXY(pattern[X1]+x, pattern[Y1]+y),
                            Coordinates.fromXY(pattern[X2]+x, pattern[Y2]+y),
                            pattern[R]));
                  
                }
            }
        }
        return args.stream();
    }
}
