package org.xenei.robot.common;

import org.locationtech.jts.geom.Coordinate;
import org.xenei.robot.common.mapping.Map;
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
public class DeadReckoning implements Compass, Supplier<Position> {
    private final AtomicReference<Position> position;
    private StepMonitor currentMonitor;
    private Map map;
    private final RobutContext ctxt;

    /**
     * Constructor that defaults to position at origin with heading of 0.
     *
     * @param ctxt
     *            the RobutContext to work in.
     */
    public DeadReckoning(RobutContext ctxt) {
        this( ctxt, Position.asPosition(Location.ORIGIN, 0));
    }

    /**
     * Constructor that defaults to position at origin with heading of 0.
     *
     * @param ctxt
     *            the RobutContext to work in.
     * @param position The position to start at.
     */
    public DeadReckoning(RobutContext ctxt, Position position) {
        //this.map = map;
        this.ctxt = ctxt;
        this.position = new AtomicReference<>(position);
    }

    @Override
    public double heading() {
        if (currentMonitor != null) {
            return position.get().getHeading()
                    + ctxt.chassisInfo.theta(currentMonitor.leftSteps(), currentMonitor.rightSteps());
        }
        return position.get().getHeading();
    }

    Position nextPosition(Position start, ThetaAndRange relativeCoordinates) {
        if (relativeCoordinates.range() == 0 && relativeCoordinates.theta() == 0) {
            return start;
        }
        double newHeading = AngleUtils.normalize(start.getHeading() + relativeCoordinates.theta());
        Coordinate newCoordinate = CoordUtils.fromAngle(newHeading, relativeCoordinates.range());

        return Position.asPosition(newCoordinate, newHeading);
    }

    @Override
    public Position get() {
        if (currentMonitor != null) {
            return nextPosition(position.get(), ctxt.chassisInfo.thetaAndRange(currentMonitor));
        }
        return position.get();
    }

    @Override
    public double instantaneousHeading() {
        if (currentMonitor != null) {
            return position.get().getHeading()
                    + ctxt.chassisInfo.theta(currentMonitor.leftSteps(), currentMonitor.rightSteps());
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
        }
        this.currentMonitor = stepMonitor;
    }

    @Override
    public String toString() {
        double h = heading();
        double sd = sd();
        return String.format("DeadReckoning[Heading: %s %s degrees]", h,
                DoubleUtils.round(Math.toDegrees(h), decimalPlaces() + 1));
    }
}
