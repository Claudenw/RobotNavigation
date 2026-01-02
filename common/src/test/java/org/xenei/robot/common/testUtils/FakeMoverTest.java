package org.xenei.robot.common.testUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.xenei.robot.common.utils.AngleUtils.RADIANS_135;
import static org.xenei.robot.common.utils.AngleUtils.RADIANS_225;
import static org.xenei.robot.common.utils.AngleUtils.RADIANS_315;
import static org.xenei.robot.common.utils.AngleUtils.RADIANS_45;

import org.junit.jupiter.api.AfterEach;
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

public final class FakeMoverTest {
    private double delta = 0.00000000001;

    private final double sqrt2 = Math.sqrt(2.0);
    private FakeMover underTest;
    private RobutContext ctxt;

    @BeforeEach
    void setup() {
        RobutContext.Builder builder = RobutContext.builder();
        builder.setOptions(builder.defaultOptions())
                .setChassisInfo(ChassisInfoTest.DEFAULT);
        ctxt = builder.build();
        underTest = new FakeMover(ctxt, new CoordinateXY(0, 0));
        delta = 1 / Math.pow(10, ctxt.scaleInfo.decimalPlaces() + 1);
    }

    @AfterEach
    void teardown() {
        ctxt.close();
    }

    @Test
    void zigZagTest() {
        Location move = Location.asLocation(CoordUtils.fromAngle(RADIANS_45, 2));
        underTest.move(move);
        assertEquals(RADIANS_45, underTest.position().heading(), delta);
        ctxt.scaleInfo.compare(ScaleInfo.OP.EQ, Location.asLocation(new Coordinate(sqrt2, sqrt2)), underTest.position());

        move = Location.asLocation(CoordUtils.fromAngle(-RADIANS_45, 2));
        underTest.move(move);
        assertEquals(0.0, underTest.position().heading(), delta);
        ctxt.scaleInfo.compare(ScaleInfo.OP.EQ, Location.asLocation(new Coordinate(sqrt2 + 2, sqrt2)), underTest.position());
    }

    @Test
    void boxTest() {
        Location move = Location.asLocation(new Coordinate(sqrt2, sqrt2));
        underTest.move(move);
        System.out.println(underTest.position());
        assertEquals(RADIANS_45, underTest.position().heading(), delta);
        ctxt.scaleInfo.compare(ScaleInfo.OP.EQ, move, underTest.position());

        move = Location.asLocation(CoordUtils.fromAngle(AngleUtils.RADIANS_90, 2));
        underTest.move(move);
        System.out.println(underTest.position());
        assertEquals(RADIANS_135, underTest.position().heading(), delta);
        ctxt.scaleInfo.compare(ScaleInfo.OP.EQ, Location.asLocation(new Coordinate(0.0, sqrt2)), underTest.position());

        underTest.move(move);
        System.out.println(underTest.position());
        assertEquals(RADIANS_225, underTest.position().heading(), delta);
        ctxt.scaleInfo.compare(ScaleInfo.OP.EQ, Location.asLocation(new Coordinate(-sqrt2, sqrt2)), underTest.position());

        underTest.move(move);
        System.out.println(underTest.position());
        assertEquals(RADIANS_315, underTest.position().heading(), delta);
        ctxt.scaleInfo.compare(ScaleInfo.OP.EQ, Location.ORIGIN, underTest.position());
    }
}
