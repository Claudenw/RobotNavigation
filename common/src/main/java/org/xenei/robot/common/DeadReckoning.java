package org.xenei.robot.common;

import org.locationtech.jts.geom.Coordinate;
import org.xenei.robot.common.mapping.ThetaAndRange;
import org.xenei.robot.common.utils.AngleUtils;
import org.xenei.robot.common.utils.CoordUtils;
import org.xenei.robot.common.utils.DoubleUtils;
import org.xenei.robot.common.utils.RobutContext;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Compass that determines position by dead reckoning.
 */
public final class DeadReckoning implements Compass, Supplier<Position> {
    /** The current calculated position */
    private final AtomicReference<Position> position;
    /** The robut context we are wroking in */
    private final RobutContext ctxt;
    /** The external position supplier */
    private final Supplier<Position> positionSupplier;
    /** The current step monitor */
    private StepMonitor currentMonitor;

    public static DeadReckoning from(RobutContext ctxt, Supplier<Position> positionSupplier) {
        if (positionSupplier instanceof DeadReckoning) {
            return (DeadReckoning) positionSupplier;
        }
        return new DeadReckoning(ctxt, positionSupplier);
    }

    public static DeadReckoning from(RobutContext ctxt, Position initialPosition) {
        return new DeadReckoning(ctxt, initialPosition);
    }

    private DeadReckoning(RobutContext ctxt, Supplier<Position> positionSupplier) {
        this.ctxt = ctxt;
        this.positionSupplier = positionSupplier;
        this.position = new AtomicReference<>(positionSupplier.get());
    }

    private DeadReckoning(RobutContext ctxt, Position position) {
        this.ctxt = ctxt;
        this.position = new AtomicReference<>(position);
        this.positionSupplier = null;
    }

    @Override
    public double heading() {
        if (currentMonitor != null) {
            return position.get().heading()
                    + ctxt.chassisInfo.theta(currentMonitor.leftSteps(), currentMonitor.rightSteps());
        }
        return get().heading();
    }

    Position nextPosition(Position start, ThetaAndRange relativeCoordinates) {
        if (relativeCoordinates.range() == 0 && relativeCoordinates.theta() == 0) {
            return start;
        }
        double newHeading = AngleUtils.normalize(start.heading() + relativeCoordinates.theta());
        Coordinate newCoordinate = CoordUtils.fromAngle(newHeading, relativeCoordinates.range());

        return Position.asPosition(newCoordinate, newHeading);
    }

    @Override
    public Position get() {
        if (currentMonitor != null) {
            return nextPosition(position.get(), ctxt.chassisInfo.thetaAndRange(currentMonitor));
        }
        return positionSupplier == null ? position.get() : positionSupplier.get();
    }

    @Override
    public int headingAccuracy() {
        return 2;
    }

    /**
     * Track heading based on the step monitor.
     *
     * @param stepMonitor
     */
    public void track(StepMonitor stepMonitor) {
        if (currentMonitor != null) {
            if (currentMonitor.hasStepDifferential()) {
                position.getAndUpdate(p -> Position.PositionUtils.nextPosition(p, ctxt.chassisInfo.thetaAndRange(currentMonitor)));
            } else {
                double range = ctxt.chassisInfo.range(currentMonitor.leftRotation());
                position.getAndUpdate(p -> Position.PositionUtils.nextPosition(p, range));
            }
        } else {
            if (positionSupplier != null) {
                position.set(positionSupplier.get());
            }
        }
        this.currentMonitor = stepMonitor;
    }

    @Override
    public String toString() {
        double h = heading();
        return String.format("DeadReckoning[%s Heading: %s %s degrees]", get(), h,
                DoubleUtils.round(Math.toDegrees(h), headingAccuracy() + 1));
    }
}
