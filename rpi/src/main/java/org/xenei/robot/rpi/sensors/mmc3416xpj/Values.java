package org.xenei.robot.rpi.sensors.mmc3416xpj;

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
        sensitivity = resolution != null ? resolution.sensitivity : 1;
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

    public double degrees() {
        return Math.atan(getGauss(Axis.X)/ getGauss(Axis.Y)) * (180/Math.PI);
    }
}
