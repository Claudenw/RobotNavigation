package org.xenei.robot.common.testUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xenei.robot.common.AngleUnits;
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
        assertEquals(radians, underTest.position().getHeading(AngleUnits.RADIANS), DELTA);
        assertEquals(sqrt2, underTest.position().getX(), DELTA);
        assertEquals(sqrt2, underTest.position().getY(), DELTA);

        move = Coordinates.fromXY(sqrt2 + 2, sqrt2).minus(underTest.position());
        underTest.move(move);
        assertEquals(sqrt2 + 2, underTest.position().getX(), DELTA);
        assertEquals(sqrt2, underTest.position().getY(), DELTA);
        assertEquals(0.0, underTest.position().getHeading(AngleUnits.RADIANS), DELTA);
    }

    @Test
    public void boxTest() {
        Coordinates move = Coordinates.fromXY(sqrt2, sqrt2).minus(underTest.position());
        underTest.move(move);
        assertEquals(radians, underTest.position().getHeading(AngleUnits.RADIANS), DELTA);
        assertEquals(sqrt2, underTest.position().getX(), DELTA);
        assertEquals(sqrt2, underTest.position().getY(), DELTA);

        move = Coordinates.fromXY(sqrt2 * 2, 0.0).minus(underTest.position());
        underTest.move(move);
        assertEquals(-45, underTest.position().getHeading(AngleUnits.DEGREES), DELTA);
        assertEquals(sqrt2 * 2, underTest.position().getX(), DELTA);
        assertEquals(0.0, underTest.position().getY(), DELTA);

        move = Coordinates.fromXY(sqrt2, -sqrt2).minus(underTest.position());
        underTest.move(move);
        assertEquals(-135, underTest.position().getHeading(AngleUnits.DEGREES), DELTA);
        assertEquals(sqrt2, underTest.position().getX(), DELTA);
        assertEquals(-sqrt2, underTest.position().getY(), DELTA);

        move = Coordinates.fromXY(0, 0).minus(underTest.position());
        underTest.move(move);
        assertEquals(135, underTest.position().getHeading(AngleUnits.DEGREES), DELTA);
        assertEquals(0, underTest.position().getX(), DELTA);
        assertEquals(0, underTest.position().getY(), DELTA);
    }
}
