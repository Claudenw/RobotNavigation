package org.xenei.robot.common.testUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.function.Supplier;

import org.xenei.robot.common.Coordinates;
import org.xenei.robot.common.utils.PointUtils;

public class CoordinateUtils {
    
    private CoordinateUtils() {}

    public static Coordinates[] sortedArray(Collection<Coordinates> collect) {
        return sortedArray(collect.toArray(new Coordinates[0]));
    }
    
    public static Coordinates[] sortedArray(Coordinates[] collect) {
        Arrays.sort(collect, Coordinates.XYCompr);
        return collect;
    }

    public static void assertEquivalent(Coordinates one, Coordinates two, double delta, Supplier<String> prefix) {
        assertTrue( PointUtils.sameAs(one, two, delta), () -> String.format("%s Expected %s actual %s delta %s",
               prefix.get(), PointUtils.toString(one), PointUtils.toString(two), delta ));
    }
    
    public static void assertEquivalent(Coordinates one, Coordinates two, double delta) {
        assertEquivalent(one, two, delta, ()->"");
    }

    public static void assertNotEquivalent(Coordinates one, Coordinates two, double delta) {
        assertFalse( PointUtils.sameAs(one, two, delta), () -> String.format("expected %s actual %s delta %s",
                PointUtils.toString(one), PointUtils.toString(two), delta ));
    }
    
    public static void assertEquivalent(Collection<Coordinates> one, Collection<Coordinates> two, double delta) {
        assertEquals( one.size(), two.size(), () -> "differing sizes");
        Iterator<Coordinates> iter1 = one.iterator();
        Iterator<Coordinates> iter2 = two.iterator();
        for (int i=0;i<one.size();i++) {
            final int idx = i;
            assertEquivalent( iter1.next(), iter2.next(), delta, ()->"Error at "+idx);
        }
    }

    public static void assertEquivalent(Coordinates[] one, Coordinates[] two, double delta) {
        assertEquals( one.length, two.length, () -> "differing sizes");
        for (int i=0;i<one.length;i++) {
            final int idx = i;
            assertEquivalent( one[i], two[i], delta, ()->"Error at "+idx);
        }
    }
}
