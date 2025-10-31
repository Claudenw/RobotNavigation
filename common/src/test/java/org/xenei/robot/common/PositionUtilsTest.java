package org.xenei.robot.common;

import org.apache.commons.math3.util.Precision;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.locationtech.jts.geom.Coordinate;
import org.xenei.robot.common.mapping.ThetaAndRange;
import org.xenei.robot.common.utils.AngleUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.withPrecision;
import static org.xenei.robot.common.utils.AngleUtils.RADIANS_135;
import static org.xenei.robot.common.utils.AngleUtils.RADIANS_180;
import static org.xenei.robot.common.utils.AngleUtils.RADIANS_225;
import static org.xenei.robot.common.utils.AngleUtils.RADIANS_270;
import static org.xenei.robot.common.utils.AngleUtils.RADIANS_315;
import static org.xenei.robot.common.utils.AngleUtils.RADIANS_45;
import static org.xenei.robot.common.utils.AngleUtils.RADIANS_90;
import static org.xenei.robot.common.utils.DoubleUtils.SQRT2;

public class PositionUtilsTest {
    
    public static final double[] ANGLES = {0, RADIANS_45, RADIANS_90, RADIANS_135, RADIANS_180, RADIANS_225,
            RADIANS_270, RADIANS_315};

    private final static double TOLERANCE = 0.000000000001;

    private static Location makeLoc(double x, double y) {
        return Location.asLocation(new Coordinate(x, y));
    }

    private static Position makePos(double x, double y, double heading) {
        return Position.asPosition(new Coordinate(x, y), heading);
    }

    /**
     * Returns arguments of the form
     * name Position relativeLocation, expectedPosition
     * @return a Stream of Arguments
     */
    public static Stream<Arguments> nextPositionTestData() {
        List<Arguments> args = new ArrayList<>();

        // BOX TEST DATA
        Position position = Position.asPosition(Location.ORIGIN, 0);
        Location relativeLocation = new ThetaAndRange(RADIANS_45, 2);
        Position expectedPosition = Position.asPosition(new Coordinate(SQRT2, SQRT2), RADIANS_45);
        args.add(Arguments.of("box", position, relativeLocation, expectedPosition));
        position = expectedPosition;
        relativeLocation = new ThetaAndRange(RADIANS_90, 2);
        expectedPosition = Position.asPosition(new Coordinate(0.0, SQRT2 * 2), RADIANS_135);
        args.add(Arguments.of("box", position, relativeLocation, expectedPosition));
        position = expectedPosition;
        // same relative position.
        expectedPosition = Position.asPosition(new Coordinate(-SQRT2, SQRT2), RADIANS_225);
        args.add(Arguments.of("box", position, relativeLocation, expectedPosition));

        position = expectedPosition;
        // same relative position.
        expectedPosition = Position.asPosition(new Coordinate(0, 0), RADIANS_315);
        args.add(Arguments.of("box", position, relativeLocation, expectedPosition));

        // ZIGZAG Test data
        position = Position.asPosition(Location.ORIGIN, 0);
        relativeLocation = new ThetaAndRange(RADIANS_45, 2);
        expectedPosition = Position.asPosition(new Coordinate(SQRT2, SQRT2), RADIANS_45);
        args.add(Arguments.of("zigzag", position, relativeLocation, expectedPosition));

        position = expectedPosition;
        relativeLocation = new ThetaAndRange(-RADIANS_45, 2);
        expectedPosition = Position.asPosition(new Coordinate(SQRT2 + 2, SQRT2), 0);
        args.add(Arguments.of("zigzag", position, relativeLocation, expectedPosition));

        // standard position tests
        args.add(Arguments.of("fwd one", Position.asPosition(Location.ORIGIN, 0), makeLoc(1, 0), makePos(1, 0, 0)));
        args.add(Arguments.of("fwd up", Position.asPosition(Location.ORIGIN, 0), makeLoc(1, 1), makePos(1, 1, RADIANS_45)));
        args.add(Arguments.of("up", Position.asPosition(Location.ORIGIN, 0), makeLoc(0, 1), makePos(0, 1, RADIANS_90)));
        args.add(Arguments.of("back up", Position.asPosition(Location.ORIGIN, 0), makeLoc(-1, 1), makePos(-1, 1, RADIANS_135)));
        args.add(Arguments.of("back", Position.asPosition(Location.ORIGIN, 0), makeLoc(-1, 0), makePos(-1, 0, RADIANS_180)));
        args.add(Arguments.of("back down", Position.asPosition(Location.ORIGIN, 0), makeLoc(-1, -1), makePos(-1, -1, RADIANS_225)));
        args.add(Arguments.of("down", Position.asPosition(Location.ORIGIN, 0), makeLoc(0, -1), makePos(0, -1, RADIANS_270)));
        args.add(Arguments.of("fwd down", Position.asPosition(Location.ORIGIN, 0), makeLoc(1, -1), makePos(1, -1, RADIANS_315)));

        args.add(Arguments.of("EPSILON suite", Position.asPosition(Location.ORIGIN, 0), makeLoc(Precision.EPSILON, 0), makePos(Precision.EPSILON, 0, 0)));
        args.add(Arguments.of("EPSILON suite", Position.asPosition(Location.ORIGIN, 0), makeLoc(Precision.EPSILON, Precision.EPSILON),
                makePos(Precision.EPSILON, Precision.EPSILON, RADIANS_45)));
        args.add(Arguments.of("EPSILON suite 1", Position.asPosition(Location.ORIGIN, 0), makeLoc(0, Precision.EPSILON),
                makePos(0, Precision.EPSILON, RADIANS_90)));
        args.add(Arguments.of("EPSILON suite 1", Position.asPosition(Location.ORIGIN, 0), makeLoc(-Precision.EPSILON, Precision.EPSILON),
                makePos(-Precision.EPSILON, Precision.EPSILON, RADIANS_135)));
        args.add(Arguments.of("EPSILON suite 1", Position.asPosition(Location.ORIGIN, 0), makeLoc(-Precision.EPSILON, 0),
                makePos(-Precision.EPSILON, 0, RADIANS_180)));
        args.add(Arguments.of("EPSILON suite 1", Position.asPosition(Location.ORIGIN, 0), makeLoc(-Precision.EPSILON, -Precision.EPSILON),
                makePos(-Precision.EPSILON, -Precision.EPSILON, RADIANS_225)));
        args.add(Arguments.of("EPSILON suite 1", Position.asPosition(Location.ORIGIN, 0), makeLoc(0, -Precision.EPSILON),
                makePos(0, -Precision.EPSILON, RADIANS_270)));
        args.add(Arguments.of("EPSILON suite 1", Position.asPosition(Location.ORIGIN, 0), makeLoc(Precision.EPSILON, -Precision.EPSILON),
                makePos(Precision.EPSILON, -Precision.EPSILON, RADIANS_315)));

        args.add(Arguments.of("(1,3, 90deg) + (.5,3)", makePos(-1, -3, RADIANS_90), makeLoc(0.5, 3.0), makePos(-4.0, -2.5, 2.976443976175166)));

        return args.stream();
    }

