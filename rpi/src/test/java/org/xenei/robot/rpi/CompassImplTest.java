package org.xenei.robot.rpi;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.ScaleInfo;
import org.xenei.robot.common.utils.AngleUtils;

public class CompassImplTest {

    @ParameterizedTest
    @MethodSource("coordPairParameters")
    public void headingText(double x, double y, double angle) {
        assertEquals(angle, CompassImpl.heading(x, y), AngleUtils.TOLERANCE);
    }

    private static void processStream(List<Arguments> lst, double[] args) {
        Location l = Location.from(args[CoordUtilsTest.X], args[CoordUtilsTest.Y]);
        lst.add(Arguments.of(args[CoordUtilsTest.X], args[CoordUtilsTest.Y], args[CoordUtilsTest.RAD]));
        ;
    }

    private static Stream<Arguments> coordPairParameters() {

        List<Arguments> lst = new ArrayList<Arguments>();

        Arrays.stream(CoordUtilsTest.arguments()).forEach(s -> processStream(lst, s));

        return Stream.of(lst.toArray(new Arguments[0]));
    }

    @Test
    public void x() {
        ScaleInfo info = ScaleInfo.DEFAULT;
        System.out.println( info.decimalPlaces() );
        System.out.println( info.round(0.123456789));
    }
}
