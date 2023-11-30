package org.xenei.robot.rpi;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.xenei.robot.common.AngleUnits;
import org.xenei.robot.common.Coordinates;

import com.diozero.api.I2CDevice;

public class MMC3416xPJ {

    private static final int CONTROLLER = 1;
    private static final int ADDRESS = 0x30;

    private static byte REG_PRODUCT_ID = 0x20;
    private static byte PRODUCT_ID = 0x06;

    public static byte OK = 0x00;
    public static byte ERROR = (byte) 0xFF;

    public enum Axis {
        X, Y, Z
    }

    public enum Resolution {
        _16bits_8ms(2048f, (byte) 0), _16bits_4ms(2048f, (byte) 0), _14bits_2ms(512f, (byte) 2),
        _12bits_1ms(128f, (byte) 4);

        private float max;
        private byte flag;

        Resolution(float max, byte flag) {
            this.max = max;
            this.flag = flag;
        }

        public float getMax() {
            return max;
        }

        public float getFlag() {
            return flag;
        }

    };

    private final ReentrantLock lock;
    private final I2CDevice device;
    private Resolution resolution;
    private boolean continuous;

    public MMC3416xPJ() {
        lock = new ReentrantLock();
        device = new I2CDevice(CONTROLLER, ADDRESS);
        setResolution(Resolution._16bits_8ms);
        continuous = false;
    }

    public boolean isContinuous() {
        return continuous;
    }

    public Configuration getConfiguration() {
        return new Configuration();
    }

    public Resolution getResolution() {
        return resolution;
    }

    public Status setResolution(Resolution resolution) {
        this.resolution = resolution;
        return new InternalControl().setResolution(resolution).execute();
    }

    public Status selfTest() {
        return new InternalControl().setSelfTest().execute();
    }

    public Status swReset() {
        return new InternalControl().setSWReset().execute();
    }

    public Values getHeading() {
        lock.lock();
        try {
            new Configuration().setSet().execute();
            Values v1 = new Values();
            new Configuration().setReset().execute();
            Values v2 = new Values();
            int[] v3 = new int[3];
            v3[0] = (v1.getAxisData(Axis.X) - v2.getAxisData(Axis.X)) / 2;
            v3[1] = (v1.getAxisData(Axis.Y) - v2.getAxisData(Axis.Y)) / 2;
            v3[2] = (v1.getAxisData(Axis.Z) - v2.getAxisData(Axis.Z)) / 2;
            return new Values(v3);
        } finally {
            lock.unlock();
        }
    }

    private boolean checkMask(byte result, byte mask) {
        return (result & mask) != 0;
    }

    private byte writeThenRead(byte cmd) {
        lock.lock();
        try {
            device.writeByte(cmd);
            return device.readByte();
        } finally {
            lock.unlock();
        }
    }

    public Status getStatus() {
        return new Status();
    }

    private static void delay(TimeUnit unit, int ms) {
        try {
            unit.sleep(ms);
        } catch (InterruptedException e) {
            // log error
        }
    }

    private static void delay(int ms) {
        try {
            TimeUnit.MILLISECONDS.sleep(ms);
        } catch (InterruptedException e) {
            // log error
        }
    }

    public byte getProductId() {
        byte result = 0;
        if (getStatus().readDone()) {
            result = writeThenRead(REG_PRODUCT_ID);

            if (result != PRODUCT_ID) {
                result = ERROR;
            }
        }
        delay(10);
        return result;
    }

    public Values getData() {
        return new Values();
    }

    public void reset() {
        new Configuration().setReset().execute();
    }

    @Override
    public String toString() {
        return String.format("MMC3146xPJ[ continuous:%s product:%s resolution:%s]", isContinuous(), getProductId(),
                getResolution());
    }

    public class Status {
        private static byte REG_STATUS = 0x06;
        /**
         * Check Status
         */
        private static byte MEASUREMENT_DONE = 0x01;
        private static byte PUMP_ON = 0x02;
        private static byte READ_DONE = 0x04;
        private static byte SELFTEST_OK = 0x08;

        private byte status;

        Status() {
            status = writeThenRead(REG_STATUS);
        }

        public void refresh() {
            status = writeThenRead(REG_STATUS);
        }

        public boolean measurementDone() {
            return checkMask(status, MEASUREMENT_DONE);
        }

        public boolean pumpOn() {
            return checkMask(status, PUMP_ON);
        }

        public boolean readDone() {
            return checkMask(status, READ_DONE);
        }

        public boolean selfTestOk() {
            return checkMask(status, SELFTEST_OK);
        }

        @Override
        public String toString() {
            return String.format("Status[ measure:%s read:%s pump-on:%s self-test:%s]", measurementDone(), readDone(),
                    pumpOn(), selfTestOk());
        }
    }

    enum Frequency {
        HZ1_5, HZ13, HZ25, HZ50
    }

    public class Configuration {
        private static byte INTERNAL_CONTROL_0 = 0x07;

