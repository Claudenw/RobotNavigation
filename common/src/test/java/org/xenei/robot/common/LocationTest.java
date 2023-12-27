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

    @ParameterizedTest
    @MethodSource("coordPairParameters")
    public void rangeAndThetaTest(Location a, double expected, double angle) {
        assertEquals(expected, a.range(), AngleUtils.TOLERANCE);
        assertEquals(angle, a.theta(), AngleUtils.TOLERANCE);
    }

    private static void processStream(List<Arguments> lst, double[] args) {
        Location l = Location.from(args[CoordUtilsTest.X], args[CoordUtilsTest.Y]);
        lst.add(Arguments.of(l, args[CoordUtilsTest.RANGE], args[CoordUtilsTest.RAD]));

        l = Location.from(CoordUtils.fromAngle(args[CoordUtilsTest.RAD], args[CoordUtilsTest.RANGE]));
        lst.add(Arguments.of(l, args[CoordUtilsTest.RANGE], args[CoordUtilsTest.RAD]));

        l = Location.from(CoordUtils.fromAngle(Math.toRadians(args[CoordUtilsTest.DEG]), args[CoordUtilsTest.RANGE]));
        lst.add(Arguments.of(l, args[CoordUtilsTest.RANGE], args[CoordUtilsTest.RAD]));

    }

    private static Stream<Arguments> coordPairParameters() {

        List<Arguments> lst = new ArrayList<Arguments>();

        Arrays.stream(CoordUtilsTest.arguments()).forEach(s -> processStream(lst, s));

        return Stream.of(lst.toArray(new Arguments[0]));
    }
}
