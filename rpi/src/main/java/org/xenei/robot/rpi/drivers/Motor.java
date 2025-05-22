package org.xenei.robot.rpi.drivers;

import java.util.concurrent.Callable;

public interface Motor extends AutoCloseable {
    boolean active();
    SteppingStatus prepareRun(int steps, int rpm);
    double stepsPerRotation();
    

    /**
     * Stop a stepper motor.
     */
    void stop();

    interface SteppingStatus extends Callable<SteppingStatus> {
        
        /**
         * Returns true if the motor is running.
         * @return
         */
        public boolean isRunning();
        
        /**
         * Gets the number of steps taken in a forward direction.
         * @return the number of steps taken, negative for reverse travel.
         */
        public int fwdSteps();
        
        /**
         * Gets the number of rotations of the wheel..
         * @return the number of rotations.
         */
        public double fwdRotation();
    }
}
