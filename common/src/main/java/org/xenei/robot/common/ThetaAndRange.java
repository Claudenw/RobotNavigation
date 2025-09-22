package org.xenei.robot.common;

import org.xenei.robot.common.utils.CoordUtils;

public record ThetaAndRange(double theta, double range) implements Location {
	public UnmodifiableCoordinate getCoordinate() {
		return UnmodifiableCoordinate.make(CoordUtils.fromAngle(theta, range));
	}
}
