package org.xenei.robot.common;

import org.xenei.robot.common.utils.AngleUtils;

public class ChassisInfo {
    /**
     * The buffer needed from the center of the chassis
     * to the outside edge.
     */
    public final double radius;
    /** The diameter of the wheels in cm. */
    public final double wheelDiameter;
    /** the maximum speed of the chassis in m/min */
    public final double maxSpeed; // m/min
    /**
     * Step is a chassis unit of distance traveled.  This is the conversion to meters.
     * A step is used in the motor assemblies.
     */
    public final double metersPerStep;

    public static Builder builder() {
        return new Builder();
    }
    /**
     * Constructor.
     * @param radius the radius of the chassis.
     * @param wheelDiameter in m
     * @param maxSpeed in m/min
     */
    private ChassisInfo(double radius, double wheelDiameter, double maxSpeed, double metersPerStep) {
        this.radius = radius;
        this.wheelDiameter = wheelDiameter;
        this.maxSpeed = maxSpeed;
        this.metersPerStep = metersPerStep;
    }

    public double width() {
        return radius * 2.0;
    }

    public int rotateSteps(double theta) {
        return (int) Math.round(theta * radius / metersPerStep);
    }

    public double rotation(int steps) {
        // meters = PI * wheelDiameter * rotation => rotation = meters / (PI * wheelDiameter)
        return metersPerStep * steps / (Math.PI * wheelDiameter);
    }

    public double rotateAngle(double range) {
        return range / radius;
    }

    public int pivotSteps(double theta) {
        return (int) Math.round(theta * width() / metersPerStep);
    }

    public double pivotAngle(double range) {
        return range / width();
    }

    public double range(double theta) {
        return theta * wheelDiameter * Math.PI;
    }

    public int steps(double range) {
        return (int) Math.round(range / metersPerStep);
    }

    public double theta(int leftSteps , int rightSteps) {
        double leftPart = pivotAngle(leftSteps * metersPerStep);
        double rightPart = pivotAngle(rightSteps * metersPerStep);
        return -leftPart + rightPart;
    }

    public static class Builder {
        double width;
        double wheelDiameterInCm;
        double motorFreq;
        double stepAngle;

        public Builder width(double width) {
            this.width = width;
            return this;
        }
        public Builder wheelSize(double wheelDiameterInCm) {
            this.wheelDiameterInCm = wheelDiameterInCm;
            return this;
        }
        public Builder motorFreq(double motorFreq) {
            this.motorFreq = motorFreq;
            return this;
        }
        public Builder stepAngle(double stepAngle) {
            this.stepAngle = stepAngle;
            return this;
        }
        public ChassisInfo build() {
            return new ChassisInfo(width, wheelDiameterInCm, motorFreq, metersPerStep(stepAngle, wheelDiameterInCm / 100));
        }

        /**
         *
         * @param stepAngle in radians per step.
         * @param wheelDiameter in m
         * @return meters per step
         */
        public static double metersPerStep(double stepAngle, double wheelDiameter) {
            double metersPerRev = Math.PI * wheelDiameter;
            return metersPerRev / stepsPerRotation(stepAngle);
        }

        public static double stepsPerRotation(double stepAngle) {
            return AngleUtils.PI_x_2 / stepAngle;
        }

    }
}
