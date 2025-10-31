package org.xenei.robot.common;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

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

public abstract class AbstractPositionTest extends AbstractLocationTest {

    private static final double[] angles = {0, RADIANS_45, RADIANS_90, RADIANS_135, RADIANS_180, RADIANS_225,
            RADIANS_270, RADIANS_315};

    abstract protected Position convertPosition(Position position);

    final protected Location convertLocation(Location location) {
        if (location instanceof Position) {
            return (Position) location;
        }
        return convertPosition(Position.asPosition(location, 0));
    }

    @ParameterizedTest
    @MethodSource("nextPositionTestData")
    void nextPositionTest(String name, Position position, Location relativeLocation, Position expectedPosition) {
        Position underTest = convertPosition(position);
        Position convertedExpectedPosition = convertPosition(expectedPosition);
        Position actualPosition  = underTest.nextPosition(relativeLocation);
        assertThat(actualPosition.getHeading()).describedAs("heading").isEqualTo(convertedExpectedPosition.getHeading(), withPrecision(tolerance()));
        assertThat(actualPosition.getX()).describedAs("x").isEqualTo(expectedPosition.getX(), withPrecision(tolerance()));
        assertThat(actualPosition.getY()).describedAs("y").isEqualTo(expectedPosition.getY(), withPrecision(tolerance()));
    }

    static Stream<Arguments> nextPositionTestData() {
        return PositionUtilsTest.nextPositionTestData();
    }

    @ParameterizedTest(name = "{index} {0}")
    @MethodSource("headingParameters")
    public void headingTest(Position position, Location coordinate, double expected) {
        Position underTest = convertPosition(position);
        Assertions.assertEquals(expected, underTest.headingTo(coordinate), ScaleInfo.DEFAULT.getResolution());
    }

    public static Stream<Arguments> headingParameters() {
        return PositionUtilsTest.headingParameters();
    }
}
