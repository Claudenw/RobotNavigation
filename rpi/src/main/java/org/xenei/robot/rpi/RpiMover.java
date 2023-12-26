package org.xenei.robot.rpi;

import org.locationtech.jts.geom.Coordinate;
import org.xenei.robot.common.Compass;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.Mover;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.Scale;
import org.xenei.robot.common.UnmodifiableCoordinate;
import org.xenei.robot.common.utils.CoordUtils;
import org.xenei.robot.common.utils.DoubleUtils;
import org.xenei.robot.rpi.drivers.ULN2003;

public class RpiMover implements Mover {
    private ULN2003[] motor = new ULN2003[2];
    private static final int LEFT = 0;
    private static final int RIGHT = 1;
    private Position position;
    private Compass compass;
    private double rotationalDistance;
    private int rpm;
    /**
     * @param compass the compass implementation to use.
     * @param wheelDiameter the wheel diameter in CM.
     * @param maxSpeed meters / minute.
     */
    RpiMover(Compass compass, double wheelDiameter, int maxSpeed) {
        motor[LEFT] = new ULN2003(ULN2003.Mode.FULL_STEP, ULN2003.STEPPER_28BYJ48, 12, 13, 14, 15 );
        motor[RIGHT] = new ULN2003(ULN2003.Mode.FULL_STEP, ULN2003.STEPPER_28BYJ48, 12, 13, 14, 15 );
        position = Position.from(0,0,compass.heading());
        this.compass = compass;
        this.rotationalDistance = Math.PI * wheelDiameter / 100;
        // meterminute / meterrotation = meterrotation/meter/minute = r/m
        this.rpm =limit((long) Math.ceil( maxSpeed / rotationalDistance), 1, 150);
    }


    private int limit(long value, int min, int max) {
        return (value < min) ? min : (value > max) ? max : (int)value;
    }
    
    @Override
    public Position move(Location location) {
        Position nxt = position.nextPosition(location);
        if (nxt.getHeading() != position.getHeading())
        {
            setHeading(nxt.getHeading());
        }
        
        int steps = steps(location.range());
        motor[LEFT].run(steps, rpm);
        motor[RIGHT].run(steps, rpm);
        return nxt;
    }
    
    private int steps(double range) {
        double rotations = range / rotationalDistance;
        return limit(motor[LEFT].steps(rotations), Short.MIN_VALUE, Short.MAX_VALUE);
    }

    @Override
    public Position position() {
        return position();
    }

    @Override
    public void setHeading(double heading) {
        double theta = (heading - compass.heading())/2;
        int steps = steps(theta);
        if (steps != 0) {
            motor[LEFT].run(-steps, rpm);
            motor[RIGHT].run(steps, rpm);
        }
    }

    class DynamicPosition implements Position {        
        private Location startingLocation;
        
        DynamicPosition(Location location) {
            this.startingLocation = location;
        }

        @Override
        public double getHeading() {
            return compass.heading();
        }

        @Override
        public UnmodifiableCoordinate getCoordinate() {
            double heading = getHeading();
            double distance = motor[RIGHT].rotations()*rotationalDistance;
            
            return UnmodifiableCoordinate.make(new Coordinate(
                    startingLocation.getX() + Math.sin(heading) * distance,
                    startingLocation.getY() + Math.cos(heading) * distance));
        }
    }
}
