package org.xenei.robot.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;

import static org.xenei.robot.common.utils.AngleUtils.RADIANS_180;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
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

public abstract class AbstractLocationTest {

    abstract protected double tolerance();
    abstract protected Location convertLocation(Location location);

    private Location underTest;

    final protected Location makeUnderTest(double x, double y) {
        return convertLocation(Location.asLocation(new Coordinate(x, y)));
    }

    @BeforeEach
    void setup() {
        underTest = makeUnderTest(4, 5);
    }

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
        double diff = 2 * tolerance();
        other = Location.asLocation(new Coordinate(4 + diff, 4 + diff));
        assertFalse(underTest.equals2D(other));
    }

    @Test
    void compareToTest() {
        Location other = Location.asLocation(new Coordinate(4, 5));
        assertEquals(0, underTest.compareTo(other));
        double diff = 2 * tolerance();
        other = Location.asLocation(new Coordinate(4 + diff, 5 - diff));
        assertEquals(-1, underTest.compareTo(other));
        other = Location.asLocation(new Coordinate(4 - diff, 5 - diff));
        assertEquals(1, underTest.compareTo(other));
    }

    @Test
    void orderingTest() {
        double diff = 2 * tolerance();
        Location[] expected = { makeUnderTest(-diff, -diff),
                makeUnderTest(-diff, 0),
                makeUnderTest(-diff, diff),
                makeUnderTest(0, -diff),
                makeUnderTest(0, 0),
                makeUnderTest(0, diff),
                makeUnderTest(diff, -diff),
                makeUnderTest(diff, 0),
                makeUnderTest(diff, diff)};

        List<Location> actual = new ArrayList<>(List.of(expected));
        Collections.shuffle(actual);
        assertThat(actual).isNotEqualTo(Arrays.asList(expected));
        actual.sort(Location::compareTo);
        assertThat(actual).containsExactly(expected);
    }

    @Test
    void distanceTest() {
        Location other = Location.asLocation(new Coordinate(6, 5));
        assertEquals(2, underTest.distance(other));
    }

    @Test
    void angleBetweenTest() {
        Location other = Location.asLocation(new Coordinate(6, 6));
        assertEquals(0.463647609000806, underTest.angleBetween(other), tolerance());
    }

    @ParameterizedTest
    @MethodSource("coordPairParameters")
    void distanceTest(Location a, Location b, double expected, double ignored) {
        assertEquals(expected, convertLocation(a).distance(b), tolerance());
        assertEquals(expected, convertLocation(b).distance(a), tolerance());
        assertEquals(0.0, convertLocation(a).distance(a), tolerance());
        assertEquals(0.0, convertLocation(b).distance(b), tolerance());
    }

    @ParameterizedTest
    @MethodSource("coordPairParameters")
    void angleBetweenTest(Location a, Location b, double ignored, double angle) {
        Location convertedA = convertLocation(a);
        Location convertedB = convertLocation(b);
        assertEquals(AngleUtils.normalize(RADIANS_180 + angle), convertedA.angleBetween(convertedB), tolerance());
        assertEquals(angle, convertedB.angleBetween(convertedA), tolerance());
        assertEquals(0.0, convertedA.angleBetween(convertedA), tolerance());
        assertEquals(0.0, convertedB.angleBetween(convertedB), tolerance());
    }

    @ParameterizedTest
    @MethodSource("coordPairParameters")
    public void rangeAndThetaTest(Location a, Location ignored, double expected, double angle) {
        Location converted = convertLocation(a);
        assertEquals(expected, converted.range(), tolerance());
        assertEquals(angle, converted.theta(), tolerance());
    }

    @ParameterizedTest
    @MethodSource("locationArgs")
    public void serdeTest(Location arg) {
        final Location expected = convertLocation(arg);
        final Location.Serde serde = new Location.Serde();
        final byte[] buffer = serde.serialize(expected);
        final Location actual = serde.deserialize(buffer);
        assertThat(actual.getX()).isEqualTo(expected.getX());
        assertThat(actual.getY()).isEqualTo(expected.getY());
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

    private static Stream<Location> locationArgs() {
        return Arrays.stream(CoordUtilsTest.arguments()).map(args ->
                Location.asLocation(new Coordinate(args[CoordUtilsTest.X], args[CoordUtilsTest.Y])));
    }

    private static Stream<Arguments> coordPairParameters() {

            List<Arguments> lst = new ArrayList<>();
        Arrays.stream(CoordUtilsTest.arguments()).forEach(s -> processStream(lst, s));

        return Stream.of(lst.toArray(new Arguments[0]));
    }
}
