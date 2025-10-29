package org.xenei.robot.common.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.xenei.robot.common.utils.AngleUtils.RADIANS_135;
import static org.xenei.robot.common.utils.AngleUtils.RADIANS_180;
import static org.xenei.robot.common.utils.AngleUtils.RADIANS_225;
import static org.xenei.robot.common.utils.AngleUtils.RADIANS_270;
import static org.xenei.robot.common.utils.AngleUtils.RADIANS_315;
import static org.xenei.robot.common.utils.AngleUtils.RADIANS_45;
import static org.xenei.robot.common.utils.AngleUtils.RADIANS_90;
import static org.xenei.robot.common.utils.DoubleUtils.SQRT2;

import java.util.Arrays;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.locationtech.jts.geom.Coordinate;
import org.xenei.robot.common.Location;

public class CoordUtilsTest {
    public static final double DELTA = 0.0000001;

    public static final int INPUT = 0;
    public static final int DEG = 1;
    public static final int RAD = 2;
    public static final int RANGE = 3;
    public static final int X = 4;
    public static final int Y = 5;

    public static double[][] arguments() {
        double range45 = SQRT2 / 2;
        return new double[][]{
                // @formatter:off
                //  input   deg     rad             range   X           Y          number
                {   0,      0,      0.0,            1.0,    1.0,        0.0 },      // 1
                {   45,     45,     RADIANS_45,     1.0,    range45,    range45 },  // 2
                {   90,     90,     RADIANS_90,     1.0,    0.0,        1.0 },      // 3
                {   135,    135,    RADIANS_135,    1.0,    -range45,   range45 },  // 4
                {   180,    180,    RADIANS_180,    1.0,   -1.0,        0.0 },      // 5
                {   225,    -135,   RADIANS_225,    1.0,    -range45,   -range45 }, // 6
                {   270,    -90,    RADIANS_270,    1.0,    0.0,        -1.0 },     // 7
                {   315,    -45,    RADIANS_315,    1.0,    range45,    -range45 }, // 8
                {   360,    0,      0.0,            1.0,    1.0,        0.0 },      // 9

                {   -360,   0,      0.0,            1.0,    1.0,        0.0 },      // 10
                {   -315,   45,     RADIANS_45,     1.0,    range45,    range45 },  // 11
                {   -270,   90,     RADIANS_90,     1.0,    0.0,        1.0 },      // 12
                {   -225,   135,    RADIANS_135,    1.0,    -range45,   range45 },  // 13
                {   -180,   -180,   RADIANS_180,    1.0,    -1.0,       0.0 },      // 14
                {   -135,   -135,   RADIANS_225,    1.0,    -range45,   -range45 }, // 15
                {   -90,    -90,    RADIANS_270,    1.0,    0.0,        -1.0 },     // 16
                {   -45,    -45,    RADIANS_315,    1.0,    range45,    -range45 }, // 17
                {   0,      0,      0.0,            1.0,    1.0,        0.0 }       // 18
                // @formatter:on
        };
    }

    private static Stream<Arguments> degreeParameters() {
        return Arrays.stream(arguments())
                .map(ary -> Arguments.of(ary[INPUT], ary[DEG], ary[RAD], ary[RANGE], ary[X], ary[Y]));
    }

    // @Test
    // public void angleToTest() {
    // Location c = new Coordinate(0, -2);
    // Coordinate p = new Coordinate(2,-1);
    // double angle2 = CoordUtils.angleTo(c, p);
    // assertEquals( 0.4636476090008062, angle2);
    // }

    @ParameterizedTest
    @MethodSource("degreeParameters")
    public void fromRadiansTest(double degrees, double thetaD, double thetaR, double range, double x, double y) {
        double radians = Math.toRadians(degrees);
        Coordinate underTest = CoordUtils.fromAngle(radians, range);
        assertEquals(x, underTest.getX(), DELTA);
        assertEquals(y, underTest.getY(), DELTA);
    }

    @ParameterizedTest
    @MethodSource("degreeParameters")
    public void fromXYTest(double degrees, double thetaD, double thetaR, double range, double x, double y) {
        Coordinate underTest = CoordUtils.fromAngle(thetaR, range);
        // fromXY never generates -180 so if degrees = -180 expect 180 instead
        if (thetaD == -180.0) {
            thetaD = 180;
            thetaR = Math.PI;
        }
        assertEquals(x, underTest.getX(), DELTA);
        assertEquals(y, underTest.getY(), DELTA);
    }

    @Test
    void minusTest() {
        Coordinate underTest = new Coordinate(4, 5);
        Coordinate other = new Coordinate(3, 5);
        Coordinate result = CoordUtils.minus(underTest, other);
        Assertions.assertEquals(1, result.getX());
        Assertions.assertEquals(0, result.getY());
    }

    @Test
    void plusTest() {
        Coordinate underTest = new Coordinate(4, 5);
        Coordinate other = new Coordinate(4, 5);
        Coordinate result = CoordUtils.plus(underTest, other);
        Assertions.assertEquals(8, result.getX());
        Assertions.assertEquals(10, result.getY());
    }

    @ParameterizedTest
    @MethodSource("triCoordinates")
    public void plusAndMinusTest(Coordinate a, Coordinate b, Coordinate c) {
        assertTrue(c.equals2D(CoordUtils.plus(a, b), 0.00001));
        assertTrue(c.equals2D(CoordUtils.plus(b, a), 0.00001));
        assertTrue(a.equals2D(CoordUtils.minus(c, b), 0.00001));
        assertTrue(b.equals2D(CoordUtils.minus(c, a), 0.00001));
    }

    private static Stream<Arguments> triCoordinates() {
        return Stream.of(Arguments.of(new Coordinate(-2, 3), new Coordinate(6, 1), new Coordinate(4, 4)),
                Arguments.of(new Coordinate(CoordUtils.fromAngle(0.7853981633974483, 2)),
                        new Coordinate(CoordUtils.fromAngle(0, 2.8284271247461903)),
                        new Coordinate(CoordUtils.fromAngle(0.3217505543966422, 4.47213595499958))),
                Arguments.of(new Coordinate(0, 0), new Coordinate(CoordUtils.fromAngle(RADIANS_45, 1)),
                        new Coordinate(CoordUtils.fromAngle(RADIANS_45, 1))));
    }

}
