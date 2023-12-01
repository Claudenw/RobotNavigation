package org.xenei.robot.common.testUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.xenei.robot.common.testUtils.DoubleUtils.RADIANS_135;
import static org.xenei.robot.common.testUtils.DoubleUtils.RADIANS_45;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xenei.robot.common.Coordinates;

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
        Coordinates move = Coordinates.fromXY(sqrt2, sqrt2).minus(underTest.position());
        underTest.move(move);
        assertEquals(radians, underTest.position().getHeading(), DELTA);
        assertEquals(sqrt2, underTest.position().getX(), DELTA);
        assertEquals(sqrt2, underTest.position().getY(), DELTA);

        move = Coordinates.fromXY(sqrt2 + 2, sqrt2).minus(underTest.position());
        underTest.move(move);
        assertEquals(sqrt2 + 2, underTest.position().getX(), DELTA);
        assertEquals(sqrt2, underTest.position().getY(), DELTA);
        assertEquals(0.0, underTest.position().getHeading(), DELTA);
    }

    @Test
    public void boxTest() {
        Coordinates move = Coordinates.fromXY(sqrt2, sqrt2).minus(underTest.position());
        underTest.move(move);
        assertEquals(radians, underTest.position().getHeading(), DELTA);
        assertEquals(sqrt2, underTest.position().getX(), DELTA);
        assertEquals(sqrt2, underTest.position().getY(), DELTA);

        move = Coordinates.fromXY(sqrt2 * 2, 0.0).minus(underTest.position());
        underTest.move(move);
        assertEquals(-RADIANS_45, underTest.position().getHeading(), DELTA);
        assertEquals(sqrt2 * 2, underTest.position().getX(), DELTA);
        assertEquals(0.0, underTest.position().getY(), DELTA);

        move = Coordinates.fromXY(sqrt2, -sqrt2).minus(underTest.position());
        underTest.move(move);
        assertEquals(-RADIANS_135, underTest.position().getHeading(), DELTA);
        assertEquals(sqrt2, underTest.position().getX(), DELTA);
        assertEquals(-sqrt2, underTest.position().getY(), DELTA);

        move = Coordinates.fromXY(0, 0).minus(underTest.position());
        underTest.move(move);
        assertEquals(RADIANS_135, underTest.position().getHeading(), DELTA);
        assertEquals(0, underTest.position().getX(), DELTA);
        assertEquals(0, underTest.position().getY(), DELTA);
    }
}
