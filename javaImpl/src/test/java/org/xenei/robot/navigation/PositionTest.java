package org.xenei.robot.navigation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class PositionTest {

    private static final double DELTA = 0.00000000001;

    @Test
    public void zigZagTest() {
        Double radians = Math.toRadians(45);
        double sqrt2 = Math.sqrt(2.0);
        Position initial = new Position(0.0, 0.0);

        Coordinates cmd = Coordinates.fromDegrees(45, 2);
        Position nxt = initial.nextPosition(cmd);
        assertEquals(radians, nxt.getHeadingRadians(), DELTA);
        assertEquals(sqrt2, nxt.getX(), DELTA);
        assertEquals(sqrt2, nxt.getY(), DELTA);

        cmd = Coordinates.fromDegrees(-45, 2);
        nxt = nxt.nextPosition(cmd);

        assertEquals(sqrt2 + 2, nxt.getX(), DELTA);
        assertEquals(sqrt2, nxt.getY(), DELTA);
        assertEquals(0.0, nxt.getHeadingRadians(), DELTA);
    }

    @Test
    public void boxTest() {
        Double radians = Math.toRadians(45);
        double sqrt2 = Math.sqrt(2.0);
        Position initial = new Position(0.0, 0.0);

        Coordinates cmd = Coordinates.fromDegrees(45, 2);
        Position nxt = initial.nextPosition(cmd);
        assertEquals(radians, nxt.getHeadingRadians(), DELTA);
        assertEquals(sqrt2, nxt.getX(), DELTA);
        assertEquals(sqrt2, nxt.getY(), DELTA);

        cmd = Coordinates.fromDegrees(-90, 2);
        nxt = nxt.nextPosition(cmd);
        assertEquals(-45, nxt.getHeadingDegrees(), DELTA);
        assertEquals(sqrt2 * 2, nxt.getX(), DELTA);
        assertEquals(0.0, nxt.getY(), DELTA);

        nxt = nxt.nextPosition(cmd);
        assertEquals(-135, nxt.getHeadingDegrees(), DELTA);
        assertEquals(sqrt2, nxt.getX(), DELTA);
        assertEquals(-sqrt2, nxt.getY(), DELTA);

        nxt = nxt.nextPosition(cmd);
        assertEquals(135, nxt.getHeadingDegrees(), DELTA);
        assertEquals(0, nxt.getX(), DELTA);
        assertEquals(0, nxt.getY(), DELTA);
    }
}
