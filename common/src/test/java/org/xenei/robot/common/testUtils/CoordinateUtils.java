package org.xenei.robot.common.testUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.function.Supplier;

import org.locationtech.jts.geom.Coordinate;
import org.xenei.robot.common.mapping.MapCoordinate;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.utils.CoordUtils;

public class CoordinateUtils {

    private CoordinateUtils() {
    }

    public static Location[] sortedArray(Collection<MapCoordinate> collect) {
        return sortedArray(collect.toArray(new Location[0]));
    }

    public static Location[] sortedArray(Location[] collect) {
        Arrays.sort(collect, Location.XYCompr);
        return collect;
    }

    private static Coordinate asCoordinate(Object o) {
        return (o instanceof Coordinate) ? (Coordinate) o : ((Location) o).getCoordinate();
    }

    public static void assertEquivalent(Object a, Object b, double delta, Supplier<String> prefix) {
        Coordinate one = asCoordinate(a);
        Coordinate two = asCoordinate(b);
        assertTrue(one.equals2D(two, delta),
                () -> String.format("%s Expected %s actual %s delta %s", prefix.get(), one, two, delta));
    }

    public static void assertEquivalent(Object one, Object two, double delta) {
        assertEquivalent(one, two, delta, () -> "");
    }

    public static void assertEquivalent(Object one, Object two) {
        assertEquivalent(one, two, 0, () -> "");
    }

    public static void assertEquivalent(Object one, Object two, Supplier<String> prefix) {
        assertEquivalent(one, two, 0, prefix);
    }

    public static void assertNotEquivalent(Object a, Object b, double delta) {
        Coordinate one = asCoordinate(a);
        Coordinate two = asCoordinate(b);
        assertFalse(one.equals2D(two, delta), () -> String.format("expected %s actual %s delta %s",
                CoordUtils.toString(one), CoordUtils.toString(two), delta));
    }

    public static void assertNotEquivalent(Object a, Object b) {
        assertNotEquivalent(a, b, 0);
    }

    public static void assertEquivalent(Collection<MapCoordinate> one, Collection<MapCoordinate> two,
                                        double delta) {
        assertEquals(one.size(), two.size(), () -> "differing sizes");
        Iterator<MapCoordinate> iter1 = one.iterator();
        Iterator<MapCoordinate> iter2 = two.iterator();
        for (int i = 0; i < one.size(); i++) {
            final int idx = i;
            assertEquivalent(iter1.next(), iter2.next(), delta, () -> "Error at " + idx);
        }
    }

    public static void assertEquivalent(MapCoordinate[] one, MapCoordinate[] two, double delta) {
        assertEquals(one.length, two.length, () -> "differing sizes");
        for (int i = 0; i < one.length; i++) {
            final int idx = i;
            assertEquivalent(one[i], two[i], delta, () -> "Error at " + idx);
        }
    }
}
