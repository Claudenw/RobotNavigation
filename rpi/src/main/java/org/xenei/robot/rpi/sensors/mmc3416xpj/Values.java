package org.xenei.robot.rpi.sensors.mmc3416xpj;

import org.xenei.robot.common.utils.AngleUtils;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 * The X, Y, and Z values read from the sensor.
 */
public final class Values {

    private final short[] data = new short[3];
    private final float sensitivity;

    Values(ShortBuffer buffer, Resolution resolution) {
        for (int i=0; i<data.length; i++) {
            data[i] = buffer.get(i);
        }
        sensitivity = resolution != null ? resolution.counts_per_gauss : 1;
    }

    public FloatBuffer getGauss() {
        FloatBuffer fb = FloatBuffer.allocate(Axis.values().length);
        for (int i = 0; i < Axis.values().length; i++) {
            fb.put(i, data[i] / sensitivity);
        }
        return fb;
    }

    public ShortBuffer getData() {
        ShortBuffer sb = ShortBuffer.allocate(Axis.values().length);
        for (int i = 0; i < Axis.values().length; i++) {
            sb.put(i, data[i]);
        }
        return sb;
    }

    public float getGauss(Axis axis) {
        return data[axis.ordinal()] / sensitivity;
    }

    public int getData(Axis axis) {
        return data[axis.ordinal()];
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Values[ ");
        for (Axis axis : Axis.values()) {
            sb.append(String.format("%s:{%s field =  %.5f gauss} ", axis, getData(axis), getGauss(axis)));
        }
        return sb.append("]").toString();
    }

    public double radians() {
        float temp0 = 0;
        float temp1 = 0;
        double radians = 0;
        final double firstHalf = AngleUtils.RADIANS_90;
        final double secondHalf = AngleUtils.RADIANS_90 * 3;

//        for (int i=0; i<3; i++) {
//            data[i] = 0.48828125 * (float)raw[i] - offset[0];
//        }
        if (getGauss(Axis.X) < 0) {
            if (getGauss(Axis.Y) > 0) {
                //Quadrant 1
                temp0 = getGauss(Axis.Y);
                temp1 = -getGauss(Axis.X);
                radians = firstHalf - Math.atan(temp0 / temp1);
            } else {
                //Quadrant 2
                temp0 = -getGauss(Axis.Y);
                temp1 = -getGauss(Axis.X);
                radians = firstHalf + Math.atan(temp0 / temp1);
            }
        } else {
            if (getGauss(Axis.Y) < 0) {
                //Quadrant 3
                temp0 = -getGauss(Axis.Y);
                temp1 = getGauss(Axis.X);
                radians = secondHalf - Math.atan(temp0 / temp1);
            } else {
                //Quadrant 4
                temp0 = getGauss(Axis.Y);
                temp1 = getGauss(Axis.X);
                radians = secondHalf + Math.atan(temp0 / temp1);
            }
        }
        return radians;
    }

    public double degrees() {
        return Math.toDegrees(radians());
    }
}
