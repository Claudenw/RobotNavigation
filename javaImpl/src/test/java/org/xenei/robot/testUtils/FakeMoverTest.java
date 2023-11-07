package org.xenei.robot.testUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.xenei.robot.navigation.Coordinates;
import org.xenei.robot.navigation.Position;

public class FakeMoverTest {
    private static final double DELTA = 0.00000000001;
    
    @Test
    public void zigZagTest() {
        Double radians = Math.toRadians(45);
        double sqrt2 = Math.sqrt(2.0);
        Position initial = new Position(0.0, 0.0);
        FakeMover underTest = new FakeMover(initial,5);
        underTest.move( Coordinates.fromXY(sqrt2, sqrt2));
        assertEquals(radians, underTest.position().getHeadingRadians(), DELTA);
        assertEquals(sqrt2, underTest.position().getX(), DELTA);
        assertEquals(sqrt2, underTest.position().getY(), DELTA);

        underTest.move(Coordinates.fromXY(sqrt2+2, sqrt2));
        assertEquals(sqrt2 + 2, underTest.position().getX(), DELTA);
        assertEquals(sqrt2, underTest.position().getY(), DELTA);
        assertEquals(0.0, underTest.position().getHeadingRadians(), DELTA);
    }

    @Test
    public void boxTest() {
        Double radians = Math.toRadians(45);
        double sqrt2 = Math.sqrt(2.0);
        FakeMover underTest = new FakeMover(new Position(0.0, 0.0),5);
        
        underTest.move(Coordinates.fromXY(sqrt2, sqrt2));
        assertEquals(radians, underTest.position().getHeadingRadians(), DELTA);
        assertEquals(sqrt2, underTest.position().getX(), DELTA);
        assertEquals(sqrt2, underTest.position().getY(), DELTA);

        underTest.move(Coordinates.fromXY(sqrt2*2, 0.0));
        assertEquals(-45, underTest.position().getHeadingDegrees(), DELTA);
        assertEquals(sqrt2 * 2, underTest.position().getX(), DELTA);
        assertEquals(0.0, underTest.position().getY(), DELTA);

        underTest.move(Coordinates.fromXY(sqrt2, -sqrt2));
        assertEquals(-135, underTest.position().getHeadingDegrees(), DELTA);
        assertEquals(sqrt2, underTest.position().getX(), DELTA);
        assertEquals(-sqrt2, underTest.position().getY(), DELTA);

        underTest.move(Coordinates.fromXY(0, 0));
        assertEquals(135, underTest.position().getHeadingDegrees(), DELTA);
        assertEquals(0, underTest.position().getX(), DELTA);
        assertEquals(0, underTest.position().getY(), DELTA);
    }
}
