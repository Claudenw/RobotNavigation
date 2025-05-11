package org.xenei.robot.common.utils;

public class AngleUtils {

    /** the number of radian in 45 degrees */
    public static final double RADIANS_45 = Math.PI / 4;
    /** the number of radian in 90 degrees */
    public static final double RADIANS_90 = 2 * RADIANS_45;
    /** the number of radian in 135 degrees */
    public static final double RADIANS_135 = 3 * RADIANS_45;
    /** the number of radian in 180 degrees */
    public static final double RADIANS_180 = Math.PI;
    /** the number of radian in 225 degrees */
    public static final double RADIANS_225 = -RADIANS_135;
    /** the number of radian in 270 degrees */
    public static final double RADIANS_270 = -RADIANS_90;
    /** the number of radian in 315 degrees */
    public static final double RADIANS_315 = -RADIANS_45;
    /** 2pi */
    public static final double PI_x_2 = 2 * Math.PI;
   
    
    /**
     * The point at which rounding errors appear in angular calculations.
     */
    public static final double TOLERANCE = 0.000000000000001;

    private AngleUtils() {
    }

    public static final double normalize(double angle) {
        if (Double.isNaN(angle)) {
            return 0.0;
        }
        double d = Math.atan2(Math.sin(angle), Math.cos(angle));
        return DoubleUtils.eq(-Math.PI, d) ? Math.PI : d;
    }

}
