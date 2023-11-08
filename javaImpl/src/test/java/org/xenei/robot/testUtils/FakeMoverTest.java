package org.xenei.robot.testUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xenei.robot.navigation.Coordinates;

public class FakeMoverTest {
    private static final double DELTA = 0.00000000001;

    private final Double radians = Math.toRadians(45);
    private final double sqrt2 = Math.sqrt(2.0);
    FakeMover underTest;

    @BeforeEach
    public void setup() {
        underTest = new FakeMover(Coordinates.fromXY(0, 0), 5);
    }

    @Test
    public void zigZagTest() {
        Coordinates move = Coordinates.fromXY(sqrt2, sqrt2).minus(underTest.position().coordinates());
        underTest.move(move);
        assertEquals(radians, underTest.position().getHeadingRadians(), DELTA);
        assertEquals(sqrt2, underTest.position().coordinates().getX(), DELTA);
        assertEquals(sqrt2, underTest.position().coordinates().getY(), DELTA);

        move = Coordinates.fromXY(sqrt2 + 2, sqrt2).minus(underTest.position().coordinates());
        underTest.move(move);
        assertEquals(sqrt2 + 2, underTest.position().coordinates().getX(), DELTA);
        assertEquals(sqrt2, underTest.position().coordinates().getY(), DELTA);
        assertEquals(0.0, underTest.position().getHeadingRadians(), DELTA);
    }

    @Test
    public void boxTest() {
        Coordinates move = Coordinates.fromXY(sqrt2, sqrt2).minus(underTest.position().coordinates());
        underTest.move(move);
        assertEquals(radians, underTest.position().getHeadingRadians(), DELTA);
        assertEquals(sqrt2, underTest.position().coordinates().getX(), DELTA);
        assertEquals(sqrt2, underTest.position().coordinates().getY(), DELTA);

        move = Coordinates.fromXY(sqrt2 * 2, 0.0).minus(underTest.position().coordinates());
        underTest.move(move);
        assertEquals(-45, underTest.position().getHeadingDegrees(), DELTA);
        assertEquals(sqrt2 * 2, underTest.position().coordinates().getX(), DELTA);
        assertEquals(0.0, underTest.position().coordinates().getY(), DELTA);

        move = Coordinates.fromXY(sqrt2, -sqrt2).minus(underTest.position().coordinates());
        underTest.move(move);
        assertEquals(-135, underTest.position().getHeadingDegrees(), DELTA);
        assertEquals(sqrt2, underTest.position().coordinates().getX(), DELTA);
        assertEquals(-sqrt2, underTest.position().coordinates().getY(), DELTA);

        move = Coordinates.fromXY(0, 0).minus(underTest.position().coordinates());
        underTest.move(move);
        assertEquals(135, underTest.position().getHeadingDegrees(), DELTA);
        assertEquals(0, underTest.position().coordinates().getX(), DELTA);
        assertEquals(0, underTest.position().coordinates().getY(), DELTA);
    }
}
