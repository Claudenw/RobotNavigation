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
        
       
        public boolean isRunning();
        
        /**
         * Gets the number of steps taken in a forward direction.
         * @return the number of steps taken, negative for reverse travel.
         */
        public int fwdSteps();
        
        public double fwdRotation();
    }
}
