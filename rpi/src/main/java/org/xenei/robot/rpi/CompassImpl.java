package org.xenei.robot.rpi;

import org.xenei.robot.common.AngleUnits;
import org.xenei.robot.common.Compass;
import org.xenei.robot.common.Coordinates;
import org.xenei.robot.rpi.sensors.MMC3416xPJ;
import org.xenei.robot.rpi.sensors.MMC3416xPJ.Axis;

public class CompassImpl implements Compass {
    private MMC3416xPJ compass = new MMC3416xPJ();
    
    @Override
    public double heading() {
        MMC3416xPJ.Values values = compass.getHeading();
        Coordinates heading = Coordinates.fromXY(values.getAxisValue(Axis.X), values.getAxisValue(Axis.Y));
        return heading.getTheta(AngleUnits.RADIANS);
    }

}
