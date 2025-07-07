package org.xenei.robot.common.testUtils;

import org.locationtech.jts.geom.Coordinate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.common.BaseMover;
import org.xenei.robot.common.BumpDetector;
import org.xenei.robot.common.BumpSensorModel;
import org.xenei.robot.common.DeadReckoning;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.utils.RobutContext;
import org.xenei.robot.ml.SensorLayer;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public class FakeMover extends BaseMover {
    private static final int STEPS_PER_METER = 10;
    private static final Logger LOG = LoggerFactory.getLogger(FakeMover.class);
    private final DeadReckoning deadReckoning;

    public FakeMover(RobutContext ctxt, Coordinate initial) {
        super(ctxt, new DeadReckoning(ctxt, Position.from(initial)), new BumpSensorModel(ctxt, 8));
        this.deadReckoning = (DeadReckoning) this.compass;
        if (LOG.isDebugEnabled()) {
            LOG.debug("Initial position {}", this.position());
        }
        ctxt.scaleInfo.getResolution();
    }


    @Override
    public Position position() {
        return deadReckoning.get();
    }

    @Override
    protected Optional<SensorLayer> takeSteps(int left, int right, byte lastSensor) {
        LOG.debug("Taking steps {} {} ", left, right);
        StepMonitor result = new StepMonitor(left, right);
        BumpDetector bumpChangeDetector = new BumpDetector(result::stop, lastSensor);
        bumpSensorModel.addListener(bumpChangeDetector);
        try {
            deadReckoning.track(result);
            ctxt.submit(result).join();
        } finally {
            bumpSensorModel.removeListener(bumpChangeDetector);
            deadReckoning.track(null);
        }
        return bumpChangeDetector.getSensorLayer();
    }

    class StepMonitor implements DeadReckoning.StepMonitor, Runnable {
        private int leftSteps;
        private int rightSteps;
        private final int leftLimit;
        private final int rightLimit;
        private final int leftIncrement;
        private final int rightIncrement;
        private final AtomicBoolean stopped = new AtomicBoolean(false);


        StepMonitor(int leftLimit, int rightLimit) {
            this.leftLimit = leftLimit;
            this.leftIncrement = leftLimit < 0 ? -1 : 1;
            this.rightLimit = rightLimit;
            this.rightIncrement = rightLimit < 0 ? -1 : 1;
        }

        @Override
        public void stop() {
           stopped.set(true);
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
        public void run()  {
            final long sleepTime = 10;
            while (!stopped.get()) {
                // do not merge the following 2 lines or the logic will short circuit.
                boolean keepRunning = leftSteps < leftLimit;
                keepRunning |= rightSteps < rightLimit;
                stopped.compareAndExchange(false, !keepRunning);
                if (keepRunning) {
                    if (leftSteps != leftLimit) {
                        leftSteps += leftIncrement;
                    }
                    if (rightSteps != rightLimit) {
                        rightSteps += rightIncrement;
                    }
                    try {
                        Thread.sleep(sleepTime);
                    } catch (InterruptedException e) {
                        LOG.warn("Interrupted while waiting for sleep", e);
                        stop();
                    }
                }
            }
        }
    }
}
