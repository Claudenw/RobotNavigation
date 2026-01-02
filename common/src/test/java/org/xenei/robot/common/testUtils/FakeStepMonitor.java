package org.xenei.robot.common.testUtils;

import org.xenei.robot.common.ChassisInfo;
import org.xenei.robot.common.Mover;
import org.xenei.robot.common.utils.RobutContext;

import java.util.concurrent.atomic.AtomicReference;

import static org.apache.jena.http.auth.AuthEnv.LOG;

public class FakeStepMonitor implements org.xenei.robot.common.StepMonitor, Runnable {
    private int leftSteps;
    private int rightSteps;
    private final int leftLimit;
    private final int rightLimit;
    private final int leftIncrement;
    private final int rightIncrement;
    private final ChassisInfo chassisInfo;
    private final AtomicReference<Byte> motorState;
    private final RobutContext.ByteTopic motorStateTopic;

    public FakeStepMonitor(RobutContext ctxt, int leftLimit, int rightLimit) {
        this.motorState = new AtomicReference<>(Mover.MotorState.STOP);
        ctxt.motorStateTopic.listen(i -> motorState.set((byte)i));
        motorStateTopic = ctxt.motorStateTopic;
        this.chassisInfo = ctxt.chassisInfo;
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
        return chassisInfo.rotation(leftSteps);
    }

    @Override
    public double rightRotation() {
        return chassisInfo.rotation(rightSteps);
    }

    @Override
    public int leftSteps() {
        return leftSteps;
    }

    @Override
    public int rightSteps() {
        return rightSteps;
    }

    public void takeStep() {
        if (leftSteps != leftLimit) {
            leftSteps += leftIncrement;
        }
        if (rightSteps != rightLimit) {
            rightSteps += rightIncrement;
        }
        sleep();
    }

    @Override
    public void run() {
        // read the motor state
        while (Mover.MotorState.RUN != motorState.get()) {
            // do not merge the following 2 lines or the logic will short circuit.
            boolean keepRunning = leftSteps < leftLimit;
            keepRunning |= rightSteps < rightLimit;
            if (keepRunning) {
                takeStep();
            } else {
                motorStateTopic.send(Mover.MotorState.STOP);
            }
        }
    }

    /**
     * Sleeps for the time it takes to take 1 step
     */
    private void sleep() {
        try {
            Thread.sleep((int) chassisInfo.motorInfo.freq());
        } catch (InterruptedException e) {
            LOG.warn("Interrupted while waiting for sleep", e);
            motorStateTopic.send(Mover.MotorState.STOP);
        }
    }
}
