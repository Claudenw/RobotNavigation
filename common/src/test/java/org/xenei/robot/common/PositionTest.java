package org.xenei.robot.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.xenei.robot.common.utils.AngleUtils.RADIANS_135;
import static org.xenei.robot.common.utils.AngleUtils.RADIANS_180;
import static org.xenei.robot.common.utils.AngleUtils.RADIANS_225;
import static org.xenei.robot.common.utils.AngleUtils.RADIANS_270;
import static org.xenei.robot.common.utils.AngleUtils.RADIANS_315;
import static org.xenei.robot.common.utils.AngleUtils.RADIANS_45;
import static org.xenei.robot.common.utils.AngleUtils.RADIANS_90;
import static org.xenei.robot.common.utils.DoubleUtils.SQRT2;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.math3.util.Precision;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.locationtech.jts.geom.Coordinate;
import org.xenei.robot.common.testUtils.CoordinateUtils;
import org.xenei.robot.common.testUtils.TestChassisInfo;
import org.xenei.robot.common.utils.AngleUtils;
import org.xenei.robot.common.utils.CoordUtils;
import org.xenei.robot.common.utils.RobutContext;

public class PositionTest {

    private final static double TOLERANCE = 0.000000000001;

    private static double[] angles = { 0, RADIANS_45, RADIANS_90, RADIANS_135, RADIANS_180, RADIANS_225, RADIANS_270,
            RADIANS_315 };

    private Position initial;
    private static RobutContext ctxt = new RobutContext(ScaleInfo.DEFAULT, TestChassisInfo.DEFAULT);

    @BeforeEach
    public void setup() {
        initial = Position.from(0.0, 0.0);
    }

    @Test
    public void zigZagTest() {
        Location cmd = Location.from(CoordUtils.fromAngle(RADIANS_45, 2));
        Position nxt = initial.nextPosition(cmd);
        assertEquals(RADIANS_45, nxt.getHeading(), AngleUtils.TOLERANCE);
        assertEquals(SQRT2, nxt.getX(), TOLERANCE);
        assertEquals(SQRT2, nxt.getY(), TOLERANCE);

        cmd = Location.from(CoordUtils.fromAngle(-RADIANS_45, 2));
        nxt = nxt.nextPosition(cmd);

        assertEquals(SQRT2 + 2, nxt.getX(), TOLERANCE);
        assertEquals(SQRT2, nxt.getY(), TOLERANCE);
        assertEquals(0.0, nxt.getHeading(), AngleUtils.TOLERANCE);
    }

    @Test
    public void boxTest() {
        Location cmd = Location.from(CoordUtils.fromAngle(RADIANS_45, 2));
        Position nxt = initial.nextPosition(cmd);
        assertEquals(RADIANS_45, nxt.getHeading(), AngleUtils.TOLERANCE);
        assertEquals(SQRT2, nxt.getX(), TOLERANCE);
        assertEquals(SQRT2, nxt.getY(), TOLERANCE);

        cmd = Location.from(CoordUtils.fromAngle(RADIANS_90, 2));
        nxt = nxt.nextPosition(cmd);
        assertEquals(RADIANS_135, nxt.getHeading(), AngleUtils.TOLERANCE);
        assertEquals(0.0, nxt.getX(), TOLERANCE);
        assertEquals(SQRT2 * 2, nxt.getY(), TOLERANCE);

        nxt = nxt.nextPosition(cmd);
        assertEquals(RADIANS_225, nxt.getHeading(), AngleUtils.TOLERANCE);
        assertEquals(-SQRT2, nxt.getX(), TOLERANCE);
        assertEquals(SQRT2, nxt.getY(), TOLERANCE);

        nxt = nxt.nextPosition(cmd);
        assertEquals(RADIANS_315, nxt.getHeading(), AngleUtils.TOLERANCE);
        assertEquals(0, nxt.getX(), TOLERANCE);
        assertEquals(0, nxt.getY(), TOLERANCE);
    }

    @ParameterizedTest(name = "{index} {0}")
    @MethodSource("collisionParameters")
    public void collisionTest(String name, boolean state, Position pos, Coordinate target) {
        if (state) {
            assertTrue(pos.checkCollision(ctxt, target, ctxt.chassisInfo.radius),
                    () -> String.format("Did not collide with %s/%s", target.getX(), target.getY()));
        } else {
            assertFalse(pos.checkCollision(ctxt, target, ctxt.chassisInfo.radius),
                    () -> String.format("Did collide with %s/%s", target.getX(), target.getY()));
        }
    }

    private static Stream<Arguments> collisionParameters() {
        double[] angles = { 0, RADIANS_45, RADIANS_90, RADIANS_135, RADIANS_180, RADIANS_225, RADIANS_270,
                RADIANS_315 };
        List<Arguments> args = new ArrayList<>();

        for (double angle : angles) {
            for (double heading : angles) {
                Coordinate c = CoordUtils.fromAngle(angle, 10);
                Position p = Position.from(0, 0, heading);
                args.add(Arguments.of(String.format("%s/%s", Math.toDegrees(heading), Math.toDegrees(angle)),
                        angle == heading, p, c));
            }
        }
        return args.stream();
    }

    @ParameterizedTest(name = "{index} {0}")
    @MethodSource("nextPositionParameters")
    public void nextPositionTest(Position p, Location relative, Location expected, double radians) {
        Position t = p.nextPosition(relative);
        CoordinateUtils.assertEquivalent(expected, t, TOLERANCE);
        assertEquals(radians, t.getHeading(), ScaleInfo.DEFAULT.getResolution());
    }

