package org.xenei.robot.common;

public interface StepMonitor {

    default boolean hasStepDifferential() {
        return leftSteps() != rightSteps();
    }

    double leftRotation();

    double rightRotation();

    int leftSteps();

    int rightSteps();
}
