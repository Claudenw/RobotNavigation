package org.xenei.robot.common.utils;

import org.apache.commons.math3.util.Precision;

public class DoubleUtils {
    
    public static final double SQRT2 = Math.sqrt(2.0);

    
    private DoubleUtils() {}

    // bitmasking negative number check
    public static boolean isNeg(double d) {
        return (Double.doubleToLongBits(d) & 0x8000000000000000L) != 0;
    }

    public static boolean inRange(double a, double b, double range) {
        return Precision.equals(Math.abs(a-b), 0, range+Precision.EPSILON);
    
    }
}
