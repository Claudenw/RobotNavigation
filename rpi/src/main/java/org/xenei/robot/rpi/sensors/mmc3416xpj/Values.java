package org.xenei.robot.rpi.sensors.mmc3416xpj;

import org.xenei.robot.common.utils.TimingUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 * The X, Y, and Z values read from the sensor.
 */
public final class Values {

    private final int[] data = new int[3];
    private final float sensitivity;

    Values(ShortBuffer buffer, Resolution resolution) {
        for (int i=0; i<data.length; i++) {
            data[i] = buffer.get(i);
        }
        sensitivity = resolution != null ? resolution.sensitivity : 1;
    }

    public FloatBuffer getGauss() {
        FloatBuffer fb = FloatBuffer.allocate(Axis.values().length);
        for (int i = 0; i < fb.capacity(); i++) {
            fb.put(i, data[i] / sensitivity);
        }
        return fb;
    }

    public float getAxisGauss(Axis axis) {
        return data[axis.ordinal()] / sensitivity;
    }

    public int getAxisData(Axis axis) {
        return data[axis.ordinal()];
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Values[ ");
        for (Axis axis : Axis.values()) {
            sb.append(String.format("%s:{%s field =  %.5f gauss} ", axis, getAxisData(axis), getAxisGauss(axis)));
        }
        return sb.append("]").toString();
    }
}
