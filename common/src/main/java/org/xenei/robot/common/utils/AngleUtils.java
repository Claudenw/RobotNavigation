package org.xenei.robot.common.utils;

public class AngleUtils {
    
    public static final double RADIANS_45 = Math.PI / 4;
    public static final double RADIANS_90 = 2 * RADIANS_45;
    public static final double RADIANS_135 = 3 * RADIANS_45;
    public static final double RADIANS_180 = Math.PI;
    public static final double RADIANS_225 = -RADIANS_135;
    public static final double RADIANS_270 = -RADIANS_90;
    public static final double RADIANS_315 = -RADIANS_45;
    
    private AngleUtils() {}
    
    public static final double normalize(double angle) {
        if (Double.isNaN(angle)) {
            return 0.0;
        }
        double d = Math.atan2(Math.sin(angle), Math.cos(angle));
        return DoubleUtils.eq(-Math.PI, d) ? Math.PI : d;
    }
   
}
