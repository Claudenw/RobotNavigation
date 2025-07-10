package org.xenei.robot.common;

public interface StepMonitor {

    boolean hasStepDifferential();

    double leftRotation();

    double rightRotation();

    int leftSteps();

    int rightSteps();
}
