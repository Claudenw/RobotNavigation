package org.xenei.robot.common;

import org.xenei.robot.common.utils.AngleUtils;

import java.time.Duration;

public class ChassisInfo {
    /**
     * The buffer needed from the center of the chassis to the outside edge.
     */
    public final double radius;
    /** The diameter of the wheels in cm. */
    public final double wheelDiameter;
    /** the maximum speed of the chassis in m/min */
    public final double maxSpeed; // m/min
    /**
     * Step is a chassis unit of distance traveled. This is the conversion to
     * meters. A step is used in the motor assemblies.
     */
    public final double metersPerStep;

    public final double stepsPerRotation;

    public final MotorInfo motorInfo;

    public static Builder builder() {
        return new Builder();
    }
    /**
     * Constructor.
     *
     * @param radius
     *            the radius of the chassis.
     * @param wheelDiameter
     *            in m
     * @param motorInfo
     *            the info for the motor.
     */
    private ChassisInfo(double radius, double wheelDiameter, MotorInfo motorInfo) {
        this.radius = radius;
        this.wheelDiameter = wheelDiameter;
        this.metersPerStep = metersPerStep(motorInfo.stepAngle(), wheelDiameter);
        this.stepsPerRotation = stepsPerRotation(motorInfo.stepAngle());
        long stepsPerMinute = Duration.ofMinutes(1).toMillis() / motorInfo.freq();
        this.maxSpeed = metersPerStep * stepsPerMinute;
        this.motorInfo = motorInfo;
    }

    public double width() {
        return radius * 2.0;
    }

    /**
     *
     * @param stepAngle
     *            in radians per step.
     * @param wheelDiameter
     *            in m
     * @return meters per step
     */
    private static double metersPerStep(double stepAngle, double wheelDiameter) {
        double metersPerRev = Math.PI * wheelDiameter;
        return metersPerRev / stepsPerRotation(stepAngle);
    }

    private static double stepsPerRotation(double stepAngle) {
        return AngleUtils.PI_x_2 / stepAngle;
    }

    public int rotateSteps(double theta) {
        return (int) Math.round(theta * radius / metersPerStep);
    }

    public double rotation(int steps) {
        // meters = PI * wheelDiameter * rotation => rotation = meters / (PI *
        // wheelDiameter)
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

    double calcRange(double leftArc, double rightArc) {
        if (leftArc >= 0) {
            if (rightArc >= 0) {
                return 2 * leftArc - rightArc;
            } else {
                return leftArc + rightArc;
            }
        } else {
            if (rightArc >= 0) {
                return leftArc + rightArc;
            } else {
                return 2 * leftArc - rightArc;
            }
        }
    }

    public ThetaAndRange thetaAndRange(StepMonitor stepMonitor) {
        double leftRange = range(stepMonitor.leftRotation());
        double leftArc = pivotAngle(leftRange);

        double rightRange = range(stepMonitor.rightRotation());
        double rightArc = pivotAngle(rightRange);

        return new ThetaAndRange(theta(stepMonitor.leftSteps(), stepMonitor.rightSteps()),
                calcRange(leftArc, rightArc));
    }

    /**
     * Estimate the number of steps to achieve a specific range.
     *
     * @param range
     *            the range to achieve
     * @return the number of steps to get there.
     */
    public int steps(double range) {
        return (int) Math.round(range / metersPerStep);
    }

    public double theta(int leftSteps, int rightSteps) {
        double leftPart = pivotAngle(leftSteps * metersPerStep);
        double rightPart = pivotAngle(rightSteps * metersPerStep);
        return -leftPart + rightPart;
    }

    public static class Builder {
        double width;
        double wheelDiameterInCm;
        MotorInfo motorInfo;

        public Builder width(double width) {
            this.width = width;
            return this;
        }
        public Builder wheelSize(double wheelDiameterInCm) {
            this.wheelDiameterInCm = wheelDiameterInCm;
            return this;
        }
        public Builder motorInfo(MotorInfo motorInfo) {
            this.motorInfo = motorInfo;
            return this;
        }

        public ChassisInfo build() {
            double wheelDiameter = wheelDiameterInCm / 100;
            return new ChassisInfo(width, wheelDiameter, motorInfo);
        }
    }
}
