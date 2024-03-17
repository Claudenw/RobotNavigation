package org.xenei.robot.common;

public class ChassisInfo {
    /**
     * The buffer needed from the center of the chassis
     * to to the outside edge.
     */
    public final double radius;
    /** The diameter of the wheels in cm. */
    public final int wheelDiameter; 
    /** the maximum speed of the chassis in m/min */
    public final double maxSpeed; // m/min

    /**
     * Constructor.
     * @param maxWidth the maximum width of the chassis in meters.
     * @param wheelDiameter in cm
     * @param maxSpeed in m/min
     */
    public ChassisInfo(double maxWidth, int wheelDiameter, double maxSpeed) {
        radius = maxWidth/2.0;
        this.wheelDiameter = wheelDiameter;
        this.maxSpeed = maxSpeed;
    }

    public double width() {
        return radius * 2.0;
    }
    
    public class Builder {
        
    }
}
