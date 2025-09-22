package org.xenei.robot.common;

import org.xenei.robot.common.utils.AngleUtils;

public class MotorInfo {
	private final double stepAngle;
	private final int freq;
	private final double stepsPerRotation;

	public MotorInfo(double stepAngle, int freq) {
		this.stepAngle = stepAngle;
		this.freq = freq;
		this.stepsPerRotation = AngleUtils.PI_x_2 / stepAngle;
	}

	public int stepsPerMinute() {
		return freq * 60;
	}

	public double stepsPerRotation() {
		return stepsPerRotation;
	}

	public double stepAngle() {
		return stepAngle;
	}

	public int freq() {
		return freq;
	}
}
