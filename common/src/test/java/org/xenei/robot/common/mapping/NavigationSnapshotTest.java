package org.xenei.robot.common.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.locationtech.jts.geom.Coordinate;
import org.xenei.robot.common.ChassisInfoTest;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.ScaleInfo;
import org.xenei.robot.common.utils.AngleUtils;
import org.xenei.robot.common.utils.RobutContext;

import javax.measure.Quantity;

public class NavigationSnapshotTest {

    private static RobutContext.Builder builder;

    @BeforeAll
    static void setup() {
        RobutContext.Builder builder = RobutContext.builder();
        builder.setOptions(builder.defaultOptions())
                .setChassisInfo(ChassisInfoTest.DEFAULT);
    }

    private static Stream<Arguments> baseParams(BiPredicate<NavigationSnapshot, NavigationSnapshot> test) {
        final Map map = new Map(builder.build(), new MapTest.TestingStorage());
        final MapLocation target = map.asMapLocation(new Coordinate(2, 2));
        final MapPosition position = map.asMapPosition(new Coordinate(1, 1), AngleUtils.RADIANS_45);

        final NavigationSnapshot fullSnapshot = new NavigationSnapshot(position, target);
        final NavigationSnapshot nullPosition = new NavigationSnapshot(null, target);
        final NavigationSnapshot nullTarget = new NavigationSnapshot(position, null);
        final NavigationSnapshot diffHead = new NavigationSnapshot(map.asMapPosition(new Coordinate(1, 1), AngleUtils.RADIANS_90), target);
        final NavigationSnapshot diffLoc = new NavigationSnapshot(map.asMapPosition(new Coordinate(1, 2), AngleUtils.RADIANS_45), target);
        final NavigationSnapshot diffTarget = new NavigationSnapshot(position, map.asMapLocation(new Coordinate(3, 3)));

        final java.util.Map<NavigationSnapshot, String> navMap = new HashMap<>();


            navMap.put(fullSnapshot, "full");
            navMap.put(nullPosition, "nullPos");
            navMap.put(nullTarget, "nullTarg");
            navMap.put(diffHead, "diffhead");
            navMap.put(diffLoc, "diffLoc");
            navMap.put(diffTarget, "diffTarg");

        List<Arguments> lst = new ArrayList<>();

        BiPredicate<NavigationSnapshot, NavigationSnapshot> filter = (x, y) -> {
            if (x == null) {
                return y == null;
            }
            return y != null && test.test(x, y);
        };

        for (NavigationSnapshot snap1 : navMap.keySet()) {
            for (NavigationSnapshot snap2 : navMap.keySet()) {
                String name = String.format("%s-%s", navMap.get(snap1), navMap.get(snap2));
                lst.add(Arguments.of(name, filter.test(snap2, snap1), snap1, snap2));
            }
        }
        return lst.stream();
    }

    @Test
    public void headingTest() {
        try (RobutContext ctxt = builder.build()) {
            final Map map = new Map(ctxt, new MapTest.TestingStorage());
            final MapLocation target = map.asMapLocation(new Coordinate(2, 2));
            final MapPosition position = map.asMapPosition(new Coordinate(1, 1), AngleUtils.RADIANS_45);
            final NavigationSnapshot fullSnapshot = new NavigationSnapshot(position, target);
            final NavigationSnapshot nullPosition = new NavigationSnapshot(null, target);
            final NavigationSnapshot nullTarget = new NavigationSnapshot(position, null);

            assertEquals(AngleUtils.RADIANS_45, fullSnapshot.heading(), fullSnapshot::toString);
            assertEquals(Double.NaN, nullPosition.heading(), nullPosition::toString);
            assertEquals(AngleUtils.RADIANS_45, nullTarget.heading(), nullTarget::toString);
        }
    }

    @ParameterizedTest
    @MethodSource("changeParams")
    public void didChangeTest(String name, boolean state, NavigationSnapshot underTest, NavigationSnapshot other) {
        try {
            assertEquals(state, underTest.didChange(other));
        } finally {
            underTest.position.getContext().close();
        }
    }

    private static Stream<Arguments> changeParams() {
        return baseParams((x, y) -> x != y);
    }


    @ParameterizedTest
    @MethodSource("headingChangeParams")
    public void didHeadingChangeTest(String name, boolean state, NavigationSnapshot underTest,
            NavigationSnapshot other) {
        try {
            assertEquals(state, underTest.didHeadingChange(other));
        } finally {
            underTest.position.getContext().close();
        }
    }

    private static Stream<Arguments> headingChangeParams() {
        return baseParams((x, y) -> !Objects.equals(x.heading(), y.heading()));
    }

    //
    // /**
    // * Checks for change in heading.
    // * @param positionToCheck the position to check against.
    // * @return true if heading has changed.
    // */
    // public boolean didHeadingChange(Position positionToCheck) {
    // if (position == null) {
    // return (positionToCheck != null);
    // }
    // return !DoubleUtils.eq(position.getHeading(), positionToCheck.getHeading());
    // }
    //
    @ParameterizedTest
    @MethodSource("locationChangeParams")
    public void didLocationChangeTest(String name, boolean state, NavigationSnapshot underTest,
            NavigationSnapshot other) {
        try {
            assertEquals(state, underTest.didLocationChange(other));
        } finally {
            underTest.position.getContext().close();
        }
    }

    private static Stream<Arguments> locationChangeParams() {
        return baseParams((x, y) -> {
            if (x.position == null) {
                return y.position != null;
            }
            return y.position == null || !Objects.equals(x.position.getCoordinate(), y.position.getCoordinate());
        });
    }
    //
    // /**
    // * Checks for change in location.
    // * @param positionToCheck the position to check against.
    // * @return true if locatoin has changed.
    // */
    // boolean didLocationChange(Position positionToCheck) {
    // if (position == null) {
    // return (positionToCheck != null);
    // }
    // return !position.equals2D(positionToCheck);
    // }
    //
    @ParameterizedTest
    @MethodSource("targetChangeParams")
    public void didTargetChangeTest(String name, boolean state, NavigationSnapshot underTest,
            NavigationSnapshot other) {
        try {
            assertEquals(state, underTest.didTargetChange(other));
        } finally {
            underTest.position.getContext().close();
        }
    }

    private static Stream<Arguments> targetChangeParams() {
        return baseParams((x, y) -> !Objects.equals(x.target, y.target));
    }
    // /**
    // * Checks for change in target.
    // * @param coordinateToCheck the target to check against.
    // * @return true if target has changed.
    // */
    // public boolean didTargetChange(Coordinate coordinateToCheck) {
    // if (target == null) {
    // return (coordinateToCheck != null);
    // }
    // return !target.equals(coordinateToCheck);
    // }

}
