package org.xenei.robot.common.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.math3.util.Precision;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class AngleUtilsTest {

    @ParameterizedTest
    @MethodSource("angleParameters")
    public void normalizeTest(double arg, double expected) {
        assertEquals(expected, AngleUtils.normalize(arg), AngleUtils.TOLERANCE);
    }

    private static Stream<Arguments> angleParameters() {
        double incr = Math.PI / 7;
        double position = -2 * Math.PI;
        double expected = 0;
        List<Arguments> lst = new ArrayList<Arguments>();
        lst.add(Arguments.of(0, 0));
        lst.add(Arguments.of(-0, -0));
        lst.add(Arguments.of(-Math.PI, Math.PI));
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
            if (expected <= -Math.PI) {
                expected += 2 * Math.PI;
            }
        } while (position > -2 * Math.PI);

        return Stream.of(lst.toArray(new Arguments[0]));
    }

}
