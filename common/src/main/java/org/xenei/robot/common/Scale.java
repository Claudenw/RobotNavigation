package org.xenei.robot.common;

public enum Scale {
    KILOMETER(1.0 / 1000), METER(1), CENTIMETER(100), MILIMETER(1000);

    double ratio;

    Scale(double ratio) {
        this.ratio = ratio;
    }

    public double convert(double count, Scale scale) {
        return count / ratio * scale.ratio;
    }
}
