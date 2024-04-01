package org.xenei.robot.rpi.drivers;

import java.util.concurrent.Callable;

public interface Motor extends AutoCloseable {
    public boolean active();
    public SteppingStatus prepareRun(int steps, int rpm);
    public double stepsPerRotation();
    

    /**
     * Stop a stepper motor.
     */
    public void stop();

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
