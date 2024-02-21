package org.xenei.robot.rpi;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import org.locationtech.jts.geom.Coordinate;
import org.xenei.robot.common.Compass;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.Mover;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.UnmodifiableCoordinate;
import org.xenei.robot.rpi.drivers.ULN2003;

public class RpiMover implements Mover {
    private ULN2003[] motor = new ULN2003[2];
    private static final int LEFT = 0;
    private static final int RIGHT = 1;
    private Position position;
    private Compass compass;
    private double rotationalDistance;
    private int rpm;
    private double r;
    /**
     * @param compass the compass implementation to use.
     * @param wheelDiameter the wheel diameter in CM.
     * @param maxSpeed meters / minute.
     */
    RpiMover(Compass compass, double width, int wheelDiameter, double maxSpeed) {
        motor[LEFT] = new ULN2003(ULN2003.Mode.FULL_STEP, ULN2003.STEPPER_28BYJ48, 17, 27, 22, 23 );
        motor[RIGHT] = new ULN2003(ULN2003.Mode.FULL_STEP, ULN2003.STEPPER_28BYJ48, 6, 24, 25, 26 );
        position = Position.from(0,0,compass.heading());
        this.compass = compass;
        this.rotationalDistance = Math.PI * wheelDiameter / 100; // in meters
        this.r = width/2.0; // in cm
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
        
        int rangeSteps = steps(location.range());
        takeSteps( rangeSteps, rangeSteps );
        return nxt;
    }
    
    private void takeSteps(int left, int right) {
        System.out.format( "Taking steps %s %s\n", left, right);
        FutureTask<?> leftFuture = motor[LEFT].run(left, rpm);
        FutureTask<?> rightFuture = motor[RIGHT].run(right, rpm);
        try {
            leftFuture.get();
            rightFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            leftFuture.cancel(true);
            rightFuture.cancel(true);
        }
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
        // theta r is the distance the wheel has to move to pass through the arc from
        // to make the direction change.
        
        double theta = heading - compass.heading();
        int thetaSteps = steps(theta*r);
        takeSteps(-thetaSteps, thetaSteps);
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
