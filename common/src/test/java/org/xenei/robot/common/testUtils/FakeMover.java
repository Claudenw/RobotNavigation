package org.xenei.robot.common.testUtils;

import org.locationtech.jts.geom.Coordinate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.mover.BaseMover;
import org.xenei.robot.common.sensor.bump.BumpSensorModel;
import org.xenei.robot.common.DeadReckoning;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.utils.RobutContext;

public class FakeMover extends BaseMover {
    private static final Logger LOG = LoggerFactory.getLogger(FakeMover.class);
    private final DeadReckoning deadReckoning;

    public FakeMover(RobutContext ctxt, Coordinate initial) {
        super(ctxt, new DeadReckoning(ctxt, Position.from(initial)), new BumpSensorModel(ctxt, 8));
        this.deadReckoning = (DeadReckoning) this.compass;
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
            accept(MotorState.RUN);
            ctxt.submit(result);
        } finally {
            deadReckoning.track(null);
        }
    }

    public class StepMonitor implements org.xenei.robot.common.StepMonitor, Runnable {
        private int leftSteps;
        private int rightSteps;
        private final int leftLimit;
        private final int rightLimit;
        private final int leftIncrement;
        private final int rightIncrement;

        private StepMonitor(int leftLimit, int rightLimit) {
            this.leftLimit = leftLimit;
            this.leftIncrement = leftLimit < 0 ? -1 : 1;
            this.rightLimit = rightLimit;
            this.rightIncrement = rightLimit < 0 ? -1 : 1;
        }

        @Override
        public boolean hasStepDifferential() {
            return leftSteps != rightSteps;
        }

        @Override
        public double leftRotation() {
            return ctxt.chassisInfo.rotation(leftSteps);
        }

        @Override
        public double rightRotation() {
            return ctxt.chassisInfo.rotation(rightSteps);
        }

        @Override
        public int leftSteps() {
            return leftSteps;
        }

        @Override
        public int rightSteps() {
            return rightSteps;
        }

        @Override
        public void run() {
            // read the motor state
            while (!MotorState.RUN.equals(getMotorState())) {
                // do not merge the following 2 lines or the logic will short circuit.
                boolean keepRunning = leftSteps < leftLimit;
                keepRunning |= rightSteps < rightLimit;
                if (keepRunning) {
                    if (leftSteps != leftLimit) {
                        leftSteps += leftIncrement;
                    }
                    if (rightSteps != rightLimit) {
                        rightSteps += rightIncrement;
                    }
                    sleep();
                } else {
                    accept(MotorState.STOP);
                }
            }
        }
    }
}
