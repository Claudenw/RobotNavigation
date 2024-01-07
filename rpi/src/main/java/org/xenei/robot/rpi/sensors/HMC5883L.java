package org.xenei.robot.rpi.sensors;

import org.xenei.robot.common.utils.AngleUtils;

import com.diozero.api.I2CDevice;

public class HMC5883L {

    enum Mode {
        CONTINUOUS((byte) 0x0), SINGLE((byte) 0x1), IDLE((byte) 0x3);

        byte state;

        Mode(byte state) {
            this.state = state;
        }

        byte getState() {
            return state;
        }
    }

    private enum Register {
        A((byte) 0x0), B((byte) 0x1), MODE((byte) 0x2), DATA((byte) 0x3);

        byte reg;

        Register(byte reg) {
            this.reg = reg;
        }

        byte getReg() {
            return reg;
        }
    }

    private static float gauss[] = { 0.88f, 1.3f, 1.9f, 2.5f, 4.0f, 4.7f, 5.6f, 8.1f };
    private static final int CONTROLLER = 1;
    private static final int ADDRESS = 0x1E;
    private byte gain;
    private final I2CDevice device = new I2CDevice(CONTROLLER, ADDRESS);
    private float declination;

    public class Scaled {
        float XAxis;
        float YAxis;
        float ZAxis;

        Scaled() {
            Raw raw = new Raw();
            XAxis = raw.XAxis * gauss[gain];
            ZAxis = raw.ZAxis * gauss[gain];
            YAxis = raw.YAxis * gauss[gain];
        }
    };

    public class Raw {
        short XAxis;
        short YAxis;
        short ZAxis;

        Raw() {
            byte[] buffer = new byte[6];
            int read = device.readI2CBlockData(Register.DATA.reg, buffer);
            XAxis = (short) ((buffer[0] << 8) | buffer[1]);
            ZAxis = (short) ((buffer[2] << 8) | buffer[3]);
            YAxis = (short) ((buffer[4] << 8) | buffer[5]);
        }
    };

    public HMC5883L() {
        gain = 1;
        setMeasurementMode(Mode.CONTINUOUS);
    }

    public void setGain(byte gain) {
        this.gain = (byte) (gain & 0x7);
    }

    public byte getGain() {
        return gain;
    }

    public void setMeasurementMode(Mode mode) {
        device.writeByteData(Register.MODE.reg, mode.state);
    }

    /**
     * Set the declination in radians
     * 
     * @param value
     */
    public void setDeclination(float value) {
        declination = value;
    }

    /**
     * Gets the headings in radians.
     * 
     * @return
     */
    public double getHeading() {
        Scaled scaled = new Scaled();
        double heading = Math.atan2(scaled.YAxis, scaled.XAxis);
        heading = AngleUtils.normalize(heading + declination);
        return heading;

    }

}