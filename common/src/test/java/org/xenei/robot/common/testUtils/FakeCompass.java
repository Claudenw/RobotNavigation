package org.xenei.robot.common.testUtils;

import org.xenei.robot.common.Compass;
import org.xenei.robot.common.utils.DoubleUtils;

import java.util.function.Supplier;

public class FakeCompass implements Compass {

	private Supplier<Double> headingSupplier;

	public void setHeading(Supplier<Double> headingSupplier) {
		this.headingSupplier = headingSupplier;
	}

	@Override
	public double heading() {
		return headingSupplier.get();
	}

	@Override
	public double instantaneousHeading() {
		return headingSupplier.get();
	}

	@Override
	public double sd() {
		return 0;
	}

	@Override
	public int decimalPlaces() {
		return 2;
	}

	@Override
	public String toString() {
		double h = heading();
		double sd = sd();
		return String.format("FakeCompass[Heading: %s %s degrees]", h,
				DoubleUtils.round(Math.toDegrees(h), decimalPlaces() + 1), sd);
	}
}
