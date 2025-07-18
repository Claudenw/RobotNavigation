package org.xenei.robot.common;

import org.xenei.robot.common.utils.DoubleUtils;
import org.xenei.robot.common.utils.RobutContext;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Compass that determines position by dead reckoning.
 */
public class DeadReckoning implements Compass, Supplier<Position> {
    private final AtomicReference<Position> position;
    private StepMonitor currentMonitor;
    private final RobutContext ctxt;

    /**
     * Constructor that defaults to position at origin with heading of 0.
     * @param ctxt the robut context to work with.
     */
    public DeadReckoning(RobutContext ctxt) {
        this(ctxt, Position.from(0.0, 0.0, 0.0));
    }

    /**
     * Constructor.
     * @param ctxt the robut context to work with.
     * @param initialPosition the inital position.
     */
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
        if (currentMonitor != null) {
            ThetaAndRange thetaAndRange = ctxt.chassisInfo.thetaAndRange(currentMonitor);
            return position.get().nextPosition(thetaAndRange);
        }
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
                position.getAndUpdate(p -> p.nextPosition(ctxt.chassisInfo.thetaAndRange(stepMonitor)));
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
}
