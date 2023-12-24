package org.xenei.robot.common.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.apache.commons.math3.util.Precision;

public class DoubleUtils {
    
    public static final double SQRT2 = Math.sqrt(2.0);
    public static final double DEFAULT_TOLERANCE = Precision.EPSILON;

    
    private DoubleUtils() {}

    // bitmasking negative number check
    public static boolean isNeg(double d) {
        return (Double.doubleToLongBits(d) & 0x8000000000000000L) != 0;
    }

    public static boolean inRange(double a, double b, double tolerance) {
        return inRange(a-b, tolerance);
    }

    public static boolean inRange(double a, double tolerance) {
        return eq(Math.abs(a), 0, tolerance+Precision.EPSILON);
    }
    
    public static boolean eq(double a, double b, double tolerance) {
        return Precision.equals(a, b, tolerance+Precision.EPSILON);
    }
    
    public static boolean eq(double a, double b) {
        return eq(a, b, 0.0);
    }
    
    public static double truncate(double value, int decimalPlaces) {
        return new BigDecimal(String.valueOf(value)).setScale(decimalPlaces, RoundingMode.HALF_UP).doubleValue();
    }
    
}
