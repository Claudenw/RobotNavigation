package org.xenei.robot.rpi.mover;

import org.xenei.robot.rpi.drivers.Motor;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

public class StepMonitor implements Callable<StepMonitor> {
    private final Motor.SteppingStatus ssLeft;
    private final Motor.SteppingStatus ssRight;
    private final AtomicBoolean stopped = new AtomicBoolean(false);

    StepMonitor(Motor.SteppingStatus ssLeft, Motor.SteppingStatus ssRight) {
        this.ssLeft = ssLeft;
        this.ssRight = ssRight;
    }

    public void stop() {
        stopped.set(true);
    }

    @Override
    public StepMonitor call() throws Exception {
        while (!stopped.get()) {
            // do not merge the following 2 lines or the logic will short circuit.
            boolean keepRunning = ssLeft.step();
            keepRunning |= ssRight.step();
            stopped.compareAndExchange(false, !keepRunning);
        }
        return this;
    }

    public int stepDifferential() {
        return ssLeft.fwdSteps() - ssRight.fwdSteps();
    }
}
