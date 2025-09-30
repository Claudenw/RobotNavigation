package org.xenei.robot.common.testUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.xenei.robot.common.utils.AngleUtils.RADIANS_135;
import static org.xenei.robot.common.utils.AngleUtils.RADIANS_225;
import static org.xenei.robot.common.utils.AngleUtils.RADIANS_315;
import static org.xenei.robot.common.utils.AngleUtils.RADIANS_45;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateXY;
import org.xenei.robot.common.ChassisInfoTest;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.ScaleInfo;
import org.xenei.robot.common.utils.AngleUtils;
import org.xenei.robot.common.utils.CoordUtils;
import org.xenei.robot.common.utils.RobutContext;

public class FakeMoverTest {
    private double delta = 0.00000000001;

    private final double sqrt2 = Math.sqrt(2.0);
    private FakeMover underTest;
    private RobutContext ctxt;

    @BeforeEach
    public void setup() {
        ctxt = new RobutContext(ScaleInfo.DEFAULT, ChassisInfoTest.DEFAULT);
        underTest = new FakeMover(ctxt, new CoordinateXY(0, 0));
        delta = 1 / Math.pow(10, ctxt.scaleInfo.decimalPlaces() + 1);
    }

    @Test
    public void zigZagTest() {
        Location move = new Location(CoordUtils.fromAngle(RADIANS_45, 2));
        underTest.move(move);
        assertEquals(RADIANS_45, underTest.position().getHeading(), delta);
        ctxt.scaleInfo.areEquivalent(new Location(new Coordinate(sqrt2, sqrt2)), underTest.position());

        move = new Location(CoordUtils.fromAngle(-RADIANS_45, 2));
        underTest.move(move);
        assertEquals(0.0, underTest.position().getHeading(), delta);
        ctxt.scaleInfo.areEquivalent(new Location(new Coordinate(sqrt2 + 2, sqrt2)), underTest.position());
    }

    @Test
    public void boxTest() {
        Location move = new Location(new Coordinate(sqrt2, sqrt2));
        underTest.move(move);
        System.out.println(underTest.position());
        assertEquals(RADIANS_45, underTest.position().getHeading(), delta);
        ctxt.scaleInfo.areEquivalent(move, underTest.position());

        move = new Location(CoordUtils.fromAngle(AngleUtils.RADIANS_90, 2));
        underTest.move(move);
        System.out.println(underTest.position());
        assertEquals(RADIANS_135, underTest.position().getHeading(), delta);
        ctxt.scaleInfo.areEquivalent(new Location(new Coordinate(0.0, sqrt2)), underTest.position());

        underTest.move(move);
        System.out.println(underTest.position());
        assertEquals(RADIANS_225, underTest.position().getHeading(), delta);
        ctxt.scaleInfo.areEquivalent(new Location(new Coordinate(-sqrt2, sqrt2)), underTest.position());

        underTest.move(move);
        System.out.println(underTest.position());
        assertEquals(RADIANS_315, underTest.position().getHeading(), delta);
        ctxt.scaleInfo.areEquivalent(Location.ORIGIN, underTest.position());
    }
}
