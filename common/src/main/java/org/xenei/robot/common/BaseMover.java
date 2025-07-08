package org.xenei.robot.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.common.utils.AngleUtils;
import org.xenei.robot.common.utils.DoubleUtils;
import org.xenei.robot.common.utils.RobutContext;
import org.xenei.robot.ml.SensorLayer;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public abstract class BaseMover implements Mover {

    private static final Logger LOG = LoggerFactory.getLogger(BaseMover.class);

    protected final BumpSensorModel bumpSensorModel;
    protected final Compass compass;
    protected final RobutContext ctxt;
    private final AtomicReference<MotorState> motorState;

    protected BaseMover(RobutContext ctxt, Compass compass, BumpSensorModel bumpSensorModel) {
        this.ctxt = ctxt;
        this.compass = compass;
        this.bumpSensorModel = bumpSensorModel;
        this.motorState = new AtomicReference<>(MotorState.STOP);
    }

    final public Consumer<BumpSensor.BumpState> getBumpSensorListener() {
        return bumpSensorModel;
    }

    protected MotorState getMotorState() {
        return motorState.get();
    }

    public void accept(MotorState motorState) {
        this.motorState.set(motorState);
    }

    @Override
    final public Position move(Location location) {
        Position currentPosition = position();
        Position nxt = currentPosition.nextPosition(location);
        accept(MotorState.RUN);
        setHeading(currentPosition.headingTo(nxt));
        int rangeSteps = ctxt.chassisInfo.steps(location.range());
        takeSteps(rangeSteps, rangeSteps, (byte)0).ifPresent(this::fixBumpSensor);
        return position();
    }

    @Override
    final public void setHeading(double heading) {
        double headingDiff = compass.heading() - heading;

        while (makeInternalHeading(heading)) {
            double newHeadingDiff = compass.heading() - heading;
            LOG.debug("old diff heading {} - new diff heading {} = {}", headingDiff, newHeadingDiff,
                    headingDiff - newHeadingDiff);
        }
        LOG.debug("Heading {} achieved. {}", heading, compass);
    }

    /**
     * move the system.
     * @param left number of steps to take on the left side.
     * @param right the number of steps to take on the right side.
     * @return A SensorLayer if the bumper sensor triggered.
     */
    protected abstract Optional<SensorLayer> takeSteps(int left, int right, byte lastSensor);


    private void fixBumpSensor(SensorLayer sensorLayer) {
        int rangeSteps = ctxt.chassisInfo.steps(0.01);
        Optional<SensorLayer> nextLayer = Optional.empty();
        while (sensorLayer != null) {
            switch (sensorLayer.getAnswer()) {
                case FF -> {
                    nextLayer = takeSteps(rangeSteps, rangeSteps, sensorLayer.getTrigger());
                }
                case FS -> {
                    nextLayer = takeSteps(rangeSteps, 0, sensorLayer.getTrigger());
                }
                case FR -> {
                    nextLayer = takeSteps(rangeSteps, -rangeSteps, sensorLayer.getTrigger());
                }
                case SF -> {
                    nextLayer = takeSteps(0, rangeSteps, sensorLayer.getTrigger());
                }
                case SS -> {
                    nextLayer = takeSteps(0, 0, sensorLayer.getTrigger());
                }
                case SR -> {
                    nextLayer = takeSteps(0, -rangeSteps, sensorLayer.getTrigger());
                }
                case RF -> {
                    nextLayer = takeSteps(-rangeSteps, rangeSteps, sensorLayer.getTrigger());
                }
                case RS -> {
                    nextLayer = takeSteps(-rangeSteps, 0, sensorLayer.getTrigger());
                }
                case RR -> {
                    nextLayer = takeSteps(-rangeSteps, -rangeSteps, sensorLayer.getTrigger());
                }
                case DONT_CARE -> {
                    LOG.error("Invalid SensorLayer answer: DONT_CARE ");
                    return;
                }
            }
            if (nextLayer.isPresent()) {
                sensorLayer.feedback(nextLayer.get().getTrigger());
            }
            sensorLayer = nextLayer.orElse(null);
        }
    }

    /**
     * Change our heading to {@code heading}
     * @param heading the heading to achieve.
     * @return {@code true} if the heading changed, {@code false} otherwise.
     */
    private boolean makeInternalHeading(double heading) {
        // theta r is the distance the wheel has to move to pass through the arc from
        // to make the direction change.
        double theta = AngleUtils.normalize(compass.instantaneousHeading()-heading);
        int thetaSteps = ctxt.chassisInfo.rotateSteps(theta);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Setting heading: {} {} degrees sweeping through {} degrees of arc", heading,
                    Math.toDegrees(heading), Math.toDegrees(theta));
        }
        if (thetaSteps == 0) {
            return false;
        }
        takeSteps(thetaSteps, -thetaSteps, (byte)0);
        LOG.debug("{}", compass);
        return true;
    }
}
