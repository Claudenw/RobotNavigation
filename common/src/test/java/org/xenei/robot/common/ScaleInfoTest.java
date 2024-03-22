package org.xenei.robot.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class ScaleInfoTest {

    @ParameterizedTest(name = "{index} - {2}")
    @MethodSource("scaleParameters")
    public void scaleTest(ScaleInfo underTest, double expected, double arg) {
        assertEquals(expected, underTest.scale(arg));
    }

    private static Stream<Arguments> scaleParameters() {
        ScaleInfo underTest = ScaleInfo.builder().setResolution(0.5).setScale(1).build();
        List<Arguments> lst = new ArrayList<Arguments>();

        lst.add(Arguments.of(underTest, 3.0, 3.0));
        lst.add(Arguments.of(underTest, 3.5, 3.5));
        lst.add(Arguments.of(underTest, 3.5, 3.74));
        lst.add(Arguments.of(underTest, 4.0, 3.75));
        lst.add(Arguments.of(underTest, 4.0, 3.76));
        lst.add(Arguments.of(underTest, 3.5, 3.26));
        lst.add(Arguments.of(underTest, 3.5, 3.25));
        lst.add(Arguments.of(underTest, 3.0, 3.24));
        lst.add(Arguments.of(underTest, 1.0, 1.0));
        lst.add(Arguments.of(underTest, 1.0, 1.24));
        lst.add(Arguments.of(underTest, 1.5, 1.25));
        lst.add(Arguments.of(underTest, 1.5, 1.5));
        lst.add(Arguments.of(underTest, 1.5, 1.74));
        lst.add(Arguments.of(underTest, -1.0, -1.0));
        lst.add(Arguments.of(underTest, -1.0, -1.24));
        lst.add(Arguments.of(underTest, -1.5, -1.25));
        lst.add(Arguments.of(underTest, -1.5, -1.5));
        lst.add(Arguments.of(underTest, -1.5, -1.74));
        lst.add(Arguments.of(underTest, -2.0, -1.75));
        lst.add(Arguments.of(underTest, 0.0, 0.0));
        lst.add(Arguments.of(underTest, 0.0, -0.0));
        lst.add(Arguments.of(underTest, -4.0, -4.22360679774998));
        lst.add(Arguments.of(underTest, -1.5, -1.3881966011250098));
        return Stream.of(lst.toArray(new Arguments[0]));
    }

    @ParameterizedTest(name = "{index} - {2}")
    @MethodSource("precisionParameters")
    public void preciseTest(ScaleInfo underTest, double expected, double value) {
        assertEquals(expected, underTest.precise(value));
    }

    private static Stream<Arguments> precisionParameters() {
        ScaleInfo underTest = ScaleInfo.DEFAULT;
        List<Arguments> lst = new ArrayList<Arguments>();

        lst.add(Arguments.of(underTest, 3.0, 3.0));
        lst.add(Arguments.of(underTest, 3.5, 3.5));
        lst.add(Arguments.of(underTest, 3.7, 3.74));
        lst.add(Arguments.of(underTest, 3.8, 3.75));
        lst.add(Arguments.of(underTest, 3.8, 3.76));
        lst.add(Arguments.of(underTest, 3.3, 3.26));
        lst.add(Arguments.of(underTest, 3.3, 3.25));
        lst.add(Arguments.of(underTest, 3.2, 3.24));
        lst.add(Arguments.of(underTest, 1.0, 1.0));
        lst.add(Arguments.of(underTest, 1.2, 1.24));
        lst.add(Arguments.of(underTest, 1.3, 1.25));
        lst.add(Arguments.of(underTest, 1.5, 1.5));
        lst.add(Arguments.of(underTest, 1.7, 1.74));
        lst.add(Arguments.of(underTest, -1.0, -1.0));
        lst.add(Arguments.of(underTest, -1.2, -1.24));
        lst.add(Arguments.of(underTest, -1.3, -1.25));
        lst.add(Arguments.of(underTest, -1.5, -1.5));
        lst.add(Arguments.of(underTest, -1.7, -1.74));
        lst.add(Arguments.of(underTest, -1.8, -1.75));
        lst.add(Arguments.of(underTest, 0.0, 0.0));
        lst.add(Arguments.of(underTest, 0.0, -0.0));
        lst.add(Arguments.of(underTest, -4.2, -4.22360679774998));
        lst.add(Arguments.of(underTest, -1.4, -1.3881966011250098));
        return Stream.of(lst.toArray(new Arguments[0]));
    }
}
