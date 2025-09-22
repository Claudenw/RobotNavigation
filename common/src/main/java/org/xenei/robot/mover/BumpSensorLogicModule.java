package org.xenei.robot.mover;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.common.Mover;
import org.xenei.robot.common.messages.Topic;
import org.xenei.robot.common.utils.RobutContext;
import org.xenei.robot.ml.SensorLayer;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;

/**
 * starts with accept sensor layer. select a corrective action. ignore sensor
 * layer wile taking corrective action. move enough to untrigger event. check
 * sensor layer if pause go to select a corrective action if run: set stop if
 * stop: reset trigger?
 */
public class BumpSensorLogicModule implements Mover.LogicModule {
	private static final Logger LOG = LoggerFactory.getLogger(BumpSensorLogicModule.class);

	private final int rangeSteps;
	private final BaseMover mover;
	private final AtomicBoolean sensorLayerEnabled;
	private Lock lock;
	private final Topic<Mover.MotorState> motorStateTopic;

	public BumpSensorLogicModule(RobutContext ctxt, BaseMover mover) {
		motorStateTopic = ctxt.bus.motor;
		sensorLayerEnabled = new AtomicBoolean(true);
		rangeSteps = ctxt.chassisInfo.steps(ctxt.scaleInfo.getResolution());
		this.mover = mover;
		this.mover.register(this);
		ctxt.bus.bump.register(this::processSensorLayer);
	}

	/**
	 * @param sensorLayer
	 *            the input argument
	 */
	private void processSensorLayer(SensorLayer sensorLayer) {
		if (sensorLayerEnabled.get() && lock.tryLock()) {
			try {
				motorStateTopic.send(Mover.MotorState.PAUSE);
				sensorLayerEnabled.set(false);
				takeCorrectiveAction(sensorLayer);
			} finally {
				lock.unlock();
				sensorLayerEnabled.set(true);
			}
		}
	}

	private void takeCorrectiveAction(SensorLayer sensorLayer) {
		switch (sensorLayer.getAnswer()) {
			case FF -> {
				mover.takeSteps(rangeSteps, rangeSteps, sensorLayer.getTrigger());
			}
			case FS -> {
				mover.takeSteps(rangeSteps, 0, sensorLayer.getTrigger());
			}
			case FR -> {
				mover.takeSteps(rangeSteps, -rangeSteps, sensorLayer.getTrigger());
			}
			case SF -> {
				mover.takeSteps(0, rangeSteps, sensorLayer.getTrigger());
			}
			case SS -> {
				mover.takeSteps(0, 0, sensorLayer.getTrigger());
			}
			case SR -> {
				mover.takeSteps(0, -rangeSteps, sensorLayer.getTrigger());
			}
			case RF -> {
				mover.takeSteps(-rangeSteps, rangeSteps, sensorLayer.getTrigger());
			}
			case RS -> {
				mover.takeSteps(-rangeSteps, 0, sensorLayer.getTrigger());
			}
			case RR -> {
				mover.takeSteps(-rangeSteps, -rangeSteps, sensorLayer.getTrigger());
			}
			case DONT_CARE -> {
				LOG.error("Invalid SensorLayer answer: DONT_CARE ");
				return;
			}
		}
		mover.sleep(rangeSteps);
		while (Mover.MotorState.RUN.equals(mover.getMotorState())) {
			mover.sleep();
		}
	}

	@Override
	public void setLock(Lock lock) {
		this.lock = lock;
	}
}
