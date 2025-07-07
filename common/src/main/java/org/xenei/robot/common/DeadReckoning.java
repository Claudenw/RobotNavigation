package org.xenei.robot.common;

import org.xenei.robot.common.utils.DoubleUtils;
import org.xenei.robot.common.utils.RobutContext;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class DeadReckoning implements Compass, Supplier<Position> {
    private static final double STEPS_PER_RADIAN = 640.0 * 10;
    private final AtomicReference<Position> position;
    private StepMonitor currentMonitor;
    private final RobutContext ctxt;

    public DeadReckoning(RobutContext ctxt) {
        this(ctxt, Position.from(0.0, 0.0, 0.0));
    }
    public DeadReckoning(RobutContext ctxt, Position initialPosition) {
        this.ctxt = ctxt;
        position = new AtomicReference<>(initialPosition);
    }

    @Override
    public double heading() {
        return position.get().getHeading();
    }

    @Override
    public Position get() {
        return position.get();
    }

    @Override
    public double instantaneousHeading() {
        if (currentMonitor != null) {
            return position.get().getHeading() + ctxt.chassisInfo.theta(currentMonitor.leftSteps(), currentMonitor.rightSteps());
        }
        return position.get().getHeading();
    }

    @Override
    public double sd() {
        return 0;
    }

    @Override
    public int decimalPlaces() {
        return 2;
    }

    double calcRange(double leftArc, double rightArc) {
        if (leftArc >= 0) {
            if (rightArc >= 0) {
                return 2*leftArc - rightArc;
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

    /**
     * Track heading based on the step monitor.
     * @param stepMonitor
     */
    public void track(StepMonitor stepMonitor) {
        if (currentMonitor != null) {
            if (currentMonitor.hasStepDifferential()) {
                double leftRange = ctxt.chassisInfo.range(currentMonitor.leftRotation());
                double leftArc = ctxt.chassisInfo.pivotAngle(leftRange);

                double rightRange = ctxt.chassisInfo.range(currentMonitor.rightRotation());
                double rightArc = ctxt.chassisInfo.pivotAngle(rightRange);

                double range = calcRange(leftArc, rightArc);
                double theta = ctxt.chassisInfo.theta(currentMonitor.leftSteps(), currentMonitor.rightSteps());
                position.getAndUpdate(p -> p.addHeading(theta).nextPosition(range));
            } else {
                double range =  ctxt.chassisInfo.range(currentMonitor.leftRotation());
                position.getAndUpdate(p -> p.nextPosition(range));
            }
        }
        this.currentMonitor = stepMonitor;
    }

    @Override
    public String toString() {
        double h = heading();
        double sd = sd();
        return String.format("DeadReckoning[Heading: %s %s degrees]", h, DoubleUtils.round(Math.toDegrees(h), decimalPlaces() + 1));
    }

   public interface StepMonitor {
        void stop();

        boolean hasStepDifferential();

        double leftRotation();

        double rightRotation();

        int leftSteps();

        int rightSteps();
    }
}
