package org.xenei.robot.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.xenei.robot.common.utils.AngleUtils.RADIANS_180;
import static org.xenei.robot.common.utils.AngleUtils.RADIANS_45;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.locationtech.jts.geom.Coordinate;
import org.xenei.robot.common.testUtils.CoordinateUtils;
import org.xenei.robot.common.utils.AngleUtils;
import org.xenei.robot.common.utils.CoordUtils;
import org.xenei.robot.common.utils.CoordUtilsTest;

public class FrontsCoordinateTest {
    private FrontsCoordinate underTest = new FrontsCoordinate() {

        @Override
        public UnmodifiableCoordinate getCoordinate() {
            return UnmodifiableCoordinate.make(new Coordinate(4,5));
        }};

    private static FrontsCoordinate make(Coordinate c) {
        return new FrontsCoordinate() {

            @Override
            public UnmodifiableCoordinate getCoordinate() {
                return UnmodifiableCoordinate.make(c);
            }};
    }
    
    private static FrontsCoordinate make(double x, double y) {
        return make(new Coordinate(x,y));
    }
    
    @Test
    public void getCoordinateTest() {
        CoordinateUtils.assertEquivalent( new Coordinate(4,5), underTest);
    }

    @Test
    public void getXTest() {
        assertEquals(4, underTest.getX());
    }

    @Test
    public void getYTest() {
        assertEquals(5, underTest.getY());
    }

    @Test
    public void equals2DTest() {
        Coordinate other = new Coordinate(4,5);
        assertTrue( underTest.equals2D(other) );
        assertTrue( underTest.equals2D(make(other)) );
        other = new Coordinate(4.5, 4.5);
        assertTrue( underTest.equals2D(other, .5));
        assertTrue( underTest.equals2D(make(other), .5));
    }

    @Test
    public void compareToTest() {
        Coordinate other = new Coordinate(4,5);
        assertEquals( 0, underTest.compareTo(other) );
        assertEquals( 0, underTest.compareTo(make(other)) );
        other = new Coordinate(4.5, 4.5);
        assertEquals( -1, underTest.compareTo(other) );
        assertEquals( -1, underTest.compareTo(make(other)) );
        other = new Coordinate(3.5, 4.5);
        assertEquals( 1, underTest.compareTo(other) );
        assertEquals( 1, underTest.compareTo(make(other)) );
    }

    @Test
    public void distanceTest() {
        Coordinate other = new Coordinate(6,5);
        assertEquals( 2, underTest.distance(other) );
        assertEquals( 2, underTest.distance(make(other)) );
    }

    @Test
    public void angleBetweenTest() {
        Coordinate other = new Coordinate(6,6);
        assertEquals( 0.463647609000806, underTest.angleBetween(other), AngleUtils.TOLERANCE );
        assertEquals( 0.463647609000806, underTest.angleBetween(make(other)), AngleUtils.TOLERANCE );
    }

    @Test
    public void minusTest() {
        Coordinate other = new Coordinate(3,5);
        Coordinate result = underTest.minus(other);
        assertEquals( 1, result.getX());
        assertEquals( 0, result.getY());
        result = underTest.minus(make(other));
        assertEquals( 1, result.getX());
        assertEquals( 0, result.getY());
    }

    @Test
    public void plusTest() {
        Coordinate other = new Coordinate(4,5);
        Coordinate result = underTest.plus(other);
        assertEquals( 8, result.getX());
        assertEquals( 10, result.getY());
        result = underTest.plus(make(other));
        assertEquals( 8, result.getX());
        assertEquals( 10, result.getY());
    }
    
    @Test
    public void nearTest() {
        Coordinate other = new Coordinate(6,5);
        assertTrue(underTest.near(other, 5));
        assertTrue(underTest.near(other, 4));
        assertTrue(underTest.near(other, 6));
        
        assertTrue(underTest.near(make(other), 5));
        assertTrue(underTest.near(make(other), 4));
        assertTrue(underTest.near(make(other), 6));
    }
    
    @ParameterizedTest
    @MethodSource("coordPairParameters")
    public void distanceTest(FrontsCoordinate a, FrontsCoordinate b, double expected, double angle) {
        assertEquals(expected, a.distance(b), AngleUtils.TOLERANCE);
        assertEquals(expected, b.distance(a), AngleUtils.TOLERANCE);
        assertEquals(0.0, a.distance(a), AngleUtils.TOLERANCE);
        assertEquals(0.0, b.distance(b), AngleUtils.TOLERANCE);
    }

    @ParameterizedTest
    @MethodSource("coordPairParameters")
    public void angleBetweenTest(FrontsCoordinate a, FrontsCoordinate b, double expected, double angle) {
        assertEquals(AngleUtils.normalize(RADIANS_180 + angle), a.angleBetween(b), AngleUtils.TOLERANCE);
        assertEquals(angle, b.angleBetween(a), AngleUtils.TOLERANCE);
        assertEquals(0.0, a.angleBetween(a), AngleUtils.TOLERANCE);
        assertEquals(0.0, b.angleBetween(b), AngleUtils.TOLERANCE);
    }

    private static void processStream(List<Arguments> lst, double[] args) {
        FrontsCoordinate l = make(args[CoordUtilsTest.X], args[CoordUtilsTest.Y]);
        lst.add(Arguments.of(l, FrontsCoordinate.ORIGIN, args[CoordUtilsTest.RANGE], args[CoordUtilsTest.RAD]));

        l = make(CoordUtils.fromAngle(args[CoordUtilsTest.RAD], args[CoordUtilsTest.RANGE]));
        lst.add(Arguments.of(l, FrontsCoordinate.ORIGIN, args[CoordUtilsTest.RANGE], args[CoordUtilsTest.RAD]));

        l = make(CoordUtils.fromAngle(Math.toRadians(args[CoordUtilsTest.DEG]), args[CoordUtilsTest.RANGE]));
        lst.add(Arguments.of(l, FrontsCoordinate.ORIGIN, args[CoordUtilsTest.RANGE], args[CoordUtilsTest.RAD]));

    }

    private static Stream<Arguments> coordPairParameters() {

        List<Arguments> lst = new ArrayList<Arguments>();

        Arrays.stream(CoordUtilsTest.arguments()).forEach(s -> processStream(lst, s));

        return Stream.of(lst.toArray(new Arguments[0]));
    }

    @ParameterizedTest
    @MethodSource("triCoordinates")
    public void plusAndMinusTest(FrontsCoordinate a, FrontsCoordinate b, FrontsCoordinate c) {
        assertTrue(c.equals2D(a.plus(b), 0.00001));
        assertTrue(c.equals2D(b.plus(a), 0.00001));
        assertTrue(a.equals2D(c.minus(b), 0.00001));
        assertTrue(b.equals2D(c.minus(a), 0.00001));
    }

    private static Stream<Arguments> triCoordinates() {
        return Stream.of(Arguments.of(make(-2, 3), make(6, 1), make(4, 4)),
                Arguments.of(make(CoordUtils.fromAngle(0.7853981633974483, 2)),
                        make(CoordUtils.fromAngle(0, 2.8284271247461903)),
                        make(CoordUtils.fromAngle(0.3217505543966422, 4.47213595499958))),
                Arguments.of(make(0, 0), make(CoordUtils.fromAngle(RADIANS_45, 1)),
                        make(CoordUtils.fromAngle(RADIANS_45, 1))));
    }
}
