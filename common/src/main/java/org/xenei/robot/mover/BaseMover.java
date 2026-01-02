package org.xenei.robot.mover;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.sensor.bump.BumpSensorModel;
import org.xenei.robot.common.Mover;
import org.xenei.robot.common.utils.AngleUtils;
import org.xenei.robot.common.utils.RobutContext;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

public abstract class BaseMover implements Mover {

    private static final Logger LOG = LoggerFactory.getLogger(BaseMover.class);

    ///  package private for testing
    final AtomicReference<Byte> motorState;
    protected final BumpSensorModel bumpSensorModel;
    protected final Supplier<Position> positionSupplier;
    protected final RobutContext ctxt;
    protected final CopyOnWriteArrayList<LogicModule> logicModules;
    protected final Lock logicModuleLock;
    private final long sleepTime;

    /**
     * Constructs the base mover operating the specified context.
     * @param ctxt the context to operate in.
     * @param positionSupplier the position supplier for the most recent position.
     * @param bumpSensorModel the bump sensor model to operate against.
     */
    protected BaseMover(final RobutContext ctxt, final Supplier<Position> positionSupplier, final BumpSensorModel bumpSensorModel) {
        this.ctxt = ctxt;
        this.positionSupplier = positionSupplier;
        this.bumpSensorModel = bumpSensorModel;
        this.motorState = new AtomicReference<>(MotorState.STOP);
        this.logicModules = new CopyOnWriteArrayList<>();
        this.logicModuleLock = new ReentrantLock();
        this.sleepTime = ctxt.chassisInfo.motorInfo.freq();
        this.ctxt.motorStateTopic.listen(i -> motorState.set((byte)i));
        this.ctxt.moveToTopic.listen(location -> move(positionSupplier.get().relativeLocation(location)));
    }


    @Override
    public void close() {
        logicModules.forEach(LogicModule::shutdown);
    }

    /**
     * Sleeps for the time it takes to take 1 step.
     */
    final public void sleep() {
        sleep(1);
    }

    /**
     * Sleeps for the time it takes to take {@code steps} steps.
     *
     * @param steps
     *            the number of steps to sleep through.
     */
    final public void sleep(int steps) {
        try {
            Thread.sleep(sleepTime * steps);
        } catch (InterruptedException e) {
            LOG.warn("Interrupted while waiting for sleep", e);
            ctxt.motorStateTopic.send(MotorState.STOP);
        }
    }

    @Override
    public void register(LogicModule logicModule) {
        logicModules.add(logicModule);
        logicModule.setLock(logicModuleLock);
    }

    protected byte getMotorState() {
        return motorState.get();
    }

    @Override
    final public void move(Location location) {
        Position currentPosition = position();
        Position nxt = Position.PositionUtils.nextPosition(currentPosition, location);
        setHeading(currentPosition.headingTo(nxt));
        int rangeSteps = ctxt.chassisInfo.steps(location.range());
        takeSteps(rangeSteps, rangeSteps, (byte) 0);
        System.out.println( "LOCATION NOW: " + position());
    }

    @Override
    final public void setHeading(double heading) {
        double headingDiff = positionSupplier.get().heading() - heading;
        double theta = AngleUtils.normalize(headingDiff);
        int thetaSteps = ctxt.chassisInfo.rotateSteps(theta);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Setting heading: {} {} degrees sweeping through {} degrees of arc", heading,
                    Math.toDegrees(heading), Math.toDegrees(theta));
        }
        if (thetaSteps != 0) {
            takeSteps(thetaSteps, -thetaSteps, (byte) 0);
            LOG.debug("Heading {} achieved. {}", heading, positionSupplier.get());
        }
    }

    /**
     * move the system.
     *
     * @param left
     *            number of steps to take on the left side.
     * @param right
     *            the number of steps to take on the right side.
     * @param lastSensor
     *            the last bump sensor reading.
     */
    public abstract void takeSteps(int left, int right, byte lastSensor);

}
