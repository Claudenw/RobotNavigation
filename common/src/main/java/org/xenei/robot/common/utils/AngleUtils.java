package org.xenei.robot.common.utils;

public class AngleUtils {
    private AngleUtils() {}
    
    public static final double normalize(double angle) {
        if (Double.isNaN(angle)) {
            return 0.0;
        }
        return Math.atan2(Math.sin(angle), Math.cos(angle));
    }
}
