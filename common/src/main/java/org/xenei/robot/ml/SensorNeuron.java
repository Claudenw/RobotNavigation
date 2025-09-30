package org.xenei.robot.ml;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class SensorNeuron {

    public static final BitMap DONT_CARE = new BitMap();

    private BitMap options = new BitMap();
    private boolean lastState = false;
    private final byte mask;

    public SensorNeuron(int idx) {
        this.mask = (byte) BitMap.getMask(idx);
    }

    public BitMap trigger(byte triggerMap) {
        lastState = (triggerMap & mask) != 0;
        return lastState ? options : DONT_CARE;
    }

    public void feedback(int lastSelection, byte triggerMap) {
        // disable the last selection if we were triggered and continue to be triggered.
        if (lastState && (triggerMap & mask) != 0) {
            this.options = options.disable(lastSelection);
        }
    }

    public BitMap getModel() {
        return options;
    }

    public void reset(int lastSelection) {
        this.options = options.enable(lastSelection);
    }

    public String toString() {
        return options.toString();
    }

    public void write(DataOutputStream out) throws IOException {
        options.write(out);
    }

    public void read(DataInputStream in) throws IOException {
        options.read(in);
    }
}
