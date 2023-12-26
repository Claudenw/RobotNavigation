package org.xenei.robot.common.testUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.xenei.robot.common.utils.AngleUtils.RADIANS_135;
import static org.xenei.robot.common.utils.AngleUtils.RADIANS_225;
import static org.xenei.robot.common.utils.AngleUtils.RADIANS_315;
import static org.xenei.robot.common.utils.AngleUtils.RADIANS_45;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.utils.AngleUtils;
import org.xenei.robot.common.utils.CoordUtils;

public class FakeMoverTest {
    private static final double DELTA = 0.00000000001;

    private final Double radians = Math.toRadians(45);
    private final double sqrt2 = Math.sqrt(2.0);
    private FakeMover underTest;

    @BeforeEach
    public void setup() {
        underTest = new FakeMover(Position.from(0, 0), 5);
    }

    @Test
    public void zigZagTest() {
        Location move = Location.from(CoordUtils.fromAngle(RADIANS_45, 2));
        underTest.move(move);
        assertEquals(RADIANS_45, underTest.position().getHeading(), DELTA);
        assertEquals(sqrt2, underTest.position().getX(), DELTA);
        assertEquals(sqrt2, underTest.position().getY(), DELTA);

        move = Location.from(CoordUtils.fromAngle(-RADIANS_45, 2));
        underTest.move(move);
        assertEquals(sqrt2 + 2, underTest.position().getX(), DELTA);
        assertEquals(sqrt2, underTest.position().getY(), DELTA);
        assertEquals(0.0, underTest.position().getHeading(), DELTA);
    }

    @Test
    public void boxTest() {
        Location move = Location.from(sqrt2, sqrt2);
        underTest.move(move);
        assertEquals(RADIANS_45, underTest.position().getHeading(), DELTA);
        assertEquals(sqrt2, underTest.position().getX(), DELTA);
        assertEquals(sqrt2, underTest.position().getY(), DELTA);

        move = Location.from(CoordUtils.fromAngle(AngleUtils.RADIANS_90, 2));
        underTest.move(move);
        assertEquals(RADIANS_135, underTest.position().getHeading(), DELTA);
        assertEquals(0.0, underTest.position().getX(), DELTA);
        assertEquals(sqrt2 * 2, underTest.position().getY(), DELTA);

        underTest.move(move);
        assertEquals(RADIANS_225, underTest.position().getHeading(), DELTA);
        assertEquals(-sqrt2, underTest.position().getX(), DELTA);
        assertEquals(sqrt2, underTest.position().getY(), DELTA);

        underTest.move(move);
        assertEquals(RADIANS_315, underTest.position().getHeading(), DELTA);
        assertEquals(0, underTest.position().getX(), DELTA);
        assertEquals(0, underTest.position().getY(), DELTA);
    }
}
