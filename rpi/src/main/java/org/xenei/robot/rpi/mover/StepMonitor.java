package org.xenei.robot.rpi.mover;

import org.xenei.robot.common.DeadReckoning;
import org.xenei.robot.rpi.drivers.Motor;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

public class StepMonitor implements Runnable, DeadReckoning.StepMonitor {
    private final Motor.SteppingStatus ssLeft;
    private final Motor.SteppingStatus ssRight;
    private final AtomicBoolean stopped = new AtomicBoolean(false);

    StepMonitor(Motor.SteppingStatus ssLeft, Motor.SteppingStatus ssRight) {
        this.ssLeft = ssLeft;
        this.ssRight = ssRight;
    }

    @Override
    public void stop() {
        stopped.set(true);
    }

    @Override
    public void run()  {
        while (!stopped.get()) {
            // do not merge the following 2 lines or the logic will short circuit.
            boolean keepRunning = ssLeft.step();
            keepRunning |= ssRight.step();
            stopped.compareAndExchange(false, !keepRunning);
        }
    }

    @Override
    public boolean hasStepDifferential() {
        return ssLeft.fwdSteps() != ssRight.fwdSteps();
    }

    @Override
    public double leftRotation() {
        return ssLeft.fwdRotation();
    }

    @Override
    public double rightRotation() {
        return ssRight.fwdRotation();
    }

    @Override
    public int leftSteps() {
        return ssLeft.fwdSteps();
    }

    @Override
    public int rightSteps() {
        return ssRight.fwdSteps();
    }
}
