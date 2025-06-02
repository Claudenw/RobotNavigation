package org.xenei.robot.rpi.mover;

import org.xenei.robot.common.Compass;
import org.xenei.robot.common.utils.DoubleUtils;

class DeadReckoning implements Compass {
    private static final double STEPS_PER_RADIAN = 640.0 * 10;
    private double heading;
    private StepMonitor currentMonitor;

    @Override
    public double heading() {
        return heading;
    }

    @Override
    public double instantaneousHeading() {
        if (currentMonitor != null) {
            return heading + currentMonitor.stepDifferential() / STEPS_PER_RADIAN;
        }
        return heading;
    }

    @Override
    public double sd() {
        return 0;
    }

    @Override
    public int decimalPlaces() {
        return 2;
    }

    public void track(StepMonitor stepMonitor) {
        if (currentMonitor != null) {
            int stepDifferential = currentMonitor.stepDifferential();
            if (stepDifferential != 0) {
                heading += stepDifferential / STEPS_PER_RADIAN;
            }
        }
        this.currentMonitor = stepMonitor;
    }

    public static int stepsTo(double theta) {
        return (int) Math.round(theta * STEPS_PER_RADIAN / 2);
    }

    @Override
    public String toString() {
        double h = heading();
        double sd = sd();
        return String.format("DeadReckoning[Heading: %s %s degrees]", h, DoubleUtils.round(Math.toDegrees(h), decimalPlaces() + 1), sd);
    }
}
