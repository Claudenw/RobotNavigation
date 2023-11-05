package org.xenei.robot.navigation;

public class DoubleUtils {
    private DoubleUtils() {}
    
    public static double valueOrZero(double value) {
        return Double.isNaN(value) ? 0.0 : value;
    }
}
