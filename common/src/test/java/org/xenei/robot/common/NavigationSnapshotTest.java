package org.xenei.robot.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.locationtech.jts.geom.Coordinate;
import org.xenei.robot.common.utils.AngleUtils;

public class NavigationSnapshotTest {
    static final Coordinate target = new Coordinate(2,2);
    static final Position position = Position.from(1,1,AngleUtils.RADIANS_45);
    static final NavigationSnapshot fullSnapshot = new NavigationSnapshot( position, target);
    static final NavigationSnapshot nullPosition = new NavigationSnapshot( null, target);
    static final NavigationSnapshot nullTarget = new NavigationSnapshot( position, null);
    static final NavigationSnapshot diffHead = new NavigationSnapshot( Position.from(1,1,AngleUtils.RADIANS_90), target);
    static final NavigationSnapshot diffLoc = new NavigationSnapshot( Position.from(1,2,AngleUtils.RADIANS_45), target);
    static final NavigationSnapshot diffTarget = new NavigationSnapshot(position, new Coordinate(3,3));
     
    private static Map<NavigationSnapshot,String> navMap = new HashMap<>();
    
    @BeforeAll
    public static void setup() {
        navMap.put(fullSnapshot, "full");
        navMap.put(nullPosition, "nullPos");
        navMap.put(nullTarget, "nullTarg");
        navMap.put(diffHead, "diffhead");
        navMap.put(diffLoc,  "diffLoc");
        navMap.put(diffTarget,  "diffTarg");
    }
    
    private static Stream<Arguments> baseParams(BiPredicate<NavigationSnapshot,NavigationSnapshot> test) {
        List<Arguments> lst = new ArrayList<>();

        BiPredicate<NavigationSnapshot,NavigationSnapshot> filter = (x,y) -> {
                if (x == null) {
                    return y == null;
                }
                return y != null && test.test(x, y);
        };
        
        for (NavigationSnapshot snap1 : navMap.keySet()  ) {
            for (NavigationSnapshot snap2 : navMap.keySet()  ) {
                String name = String.format("%s-%s", navMap.get(snap1), navMap.get(snap2));
                lst.add(Arguments.of(name, filter.test(snap2, snap1), snap1, snap2));
            }
        }
        return lst.stream();
    }
    
    @Test
    public void headingTest() {
        assertEquals( AngleUtils.RADIANS_45, fullSnapshot.heading(), () -> fullSnapshot.toString());
        assertEquals( Double.NaN, nullPosition.heading(), () -> nullPosition.toString());
        assertEquals( AngleUtils.RADIANS_45, nullTarget.heading(), () -> nullTarget.toString());
    }

    @ParameterizedTest
    @MethodSource("changeParams")
    public void didChangeTest(String name, boolean state, NavigationSnapshot underTest, NavigationSnapshot other) {
        assertEquals( state, underTest.didChange(other));
    }
    
    private static Stream<Arguments> changeParams() {
        return baseParams((x,y) -> x != y);
    }

    
//    
//    /**
//     * Checks for change in position or target.
//     * @param positionToCheck the position to check against.
//     * @param targetToCheck the target to check against.
//     * @return true if location, heading, or target has changed.
//     */
//    public boolean didChange(Position positionToCheck, Coordinate targetToCheck) {
//        return didHeadingChange(positionToCheck) 
//                || didLocationChange(positionToCheck) 
//                || didTargetChange(targetToCheck);
//    }
//
//

    @ParameterizedTest
    @MethodSource("headingChangeParams")
    public void didHeadingChangeTest(String name, boolean state, NavigationSnapshot underTest, NavigationSnapshot other) {
        assertEquals( state, underTest.didHeadingChange(other));
    }
    
    private static Stream<Arguments> headingChangeParams() {
        return baseParams((x,y) -> !Objects.equals(x.heading(), y.heading()));
    }

//
//    /**
//     * Checks for change in heading.
//     * @param positionToCheck the position to check against.
//     * @return true if heading has changed.
//     */
//    public boolean didHeadingChange(Position positionToCheck) {
//        if (position == null) {
//            return (positionToCheck != null);
//        }
//        return !DoubleUtils.eq(position.getHeading(), positionToCheck.getHeading());
//    }
//
    @ParameterizedTest
    @MethodSource("locationChangeParams")
    public void didLocationChangeTest(String name, boolean state, NavigationSnapshot underTest, NavigationSnapshot other) {
        assertEquals( state, underTest.didLocationChange(other));
    }
    
    private static Stream<Arguments> locationChangeParams() {
        return baseParams((x,y) -> { 
            if (x.position == null) {
                return y.position != null;
            }
            return y.position == null || !Objects.equals(x.position.getCoordinate(), y.position.getCoordinate());
        });
    }
//    
//    /**
//     * Checks for change in location.
//     * @param positionToCheck the position to check against.
//     * @return true if locatoin has changed.
//     */
//    boolean didLocationChange(Position positionToCheck) {
//        if (position == null) {
//            return (positionToCheck != null);
//        }
//        return !position.equals2D(positionToCheck);
//    }
//
    @ParameterizedTest
    @MethodSource("targetChangeParams")
    public void didTargetChangeTest(String name, boolean state, NavigationSnapshot underTest, NavigationSnapshot other) {
        assertEquals( state, underTest.didTargetChange(other));
    }
    
    private static Stream<Arguments> targetChangeParams() {
        return baseParams((x,y) -> !Objects.equals(x.target, y.target));
    }
//    /**
//     * Checks for change in target.
//     * @param coordinateToCheck the target to check against.
//     * @return true if target has changed.
//     */
//    public boolean didTargetChange(Coordinate coordinateToCheck) {
//        if (target == null) {
//            return (coordinateToCheck != null);
//        }
//        return !target.equals(coordinateToCheck);
//    }

}
