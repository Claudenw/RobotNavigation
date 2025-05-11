package org.xenei.robot.rpi.sensors;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.xenei.robot.common.Location;
import org.xenei.robot.common.utils.TimingUtils;

import com.diozero.api.I2CDevice;

public class MMC3416xPJ {
    private static final int CONTROLLER = 1;
    private static final int ADDRESS = 0x30;

    private static final byte REG_PRODUCT_ID = 0x20;
    private static final byte PRODUCT_ID = 0x06;

    public static byte OK = 0x00;
    public static byte ERROR = (byte) 0xFF;

    private final Values zeroOffset = new Values(new int[] {0, 0, 0,});

    public enum Axis {
        X, Y, Z;
    }

    /**
     * The resolution of the measurements.
     */
    public enum Resolution {
        _16bits_8ms(2048f, (byte) 0), _16bits_4ms(2048f, (byte) 1), _14bits_2ms(512f, (byte) 2),
        _12bits_1ms(128f, (byte) 4);

        /** THe maximum value of the measurement */
        private final float max;
        /** The register value for the device call */
        private final byte flag;

        Resolution(float max, byte flag) {
            this.max = max;
            this.flag = flag;
        }
    }

    private final ReentrantLock lock;
    private final I2CDevice device;
    private Resolution resolution;
    private boolean continuous;
    private Values offsets;

    public MMC3416xPJ() {
        lock = new ReentrantLock();
        device = new I2CDevice(CONTROLLER, ADDRESS);
        setResolution(Resolution._16bits_8ms);
        continuous = false;
        calcOffsets();
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

    public void calcOffsets() {
        int[] v3 = new int[3];
        Arrays.fill(v3, 0);
        new Configuration().setSet().execute();
        Values v1 = new Values(zeroOffset);
        new Configuration().setReset().execute();
        Values v2 = new Values(zeroOffset);
        for (Axis axis : Axis.values()) {
            v3[axis.ordinal()] = (v1.getAxisData(axis) + v2.getAxisData(axis)) / 2;
        }
        offsets = new Values(v3);
    }

    /**
     * Calculates the Values for the difference between the
     * @return
     */
    public Values getHeading() {
        lock.lock();
        try {
            return new Values(offsets);
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

    public byte getProductId() {
        byte result = 0;
        if (getStatus().readDone()) {
            result = writeThenRead(REG_PRODUCT_ID);

            if (result != PRODUCT_ID) {
                result = ERROR;
            }
        }
        TimingUtils.delay(10);
        return result;
    }

    public Values getData() {
        return new Values(zeroOffset);
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
        private static final byte REG_STATUS = 0x06;
        /**
         * Check Status
         */
        private static final byte MEASUREMENT_DONE = 0x01;
        private static final byte PUMP_ON = 0x02;
        private static final byte READ_DONE = 0x04;
        private static final byte SELFTEST_OK = 0x08;

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
        private static final byte INTERNAL_CONTROL_0 = 0x07;

        private static final byte RESET = 0x40;
        private static final byte SET = 0x20;
        private static final byte REFILL_CAP = (byte) 0x80;
        private static final byte NO_BOOST = 0x10;
        private static final byte CONTINUOUS_MODE = 0x02;
        private static final byte TAKE_MEASUREMENT = 0x01;

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
                    TimingUtils.delay(100);
                }
                return new Status();
            } finally {
                lock.unlock();
            }
        }
    }

    private class InternalControl {
        private static final byte INTERNAL_CONTROL_1 = 0x08;
        private static final byte SOFT_RESET = (byte) 0x80;
        private static final byte SELFTEST = 0x20;

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
            TimingUtils.delay(100);
            return new Status();
        }
    }

    /**
     * The X, Y, and Z values read from the sensor.
     */
    public class Values {
        private final int[] data = new int[3];

        private Values(int[] v) {
            System.arraycopy(v, 0, data, 0, 3);
        }

        private Values(Values offsets) {
            byte[] buffer = new byte[6];
            lock.lock();
            try {
                // take measurements
                Status status = new Configuration().setCapRefill().setTakeMeasurement().execute();
                while (!status.measurementDone() && !status.readDone()) {
                    System.out.println("Waiting  for measurement");
                    TimingUtils.delay(100);
                    status.refresh();
                }

                // read the measurements
                device.writeByte(OK);
                device.readBytes(buffer);

                // save the data
                ByteBuffer bb = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN);
                IntBuffer ib = bb.asIntBuffer();
                for (Axis axis : Axis.values()) {
                    data[axis.ordinal()] = ib.get(axis.ordinal()) - offsets.getAxisData(axis);
                }
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

        TimingUtils.delay(TimeUnit.SECONDS, 1);

        while (true) {
            Values values = mag.getHeading();
            System.out.println(values);
            Location c = Location.from(values.getAxisValue(Axis.X), values.getAxisValue(Axis.Y));
            System.out.format("Heading: value: %s  data: %s\n", Math.toDegrees(c.theta()), values);
            TimingUtils.delay(TimeUnit.MILLISECONDS, 250);
        }
    }
}
