package org.xenei.robot.mover;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.common.Mover;
import org.xenei.robot.common.sensor.bump.BumpSensorModel;
import org.xenei.robot.common.utils.RobutContext;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;

/**
 * starts with accept sensor layer. select a corrective action. ignore sensor
 * layer while taking corrective action. move enough to untrigger event. check
 * sensor layer if pause go to select a corrective action if run: set stop if
 * stop: reset trigger?
 */
public class BumpSensorLogicModule implements Mover.LogicModule {
    private static final Logger LOG = LoggerFactory.getLogger(BumpSensorLogicModule.class);

    private final int rangeSteps;
    private final BaseMover mover;
    private final AtomicBoolean sensorLayerEnabled;
    private Lock lock;
    private final RobutContext.ByteTopic motorStateTopic;
    private final RobutContext.TopicRegistration bumpSensorRegistration;

    public BumpSensorLogicModule(RobutContext ctxt, BaseMover mover) {
        motorStateTopic = ctxt.motorStateTopic;
        sensorLayerEnabled = new AtomicBoolean(true);
        rangeSteps = ctxt.chassisInfo.steps(ctxt.scaleInfo.getResolution());
        this.mover = mover;
        this.mover.register(this);
        this.bumpSensorRegistration = ctxt.bumpSensorTopic.listen(this::processSensorLayer);
    }

    public void shutdown() {
        bumpSensorRegistration.unsubscribe();
    }

    /**
     * @param sensorResult
     *            the result from the Sensor model.
     */
    private void processSensorLayer(BumpSensorModel.SensorResult sensorResult) {
        if (sensorLayerEnabled.get() && lock.tryLock()) {
            try {
                motorStateTopic.send(Mover.MotorState.PAUSE);
                sensorLayerEnabled.set(false);
                takeCorrectiveAction(sensorResult);
            } finally {
                lock.unlock();
                sensorLayerEnabled.set(true);
            }
        }
    }

    private void takeCorrectiveAction(BumpSensorModel.SensorResult sensorLayer) {
        try {
            switch (sensorLayer.getAnswer()) {
                case FF -> mover.takeSteps(rangeSteps, rangeSteps, sensorLayer.bumpState());
                case FS -> mover.takeSteps(rangeSteps, 0, sensorLayer.bumpState());
                case FR -> mover.takeSteps(rangeSteps, -rangeSteps, sensorLayer.bumpState());
                case SF -> mover.takeSteps(0, rangeSteps, sensorLayer.bumpState());
                case SS -> mover.takeSteps(0, 0, sensorLayer.bumpState());
                case SR -> mover.takeSteps(0, -rangeSteps, sensorLayer.bumpState());
                case RF -> mover.takeSteps(-rangeSteps, rangeSteps, sensorLayer.bumpState());
                case RS -> mover.takeSteps(-rangeSteps, 0, sensorLayer.bumpState());
                case RR -> mover.takeSteps(-rangeSteps, -rangeSteps, sensorLayer.bumpState());
                case DONT_CARE -> {
                    LOG.error("Invalid SensorLayer answer: DONT_CARE ");
                    return;
                }
            }
            mover.sleep(rangeSteps);
        } catch (IllegalArgumentException e) {
            LOG.error("Invalid SensorLayer answer", e);
        }
        while (Mover.MotorState.RUN == mover.getMotorState()) {
            mover.sleep();
        }
    }

    @Override
    public void setLock(Lock lock) {
        this.lock = lock;
    }
}
