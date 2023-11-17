package org.xenei.robot.testUtils;

import java.util.Arrays;
import java.util.Collection;

import org.xenei.robot.navigation.Coordinates;

public class CoordinateUtils {
    
    private CoordinateUtils() {}

    public static Coordinates[] sortedArray(Collection<Coordinates> collect) {
        return sortedArray(collect.toArray(new Coordinates[0]));
    }
    
    public static Coordinates[] sortedArray(Coordinates[] collect) {
        Arrays.sort(collect, Coordinates.XYCompr);
        return collect;
    }
}