        private static byte RESET = 0x40;
        private static byte SET = 0x20;
        private static byte REFILL_CAP = (byte) 0x80;
        private static byte NO_BOOST = 0x10;
        private static byte CONTINUOUS_MODE = 0x02;
        private static byte TAKE_MEASUREMENT = 0x01;

        private byte value = 0;

        public Configuration setCapRefill() {
            value |= REFILL_CAP;
            return this;
        }

        public Configuration setReset() {
            value |= RESET;
            return this;
        }

        public Configuration setSet() {
            value |= SET;
            return this;
        }

        public Configuration setDisableBoost() {
            value |= NO_BOOST;
            return this;
        }

        public Configuration setFrequency(Frequency freq) {
            value |= (byte) (freq.ordinal() << 2);
            return this;
        }

        public Configuration setContinuousMode() {
            value |= CONTINUOUS_MODE;
            return this;
        }

        public Configuration setTakeMeasurement() {
            value |= TAKE_MEASUREMENT;
            return this;
        }

        Status execute() {
            lock.lock();
            try {
                device.writeByteData(INTERNAL_CONTROL_0, value);
                continuous = checkMask(value, CONTINUOUS_MODE);
                if (!checkMask(value, RESET)) {
                    delay(100);
                }
                return new Status();
            } finally {
                lock.unlock();
            }
        }
    }

    private class InternalControl {
        private static byte INTERNAL_CONTROL_1 = 0x08;
        private static byte SOFT_RESET = (byte) 0x80;
        private static byte SELFTEST = 0x20;

        private byte value = 0;

        InternalControl setSWReset() {
            value |= SOFT_RESET;
            return this;
        }

        InternalControl setSelfTest() {
            value |= SELFTEST;
            return this;
        }

        InternalControl setResolution(Resolution resolution) {
            value |= resolution.flag;
            return this;
        }

        Status execute() {
            device.writeByteData(INTERNAL_CONTROL_1, value);
            delay(100);
            return new Status();
        }
    }

    private void dumpBuffer(byte[] buff) {
        for (int i = 0; i < buff.length; i++) {
            System.out.format(" 0x%X", buff[i]);
        }
        System.out.println();
    }

    public class Values {
        private int[] data = new int[3];

        private Values(int[] v) {
            for (int i = 0; i < 3; i++) {
                data[i] = v[i];
            }
        }

        private Values() {
            byte[] buffer = new byte[6];
            lock.lock();
            try {
                Status status = new Configuration().setCapRefill().setTakeMeasurement().execute();

                while (!status.measurementDone() && !status.readDone()) {
                    System.out.println("Waiting  for measurement");
                    delay(100);
                    status.refresh();
                }

                device.writeByte((byte) 0x00);
                device.readBytes(buffer);
                // dumpBuffer(buffer);

                data[0] = 0xFFFF & ((buffer[1] << 8) | (buffer[0] & 0xFF)) >> resolution.flag;
                data[1] = 0xFFFF & ((buffer[3] << 8) | (buffer[2] & 0xFF)) >> resolution.flag;
                data[2] = 0xFFFF & ((buffer[5] << 8) | (buffer[4] & 0xFF)) >> resolution.flag;
                // System.out.format("0x%x 0x%x 0x%x\n", data[0], data[1], data[2]);
            } finally {
                lock.unlock();
            }
        }

        public IntBuffer getData() {
            return IntBuffer.wrap(data).asReadOnlyBuffer();
        }

        public FloatBuffer getValues() {
            FloatBuffer fb = FloatBuffer.allocate(3);
            for (int i = 0; i < 3; i++) {
                fb.put(i, data[i] / resolution.max);
            }
            return fb;
        }

        public float getAxisValue(Axis axis) {
            return data[axis.ordinal()] / resolution.max;
        }

        public int getAxisData(Axis axis) {
            return data[axis.ordinal()];
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("Values[ ");
            for (Axis axis : Axis.values()) {
                sb.append(String.format("%s:{%s %.5f} ", axis, getAxisData(axis), getAxisValue(axis)));
            }
            return sb.append("]").toString();
        }
    }

    public static void main(String[] args) {
        MMC3416xPJ mag = new MMC3416xPJ();
        mag.reset();
        Status status = mag.getStatus();
        System.out.println(mag);
        System.out.println(status);

        delay(TimeUnit.SECONDS, 1);

        while (true) {
            Values values = mag.getHeading();
            System.out.println(values);
            Coordinates c = Coordinates.fromXY(values.getAxisValue(Axis.X), values.getAxisValue(Axis.Y));
            Coordinates d = Coordinates.fromXY(values.getAxisData(Axis.X), values.getAxisData(Axis.Y));
            System.out.format("Heading: value: %s  data: %s\n", c.getTheta(AngleUnits.DEGREES),
                    d.getTheta(AngleUnits.DEGREES));
            delay(TimeUnit.MILLISECONDS, 250);
        }
    }
}
