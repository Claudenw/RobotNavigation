package org.xenei.robot.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.xenei.robot.common.LocationTest.DELTA;
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
import org.xenei.robot.common.utils.AngleUtils;
import org.xenei.robot.common.utils.CoordUtils;

public class PositionTest {

    private static double[] angles = { 0, RADIANS_45, RADIANS_90, RADIANS_135, RADIANS_180, RADIANS_225, RADIANS_270,
            RADIANS_315 };

    private Position initial;
    private double buffer = 0.5;

    @BeforeEach
    public void setup() {
        initial = new Position(0.0, 0.0);
    }

    @Test
    public void zigZagTest() {
        Location cmd = new Location(CoordUtils.fromAngle(RADIANS_45, 2));
        Position nxt = initial.nextPosition(cmd);
        assertEquals(RADIANS_45, nxt.getHeading(), DELTA);
        assertEquals(SQRT2, nxt.getX(), DELTA);
        assertEquals(SQRT2, nxt.getY(), DELTA);

        cmd = new Location(CoordUtils.fromAngle(-RADIANS_45, 2));
        nxt = nxt.nextPosition(cmd);

        assertEquals(SQRT2 + 2, nxt.getX(), DELTA);
        assertEquals(SQRT2, nxt.getY(), DELTA);
        assertEquals(0.0, nxt.getHeading(), DELTA);
    }

    @Test
    public void boxTest() {
        Location cmd = new Location(CoordUtils.fromAngle(RADIANS_45, 2));
        Position nxt = initial.nextPosition(cmd);
        assertEquals(RADIANS_45, nxt.getHeading(), DELTA);
        assertEquals(SQRT2, nxt.getX(), DELTA);
        assertEquals(SQRT2, nxt.getY(), DELTA);

        cmd = new Location(CoordUtils.fromAngle(RADIANS_90, 2));
        double ct = cmd.theta();
        nxt = nxt.nextPosition(cmd);
        assertEquals(RADIANS_135, nxt.getHeading(), DELTA);
        assertEquals(0.0, nxt.getX(), DELTA);
        assertEquals(SQRT2 * 2, nxt.getY(), DELTA);

        nxt = nxt.nextPosition(cmd);
        assertEquals(RADIANS_225, nxt.getHeading(), DELTA);
        assertEquals(-SQRT2, nxt.getX(), DELTA);
        assertEquals(SQRT2, nxt.getY(), DELTA);

        nxt = nxt.nextPosition(cmd);
        assertEquals(RADIANS_315, nxt.getHeading(), DELTA);
        assertEquals(0, nxt.getX(), DELTA);
        assertEquals(0, nxt.getY(), DELTA);
    }

    @ParameterizedTest(name = "{index} {0}")
    @MethodSource("collisionParameters")
    public void collisionTest(String name, boolean state, Position pos, Coordinate target) {
        if (state) {
            assertTrue(pos.checkCollision(target, buffer),
                    () -> String.format("Did not collide with %s/%s", target.getX(), target.getY()));
        } else {
            assertFalse(pos.checkCollision(target, buffer),
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
                Position p = new Position(0, 0, heading);
                args.add(Arguments.of(String.format("%s/%s", Math.toDegrees(heading), Math.toDegrees(angle)),
                        angle == heading, p, c));
            }
        }
        return args.stream();
    }

    @ParameterizedTest(name = "{index} {0}")
    @MethodSource("fromCoordinateParameters")
    public void fromCoordinateTest(int degrees, Coordinate c, double radians) {
        Position p = new Position(0, 0, 0);
        Position t = p.fromCoordinate(c);
        CoordinateUtils.assertEquivalent(t, c, ScaleInfo.DEFAULT.getResolution());
        assertEquals(radians, t.getHeading(), ScaleInfo.DEFAULT.getResolution());
    }

    private static Stream<Arguments> fromCoordinateParameters() {
        List<Arguments> args = new ArrayList<>();

        args.add(Arguments.of(0, new Coordinate(1, 0), 0));
        args.add(Arguments.of(45, new Coordinate(1, 1), RADIANS_45));
        args.add(Arguments.of(90, new Coordinate(0, 1), RADIANS_90));
        args.add(Arguments.of(135, new Coordinate(-1, 1), RADIANS_135));
        args.add(Arguments.of(180, new Coordinate(-1, 0), RADIANS_180));
        args.add(Arguments.of(225, new Coordinate(-1, -1), RADIANS_225));
        args.add(Arguments.of(270, new Coordinate(0, -1), RADIANS_270));
        args.add(Arguments.of(315, new Coordinate(1, -1), RADIANS_315));

        return args.stream();
    }

