package org.xenei.robot.navigation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.xenei.robot.testUtils.DoubleUtils.DELTA;
import static org.xenei.robot.testUtils.DoubleUtils.RADIANS_135;
import static org.xenei.robot.testUtils.DoubleUtils.RADIANS_180;
import static org.xenei.robot.testUtils.DoubleUtils.RADIANS_225;
import static org.xenei.robot.testUtils.DoubleUtils.RADIANS_270;
import static org.xenei.robot.testUtils.DoubleUtils.RADIANS_315;
import static org.xenei.robot.testUtils.DoubleUtils.RADIANS_45;
import static org.xenei.robot.testUtils.DoubleUtils.RADIANS_90;
import static org.xenei.robot.testUtils.DoubleUtils.SQRT2;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.math3.util.Precision;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.xenei.robot.utils.DoubleUtils;

public class PositionTest {

    private Position initial;

    @BeforeEach
    public void setup() {
        initial = new Position(0.0, 0.0);
    }

    @Test
    public void zigZagTest() {
        Coordinates cmd = Coordinates.fromDegrees(45, 2);
        Position nxt = initial.nextPosition(cmd);
        assertEquals(RADIANS_45, nxt.getHeadingRadians(), DELTA);
        assertEquals(SQRT2, nxt.coordinates().getX(), DELTA);
        assertEquals(SQRT2, nxt.coordinates().getY(), DELTA);

        cmd = Coordinates.fromXY(2, 0);
        nxt = nxt.nextPosition(cmd);

        assertEquals(SQRT2 + 2, nxt.coordinates().getX(), DELTA);
        assertEquals(SQRT2, nxt.coordinates().getY(), DELTA);
        assertEquals(0.0, nxt.getHeadingRadians(), DELTA);
    }

    @Test
    public void boxTest() {
        Coordinates cmd = Coordinates.fromDegrees(45, 2);
        Position nxt = initial.nextPosition(cmd);
        assertEquals(RADIANS_45, nxt.getHeadingRadians(), DELTA);
        assertEquals(SQRT2, nxt.coordinates().getX(), DELTA);
        assertEquals(SQRT2, nxt.coordinates().getY(), DELTA);

        cmd = Coordinates.fromDegrees(-45, 2);
        nxt = nxt.nextPosition(cmd);
        assertEquals(-45, nxt.getHeadingDegrees(), DELTA);
        assertEquals(SQRT2 * 2, nxt.coordinates().getX(), DELTA);
        assertEquals(0.0, nxt.coordinates().getY(), DELTA);

        cmd = Coordinates.fromDegrees(-135, 2);
        nxt = nxt.nextPosition(cmd);
        assertEquals(-135, nxt.getHeadingDegrees(), DELTA);
        assertEquals(SQRT2, nxt.coordinates().getX(), DELTA);
        assertEquals(-SQRT2, nxt.coordinates().getY(), DELTA);

        cmd = Coordinates.fromDegrees(135, 2);
        nxt = nxt.nextPosition(cmd);
        assertEquals(135, nxt.getHeadingDegrees(), DELTA);
        assertEquals(0, nxt.coordinates().getX(), DELTA);
        assertEquals(0, nxt.coordinates().getY(), DELTA);
    }

    @ParameterizedTest(name = "{index} {1}/{2} {3}")
    @MethodSource("collisionParameters")
    public void collisionTest(boolean state, double x, double y, double heading, double targX, double targY) {
        Position pos = new Position(Coordinates.fromXY(x, y), heading);
        double headeingD = Math.toDegrees(heading);
        Coordinates target = Coordinates.fromXY(targX, targY);
        if (state) {
            assertTrue(pos.checkCollision(target, 0.57, 10),
                    () -> String.format("Did not collide with %s/%s", target.getX(), target.getY()));
        } else {
            assertFalse(pos.checkCollision(target, 0.57, 10),
                    () -> String.format("Did collide with %s/%s", target.getX(), target.getY()));
        }
    }

    private static void addCollisionArgs(List<Arguments> args, boolean state , double x, double y, double heading, double[] targ) {
        double[] offsets = { -.4, 0, .4 };
        for (double deltax : offsets) {
            for (double deltay : offsets) {
                args.add(Arguments.of(state, x, y, heading, x + targ[0] + deltax, y + targ[1] + deltay));
            }
        }
    }

    private static Stream<Arguments> collisionParameters() {

        double[] deg0 = { 1, 0, 0 };
        double[] deg45 = { 1, 1, RADIANS_45 };
        double[] deg90 = { 0, 1, RADIANS_90 };
        double[] deg135 = { -1, 1, RADIANS_135 };
        double[] deg180 = { -1, 0, RADIANS_180 };
        double[] deg225 = { -1, -1, RADIANS_225 };
        double[] deg270 = { 0, -1, RADIANS_270 };
        double[] deg315 = { 1, -1, RADIANS_315 };

        List<double[]> targets = List.of(deg0, deg45, deg90, deg135, deg180, deg225, deg270, deg315);
        List<Arguments> args = new ArrayList<>();
        for (int origx = -1; origx < 2; origx++) {
            double x = origx;
            for (int origy = -1; origy < 2; origy++) {
                double y = origy;
                for (int targIdx = 0; targIdx < targets.size(); targIdx++) {
                    double[] data = targets.get(targIdx);
                    double heading = data[2];
                    addCollisionArgs(args, true, x, y, heading, targets.get(targIdx));
                    for (int notTarg=2;notTarg<8;notTarg+=2) {
                        int idx = (targIdx+notTarg)%8;
                            addCollisionArgs(args, false, x, y, heading, targets.get(idx));
                    }

                }
            }
        }
        return args.stream();
    }

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
