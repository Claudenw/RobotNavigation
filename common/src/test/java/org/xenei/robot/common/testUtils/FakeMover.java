package org.xenei.robot.common.testUtils;

import org.locationtech.jts.geom.Coordinate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.mover.BaseMover;
import org.xenei.robot.common.sensor.bump.BumpSensorModel;
import org.xenei.robot.common.DeadReckoning;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.utils.RobutContext;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.awaitility.Awaitility.await;

public class FakeMover extends BaseMover {
    private static final Logger LOG = LoggerFactory.getLogger(FakeMover.class);
    private final DeadReckoning deadReckoning;

    public FakeMover(RobutContext ctxt, Coordinate initial) {
        super(ctxt, DeadReckoning.from(ctxt, Position.asPosition(initial, 0)), new BumpSensorModel(ctxt, 8));
        this.deadReckoning = (DeadReckoning) this.positionSupplier;
        if (LOG.isDebugEnabled()) {
            LOG.debug("Initial position {}", this.position());
        }
    }

    @Override
    public Position position() {
        return deadReckoning.get();
    }

    @Override
    public void takeSteps(int left, int right, byte lastSensor) {
        LOG.debug("Taking steps {} {} ", left, right);
        StepMonitor result = new StepMonitor(left, right);
        try {
            deadReckoning.track(result);
            ctxt.motorStateTopic.send(MotorState.RUN);
            await().atMost(2, TimeUnit.SECONDS).until(() -> MotorState.RUN == getMotorState());
            result.run();
        } finally {
            deadReckoning.track(null);
        }
    }

    public class StepMonitor implements org.xenei.robot.common.StepMonitor, Runnable {
        private final AtomicInteger leftSteps;
        private final AtomicInteger rightSteps;
        private final int leftLimit;
        private final int rightLimit;
        private final int leftIncrement;
        private final int rightIncrement;

        private StepMonitor(int leftLimit, int rightLimit) {
            this.leftSteps = new AtomicInteger();
            this.rightSteps = new AtomicInteger();
            this.leftLimit = leftLimit;
            this.leftIncrement = leftLimit < 0 ? -1 : 1;
            this.rightLimit = rightLimit;
            this.rightIncrement = rightLimit < 0 ? -1 : 1;
        }

        @Override
        public boolean hasStepDifferential() {
            return leftSteps.get() != rightSteps.get();
        }

        @Override
        public double leftRotation() {
            return ctxt.chassisInfo.rotation(leftSteps.get());
        }

        @Override
        public double rightRotation() {
            return ctxt.chassisInfo.rotation(rightSteps.get());
        }

        @Override
        public int leftSteps() {
            return leftSteps.get();
        }

        @Override
        public int rightSteps() {
            return rightSteps.get();
        }

        @Override
        public void run() {
            // read the motor state
            while (MotorState.RUN == getMotorState()) {
                // do not merge the following 2 lines or the logic will short circuit.
                boolean keepRunning = leftSteps.get() < leftLimit;
                keepRunning |= rightSteps.get() < rightLimit;
                if (keepRunning) {
                    leftSteps.getAndAccumulate(leftIncrement, (x, inc) -> x != leftLimit ? x + inc : x);
                    rightSteps.getAndAccumulate(rightIncrement, (x, inc) -> x != rightLimit ? x + inc : x);
                } else {
                    ctxt.motorStateTopic.send(MotorState.STOP);
                }
            }
        }
    }
}
