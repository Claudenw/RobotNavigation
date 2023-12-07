package org.xenei.robot.common.testUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.xenei.robot.common.utils.AngleUtils.RADIANS_135;
import static org.xenei.robot.common.utils.AngleUtils.RADIANS_45;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xenei.robot.common.Location;

public class FakeMoverTest {
    private static final double DELTA = 0.00000000001;

    private final Double radians = Math.toRadians(45);
    private final double sqrt2 = Math.sqrt(2.0);
    private FakeMover underTest;

    @BeforeEach
    public void setup() {
        underTest = new FakeMover(new Location(0, 0), 5);
    }

    @Test
    public void zigZagTest() {
        Location move = new Location(sqrt2, sqrt2).minus(underTest.position());
        underTest.move(move);
        assertEquals(radians, underTest.position().getHeading(), DELTA);
        assertEquals(sqrt2, underTest.position().getX(), DELTA);
        assertEquals(sqrt2, underTest.position().getY(), DELTA);

        move = new Location(sqrt2 + 2, sqrt2).minus(underTest.position());
        underTest.move(move);
        assertEquals(sqrt2 + 2, underTest.position().getX(), DELTA);
        assertEquals(sqrt2, underTest.position().getY(), DELTA);
        assertEquals(0.0, underTest.position().getHeading(), DELTA);
    }

    @Test
    public void boxTest() {
        Location move = new Location(sqrt2, sqrt2).minus(underTest.position());
        underTest.move(move);
        assertEquals(radians, underTest.position().getHeading(), DELTA);
        assertEquals(sqrt2, underTest.position().getX(), DELTA);
        assertEquals(sqrt2, underTest.position().getY(), DELTA);

        move = new Location(sqrt2 * 2, 0.0).minus(underTest.position());
        underTest.move(move);
        assertEquals(-RADIANS_45, underTest.position().getHeading(), DELTA);
        assertEquals(sqrt2 * 2, underTest.position().getX(), DELTA);
        assertEquals(0.0, underTest.position().getY(), DELTA);

        move = new Location(sqrt2, -sqrt2).minus(underTest.position());
        underTest.move(move);
        assertEquals(-RADIANS_135, underTest.position().getHeading(), DELTA);
        assertEquals(sqrt2, underTest.position().getX(), DELTA);
        assertEquals(-sqrt2, underTest.position().getY(), DELTA);

        move = new Location(0, 0).minus(underTest.position());
        underTest.move(move);
        assertEquals(RADIANS_135, underTest.position().getHeading(), DELTA);
        assertEquals(0, underTest.position().getX(), DELTA);
        assertEquals(0, underTest.position().getY(), DELTA);
    }
}
