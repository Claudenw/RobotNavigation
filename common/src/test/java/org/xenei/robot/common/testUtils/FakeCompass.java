package org.xenei.robot.common.testUtils;

import org.locationtech.jts.geom.Coordinate;
import org.xenei.robot.common.Compass;
import org.xenei.robot.common.Position;
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
    public int headingAccuracy() {
        return 2;
    }

    @Override
    public String toString() {
        double h = heading();
        return String.format("FakeCompass[Heading: %s %s degrees]", h,
                DoubleUtils.round(Math.toDegrees(h), headingAccuracy() + 1));
    }
}
