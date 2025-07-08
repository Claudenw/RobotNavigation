package org.xenei.robot.rpi.drivers;

import java.util.concurrent.Callable;

public interface Motor extends AutoCloseable {
    /**
     * Gets the maximum RPM for the motor.
     * @return the maximum RPM for the motor.
     */
    int getMaxRpm();

    /**
     * Prepars the motor for a run.
     * @param steps the number of steps to take.
     * @param rpm the rpm to run at.
     * @return The SteppingStatus for the run.
     */
    SteppingStatus prepareRun(int steps, int rpm);

    /**
     * Gets the number of steps necessary to make one rotation of the drive shaft.
     * @return the number of steps necessary to make one rotation of the drive shaft.
     */
    double stepsPerRotation();

    /**
     * Stop a stepper motor.
     */
    void stop();

    interface SteppingStatus {

        /**
         * Gets the number of milliseconds it takes to complete a step.
         * @return the number of milliseconds it takes to complete a step..
         */
        long millisecondsPerStep();

        /**
         * Cause the motor to take a step.
         * @return {@code true} if the motor is still stepping.
         */
        boolean step();

        /**
         * Checks if all motion is complete.
         * @return {@code true} if all the steps have been taken.
         */
        boolean isComplete();
        
        /**
         * Gets the number of steps taken in a forward direction.
         * @return the number of steps taken, negative for reverse travel.
         */
        int fwdSteps();
        
        /**
         * Gets the number of rotations of the wheel.
         * @return the number of rotations.
         */
        double fwdRotation();

        /**
         * Gets the number of steps necessary to make one rotation of the drive shaft.
         * @return the number of steps necessary to make one rotation of the drive shaft.
         */
        double stepsPerRotation();
    }
}