    @ParameterizedTest(name = "{index} {0}")
    @MethodSource("nextPositionParameters")
    public void nextPositionTest(int degrees, Location l, double radians) {
        Position p = new Position(0, 0, 0);
        Position t = p.nextPosition(l);
        CoordinateUtils.assertEquivalent(l, t, DELTA);
        assertEquals(radians, t.getHeading(), ScaleInfo.DEFAULT.getResolution());
    }

    private static Stream<Arguments> nextPositionParameters() {
        List<Arguments> args = new ArrayList<>();

        args.add(Arguments.of(0, new Location(1, 0), 0));
        args.add(Arguments.of(45, new Location(1, 1), RADIANS_45));
        args.add(Arguments.of(90, new Location(0, 1), RADIANS_90));
        args.add(Arguments.of(135, new Location(-1, 1), RADIANS_135));
        args.add(Arguments.of(180, new Location(-1, 0), RADIANS_180));
        args.add(Arguments.of(225, new Location(-1, -1), RADIANS_225));
        args.add(Arguments.of(270, new Location(0, -1), RADIANS_270));
        args.add(Arguments.of(315, new Location(1, -1), RADIANS_315));

        args.add(Arguments.of(0, new Location(Precision.EPSILON, 0), 0));
        args.add(Arguments.of(45, new Location(Precision.EPSILON, Precision.EPSILON), RADIANS_45));
        args.add(Arguments.of(90, new Location(0, Precision.EPSILON), RADIANS_90));
        args.add(Arguments.of(135, new Location(-Precision.EPSILON, Precision.EPSILON), RADIANS_135));
        args.add(Arguments.of(180, new Location(-Precision.EPSILON, 0), RADIANS_180));
        args.add(Arguments.of(225, new Location(-Precision.EPSILON, -Precision.EPSILON), RADIANS_225));
        args.add(Arguments.of(270, new Location(0, -Precision.EPSILON), RADIANS_270));
        args.add(Arguments.of(315, new Location(Precision.EPSILON, -Precision.EPSILON), RADIANS_315));

        return args.stream();
    }

    @ParameterizedTest(name = "{index} {0}")
    @MethodSource("headingToParameters")
    public void headingToTest(Position p, Coordinate c, double expected) {
        assertEquals(expected, p.headingTo(c), ScaleInfo.DEFAULT.getResolution());
    }

    public static Stream<Arguments> headingToParameters() {
        List<Arguments> lst = new ArrayList<>();
        lst.add(Arguments.arguments(new Position(-1, -3, 0), new Coordinate(-1, -1), RADIANS_90));
        lst.add(Arguments.arguments(new Position(-1, -3, RADIANS_90), new Coordinate(-1, -1), RADIANS_90));
        lst.add(Arguments.arguments(new Position(-1, -3, -RADIANS_90), new Coordinate(-1, -1), RADIANS_90));
        lst.add(Arguments.arguments(new Position(-1, -3, RADIANS_45), new Coordinate(-1, -1), RADIANS_90));
        lst.add(Arguments.arguments(new Position(-1, 1, RADIANS_45), new Coordinate(1, 3), RADIANS_45));
        return lst.stream();
    }

    @ParameterizedTest(name = "{index} {0}")
    @MethodSource("relativeLocationParameters")
    public void relativeLocationTest(Position position, Coordinate absolute, double heading) {
        Location relative = position.relativeLocation(absolute);
        Position p2 = position.nextPosition(relative);
        CoordinateUtils.assertEquivalent(absolute, p2, DELTA);
        assertEquals(AngleUtils.normalize(heading), AngleUtils.normalize(p2.getHeading()), DELTA);
    }

    private static Arguments makeRelativeLocArguments(Position p, Coordinate c) {
        double heading = p.equals2D(c, DELTA) ? p.getHeading() : p.headingTo(c);
        return Arguments.arguments(p, c, heading);
    }

    public static Stream<Arguments> relativeLocationParameters() {
        int[] idx = { 100, 50, -50, -100 };
        List<Arguments> lst = new ArrayList<>();
        for (double d : angles) {
            for (int x : idx) {
                for (int y : idx) {
                    lst.add(makeRelativeLocArguments(new Position(50, 50, d), new Coordinate(x, y)));
                }
            }
        }
        lst.add(makeRelativeLocArguments(new Position(-2, -2, RADIANS_180), new Coordinate(-1, 1)));
        return lst.stream();
    }
}
