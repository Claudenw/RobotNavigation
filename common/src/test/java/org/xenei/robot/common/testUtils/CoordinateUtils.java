package org.xenei.robot.common.testUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.function.Supplier;

import org.xenei.robot.common.Location;
import org.xenei.robot.common.utils.CoordUtils;

public class CoordinateUtils {
    
    private CoordinateUtils() {}

    public static Location[] sortedArray(Collection<Location> collect) {
        return sortedArray(collect.toArray(new Location[0]));
    }
    
    public static Location[] sortedArray(Location[] collect) {
        Arrays.sort(collect, Location.XYCompr);
        return collect;
    }

    public static void assertEquivalent(Location one, Location two, double delta, Supplier<String> prefix) {
        assertTrue( one.equals2D( two, delta), () -> String.format("%s Expected %s actual %s delta %s",
               prefix.get(), CoordUtils.toString(one), CoordUtils.toString(two), delta ));
    }
    
    public static void assertEquivalent(Location one, Location two, double delta) {
        assertEquivalent(one, two, delta, ()->"");
    }

    public static void assertNotEquivalent(Location one, Location two, double delta) {
        assertFalse( one.equals2D( two, delta), () -> String.format("expected %s actual %s delta %s",
                CoordUtils.toString(one), CoordUtils.toString(two), delta ));
    }
    
    public static void assertEquivalent(Collection<Location> one, Collection<Location> two, double delta) {
        assertEquals( one.size(), two.size(), () -> "differing sizes");
        Iterator<Location> iter1 = one.iterator();
        Iterator<Location> iter2 = two.iterator();
        for (int i=0;i<one.size();i++) {
            final int idx = i;
            assertEquivalent( iter1.next(), iter2.next(), delta, ()->"Error at "+idx);
        }
    }

    public static void assertEquivalent(Location[] one, Location[] two, double delta) {
        assertEquals( one.length, two.length, () -> "differing sizes");
        for (int i=0;i<one.length;i++) {
            final int idx = i;
            assertEquivalent( one[i], two[i], delta, ()->"Error at "+idx);
        }
    }
}
