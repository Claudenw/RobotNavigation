package org.xenei.robot.common;

public class ChassisInfo {
    public final double radius;

    /**
     * 
     * @param maxWidth the maximum width of the chassis in meters.
     */
    public ChassisInfo(double maxWidth) {
        radius = maxWidth/2.0;
    }

    public double maxWidth() {
        return radius * 2.0;
    }
}