    @ParameterizedTest
    @MethodSource("nextPositionTestData")
    public void nextPositionTest(String name, Position position, Location relativeLocation, Position expectedPosition) {
        Position actualPosition  = Position.PositionUtils.nextPosition(position, relativeLocation);
        assertThat(actualPosition.getHeading()).describedAs("heading").isEqualTo(expectedPosition.getHeading(), withPrecision(AngleUtils.TOLERANCE));
        assertThat(actualPosition.getX()).describedAs("x").isEqualTo(expectedPosition.getX(), withPrecision(TOLERANCE));
        assertThat(actualPosition.getY()).describedAs("y").isEqualTo(expectedPosition.getY(), withPrecision(TOLERANCE));
    }

    @ParameterizedTest(name = "{index} {0}")
    @MethodSource("headingParameters")
    public void headingTest(Position position, Location coordinate, double expected) {
        Assertions.assertEquals(expected, Position.PositionUtils.headingTo(position, coordinate), ScaleInfo.DEFAULT.getResolution());
    }

    public static Stream<Arguments> headingParameters() {
        List<Arguments> lst = new ArrayList<>();
        lst.add(Arguments.arguments(makePos(-1, -3, 0), makeLoc(-1, -1), RADIANS_90));
        lst.add(Arguments.arguments(makePos(-1, -3, RADIANS_90), makeLoc(-1, -1), RADIANS_90));
        lst.add(Arguments.arguments(makePos(-1, -3, -RADIANS_90), makeLoc(-1, -1), RADIANS_90));
        lst.add(Arguments.arguments(makePos(-1, -3, RADIANS_45), makeLoc(-1, -1), RADIANS_90));
        lst.add(Arguments.arguments(makePos(-1, 1, RADIANS_45), makeLoc(1, 3), RADIANS_45));
        return lst.stream();
    }

//    private static Arguments makeRelativeLocArguments(Position position, Location location) {
//        double heading = Position.PositionUtils.headingTo(position, location);
//        return Arguments.arguments(position, location, heading);
//    }
//
//    public static Stream<Arguments> relativeLocationTestData() {
//        int[] idx = {100, 50, -50, -100};
//        List<Arguments> lst = new ArrayList<>();
//        for (double d : ANGLES) {
//            for (int x : idx) {
//                for (int y : idx) {
//                    lst.add(makeRelativeLocArguments(makePos(50, 50, d), makeLoc(x, y)));
//                }
//            }
//        }
//        lst.add(makeRelativeLocArguments(makePos(-2, -2, RADIANS_180), makeLoc(-1, 1)));
//        return lst.stream();
//    }
//
//    @ParameterizedTest(name = "{index} {0} {1}")
//    @MethodSource("relativeLocationTestData")
//    public void relativeLocationTest(Position position, Location absolute, double heading) {
//        Location relative = Position.PositionUtils.relativeLocation(position, absolute);
//        Position p2 = position.nextPosition(relative);
//        CoordinateUtils.assertEquivalent(absolute, p2, TOLERANCE);
//        Assertions.assertEquals(AngleUtils.normalize(heading), AngleUtils.normalize(p2.getHeading()),
//                AngleUtils.TOLERANCE);
//    }
}