    private static Stream<Arguments> nextPositionParameters() {
        List<Arguments> args = new ArrayList<>();

        args.add(Arguments.of(Position.from(0, 0, 0), Location.from(1, 0), Location.from(1, 0), 0));
        args.add(Arguments.of(Position.from(0, 0, 0), Location.from(1, 1), Location.from(1, 1), RADIANS_45));
        args.add(Arguments.of(Position.from(0, 0, 0), Location.from(0, 1), Location.from(0, 1), RADIANS_90));
        args.add(Arguments.of(Position.from(0, 0, 0), Location.from(-1, 1), Location.from(-1, 1), RADIANS_135));
        args.add(Arguments.of(Position.from(0, 0, 0), Location.from(-1, 0), Location.from(-1, 0), RADIANS_180));
        args.add(Arguments.of(Position.from(0, 0, 0), Location.from(-1, -1), Location.from(-1, -1), RADIANS_225));
        args.add(Arguments.of(Position.from(0, 0, 0), Location.from(0, -1), Location.from(0, -1), RADIANS_270));
        args.add(Arguments.of(Position.from(0, 0, 0), Location.from(1, -1), Location.from(1, -1), RADIANS_315));

        args.add(Arguments.of(Position.from(0, 0, 0), Location.from(Precision.EPSILON, 0),
                Location.from(Precision.EPSILON, 0), 0));
        args.add(Arguments.of(Position.from(0, 0, 0), Location.from(Precision.EPSILON, Precision.EPSILON),
                Location.from(Precision.EPSILON, Precision.EPSILON), RADIANS_45));
        args.add(Arguments.of(Position.from(0, 0, 0), Location.from(0, Precision.EPSILON),
                Location.from(0, Precision.EPSILON), RADIANS_90));
        args.add(Arguments.of(Position.from(0, 0, 0), Location.from(-Precision.EPSILON, Precision.EPSILON),
                Location.from(-Precision.EPSILON, Precision.EPSILON), RADIANS_135));
        args.add(Arguments.of(Position.from(0, 0, 0), Location.from(-Precision.EPSILON, 0),
                Location.from(-Precision.EPSILON, 0), RADIANS_180));
        args.add(Arguments.of(Position.from(0, 0, 0), Location.from(-Precision.EPSILON, -Precision.EPSILON),
                Location.from(-Precision.EPSILON, -Precision.EPSILON), RADIANS_225));
        args.add(Arguments.of(Position.from(0, 0, 0), Location.from(0, -Precision.EPSILON),
                Location.from(0, -Precision.EPSILON), RADIANS_270));
        args.add(Arguments.of(Position.from(0, 0, 0), Location.from(Precision.EPSILON, -Precision.EPSILON),
                Location.from(Precision.EPSILON, -Precision.EPSILON), RADIANS_315));

        args.add(Arguments.of(Position.from(-1, -3, RADIANS_90), Location.from(0.5, 3.0), Location.from(-4.0, -2.5),
                2.976443976175166));

        return args.stream();
    }

    @ParameterizedTest(name = "{index} {0}")
    @MethodSource("headingParameters")
    public void headingTest(Position p, Coordinate c, double expected) {
        assertEquals(expected, p.headingTo(c), ScaleInfo.DEFAULT.getResolution());
    }

    public static Stream<Arguments> headingParameters() {
        List<Arguments> lst = new ArrayList<>();
        lst.add(Arguments.arguments(Position.from(-1, -3, 0), new Coordinate(-1, -1), RADIANS_90));
        lst.add(Arguments.arguments(Position.from(-1, -3, RADIANS_90), new Coordinate(-1, -1), RADIANS_90));
        lst.add(Arguments.arguments(Position.from(-1, -3, -RADIANS_90), new Coordinate(-1, -1), RADIANS_90));
        lst.add(Arguments.arguments(Position.from(-1, -3, RADIANS_45), new Coordinate(-1, -1), RADIANS_90));
        lst.add(Arguments.arguments(Position.from(-1, 1, RADIANS_45), new Coordinate(1, 3), RADIANS_45));
        return lst.stream();
    }

    @ParameterizedTest(name = "{index} {0}")
    @MethodSource("relativeLocationParameters")
    public void relativeLocationTest(Position position, Coordinate absolute, double heading) {
        Location relative = position.relativeLocation(absolute);
        Position p2 = position.nextPosition(relative);
        CoordinateUtils.assertEquivalent(absolute, p2, TOLERANCE);
        assertEquals(AngleUtils.normalize(heading), AngleUtils.normalize(p2.getHeading()), AngleUtils.TOLERANCE);
    }

    private static Arguments makeRelativeLocArguments(Position p, Coordinate c) {
        double heading = p.equals2D(c, TOLERANCE) ? p.getHeading() : p.headingTo(c);
        return Arguments.arguments(p, c, heading);
    }

    public static Stream<Arguments> relativeLocationParameters() {
        int[] idx = { 100, 50, -50, -100 };
        List<Arguments> lst = new ArrayList<>();
        for (double d : angles) {
            for (int x : idx) {
                for (int y : idx) {
                    lst.add(makeRelativeLocArguments(Position.from(50, 50, d), new Coordinate(x, y)));
                }
            }
        }
        lst.add(makeRelativeLocArguments(Position.from(-2, -2, RADIANS_180), new Coordinate(-1, 1)));
        return lst.stream();
    }
}
