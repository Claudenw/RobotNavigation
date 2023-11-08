package org.xenei.robot.navigation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.xenei.robot.testUtils.DoubleUtils.DELTA;
import static org.xenei.robot.testUtils.DoubleUtils.RADIANS_45;
import static org.xenei.robot.testUtils.DoubleUtils.SQRT2;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
}
