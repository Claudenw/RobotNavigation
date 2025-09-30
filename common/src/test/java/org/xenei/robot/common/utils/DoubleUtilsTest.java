package org.xenei.robot.common.utils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;

import org.apache.commons.math3.util.Precision;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class DoubleUtilsTest {

    @ParameterizedTest
    @MethodSource("rangeParameters")
    public void inRangeTest(boolean state, double a, double b, double range) {
        if (state) {
            assertTrue(DoubleUtils.inRange(a, b, range));
        } else {
            assertFalse(DoubleUtils.inRange(a, b, range));
        }
    }

    private static Stream<Arguments> rangeParameters() {
        return Stream.of(Arguments.of(true, 0, 1, 1), Arguments.of(true, 0, .5, 1), Arguments.of(true, -1, -1, 1),
                Arguments.of(false, -1, 1, 1), Arguments.of(false, .5, 1.1, .5),
                Arguments.of(true, -1.2246467991473532E-16, 0.0, 0.5), Arguments.of(true, 0.0, Precision.EPSILON, 0.0),
                Arguments.of(true, 0.0, -Precision.EPSILON, 0.0));
    }
}
