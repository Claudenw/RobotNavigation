package org.xenei.robot.rpi.sensors.mmc3416xpj;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

import org.xenei.robot.common.utils.TimingUtils;
import com.diozero.api.I2CDevice;

public final class MMC3416xPJ {
    private static final int CONTROLLER = 1;
    private static final int ADDRESS = 0x30;

    private static final byte REG_PRODUCT_ID = 0x20;
    private static final byte PRODUCT_ID = 0x06;
    private static final byte FIRST_DATA_REGISTER = 0x00;

    public static byte ERROR = (byte) 0xFF;

    private static final byte INTERNAL_CONTROL_0 = 0x07;
    private static final byte INTERNAL_CONTROL_1 = 0x08;

    private static final byte EMPTY_BYTE = (byte) 0;

    // values for internal control 0
    private static final byte REFILL_CAP =  (byte) 0x80;
    private static final byte RESET = (byte) 0x40;
    private static final byte SET = (byte) 0x20;
    private static final byte NO_BOOST = (byte) 0x10;
    private static final byte CONTINUOUS_MODE = (byte) 0x02;
    private static final byte TAKE_MEASUREMENT = (byte) 0x01;

    // values for internal control 1
    private static final byte SW_RESET =  (byte) 0x80;
    private static final byte SELF_TEST_BYTE = (byte) 0x20;

    private static final byte FREQ_MASK = (byte) 0x0C;

    private final ReentrantLock lock;
    private final I2CDevice device;
    private final Configurator configurator;

    public MMC3416xPJ() {
        lock = new ReentrantLock();
        device = new I2CDevice(CONTROLLER, ADDRESS);
        this.configurator = new Configurator();
        if (!selfTest()) {
            throw new IllegalStateException("Self test failed");
        }
        set();
        reset();
    }

    void lock() {
        lock.lock();
    }

    void unlock() {
        lock.unlock();
    }

    Status status() {
        return new Status(writeThenRead(Status.REG_STATUS));
    }

    public Frequency getFrequency() {
        lock();
        try {
            byte value = (byte) (device.readByteData(INTERNAL_CONTROL_0) & FREQ_MASK);
            for (Frequency f : Frequency.values()) {
                if (f.value == value) {
                    return f;
                }
            }
            throw new IllegalStateException(String.format("Invalid frequency: %x", value));
        } finally {
            unlock();
        }
    }

    public boolean isContinuous() {
        lock();
        try {
            return checkMask(device.readByteData(INTERNAL_CONTROL_0), CONTINUOUS_MODE);
        } finally {
            unlock();
        }
    }
    /**
     * Will reset the sensor by passing a large current through Set/Reset Coil in
     * a reversed direction
     * @return this
     */
    public void reset() {
        lock();
        try {
            device.writeByteData(INTERNAL_CONTROL_0, REFILL_CAP);
            while (status().pumpOn()) {
                TimingUtils.delay(100);
            }
            device.writeByteData(INTERNAL_CONTROL_0, RESET);
        } finally {
            unlock();
        }
    }

    /**
     * Will set the sensor by passing a large current through Set/Reset Coil
     * @return this
     */
    public void set() {
        lock();
        try {
            device.writeByteData(INTERNAL_CONTROL_0, REFILL_CAP);
            while (status().pumpOn()) {
                TimingUtils.delay(100);
            }
            device.writeByteData(INTERNAL_CONTROL_0, SET);
        } finally {
            unlock();
        }
    }

    /**
     * Will disable the charge pump and cause the storage capacitor to
     * be charged off VDD.
     * @return this
     */
    public void enableBoost(boolean state) {
        byte value;
        lock();
        try {
            if (state) {
                // turn of NO_BOOST flag
                value = (byte) (device.readByteData(INTERNAL_CONTROL_0) & (CONTINUOUS_MODE | FREQ_MASK));
            } else {
                value = NO_BOOST;
            }
            device.writeByteData(INTERNAL_CONTROL_0, value);
            while (!status().readDone()) {
                TimingUtils.delay(100);
            }
        } finally {
            unlock();
        }
    }

    /**
     * Will disable the charge pump and cause the storage capacitor to
     * be charged off VDD.
     * @return this
     */
    public boolean isBoostEnabled() {
        lock();
        try {
            return !checkMask(device.readByteData(INTERNAL_CONTROL_0), NO_BOOST);
        } finally {
            unlock();
        }
    }


    /**
     * Determines how often the chip will take measurements in Continuous
     * Measurement Mode.  If freq is {@code null} continuous measurement is disabled.
     * @param freq the frequency to use.
     * @return this.
     */
    public void setContinuousMode(Frequency freq) {
        byte value = freq == null ? EMPTY_BYTE : (byte) (freq.value & CONTINUOUS_MODE);
        lock();
        try {
            value &= (byte) (device.readByteData(INTERNAL_CONTROL_0) & NO_BOOST);
            device.writeByteData(INTERNAL_CONTROL_0, value);
        } finally {
            unlock();
        }
    }

    public Values takeMeasurement() {
        lock();
        try {
            byte[] buffer = new byte[6];
            device.writeByteData(INTERNAL_CONTROL_0, TAKE_MEASUREMENT);
            while (!status().measurementDone()) {
                TimingUtils.delay(100);
            }

            // read the measurements
            device.writeByte(FIRST_DATA_REGISTER);
            int bytesRead = device.readBytes(buffer);
            if (bytesRead != 6) {
                throw new IllegalStateException(String.format("Read %s bytes, 6 expected", bytesRead));
            }
            // save the data
            ShortBuffer shortBuffer = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
            return new Values(shortBuffer, getResolution());
        } finally {
            unlock();
        }
    }

    public void softwareReset() {
        lock();
        try {
            device.writeByteData(INTERNAL_CONTROL_1, SW_RESET);
        } finally {
            unlock();
        }
    }

    public boolean selfTest() {
        lock();
        try {
            device.writeByteData(INTERNAL_CONTROL_1, SELF_TEST_BYTE);
            while (!status().readDone()) {
                TimingUtils.delay(100);
            }
            device.writeByteData(INTERNAL_CONTROL_0, TAKE_MEASUREMENT);
            while (!status().measurementDone()) {
                TimingUtils.delay(100);
            }
            return checkMask(device.readByteData(INTERNAL_CONTROL_1), SELF_TEST_BYTE);
        } finally {
            unlock();
        }
    }

    public Resolution setResolution(Resolution resolution) {
        byte value = EMPTY_BYTE;
        if (resolution != null) {
            value |= resolution.flag;
        }
        lock();
        try {
            device.writeByteData(INTERNAL_CONTROL_1, value);
            while (!status().readDone()) {
                TimingUtils.delay(100);
            }
            return getResolution();
        } finally {
            unlock();
        }
    }

    public Resolution getResolution() {
        lock();
        try {
            byte value = (byte) (device.readByteData(INTERNAL_CONTROL_1) & 0x03);
            for (Resolution resolution : Resolution.values()) {
                if (resolution.flag == value) {
                    return resolution;
                }
            }
            throw new IllegalStateException(String.format("Invalid resolution: %X", value));
        } finally {
            unlock();
        }
    }

    /**
     * Calculates the Values for the difference between the
     * @return
     */
    public Values getHeading() {
        return takeMeasurement();
    }

    private static boolean checkMask(short result, short mask) {
        return (result & mask) == mask;
    }

    byte writeThenRead(byte cmd) {
        lock.lock();
        try {
            device.writeByte(cmd);
            return device.readByte();
        } finally {
            lock.unlock();
        }
    }

    public byte getProductId() {
        byte result = 0;
        result = writeThenRead(REG_PRODUCT_ID);
        if (result != PRODUCT_ID) {
            result = ERROR;
        }
        return result;
    }

    @Override
    public String toString() {
        return String.format("MMC3146xPJ[ continuous:%s product:%s resolution:%s]", isContinuous(), getProductId(),
                getResolution());
    }

    static final class Status {
        private static final byte REG_STATUS = 0x06;
        /**
         * Check Status
         */
        private static final byte MEASUREMENT_DONE = 0x01;
        private static final byte PUMP_ON = 0x02;
        private static final byte READ_DONE = 0x04;
        private static final byte SELFTEST_OK = 0x08;

        private final byte status;

        private Status(byte status) {
            this.status = status;
        }

        /**
         * Indicates measurement event is completed. This bit should be checked before
         * reading the output
         * @return {@code true if the measurement has been read.}
         */
        public boolean measurementDone() {
            return checkMask(status, MEASUREMENT_DONE);
        }

        /**
         * Indicates the charge pump status, after Refill Cap command, the charge pump will
         * start running, and this bit will stays high, it will be reset low after the cap reaches its
         * target voltage and the charge pump is shut off.
         * @return {@code true} if the pump is on.
         */
        public boolean pumpOn() {
            return checkMask(status, PUMP_ON);
        }

        /**
         * Indicates the chip was able to successfully read its memory.
         * @return {@code true} if the memory was read.
         */
        public boolean readDone() {
            return checkMask(status, READ_DONE);
        }

        /**
         * Indicate selftest OK once this bit is
         * @return {@code true} if the self test is ok.
         */
        public boolean selfTestOk() {
            return checkMask(status, SELFTEST_OK);
        }

        @Override
        public String toString() {
            return String.format("Status[measurement:%s pumpOn:%s readDone:%s selfTest:%s]", this.measurementDone(), this.pumpOn(),
                    this.readDone(), this.selfTestOk());
        }
    }

    public class Configurator {
    }

//    public static void main(String[] args) {
//        MMC3416xPJ mag = new MMC3416xPJ();
//        System.out.println(mag);
//
//        TimingUtils.delay(TimeUnit.SECONDS, 1);
//
//        while (true) {
//            Values values = mag.getHeading();
//            System.out.println(values);
//            TimingUtils.delay(TimeUnit.MILLISECONDS, 250);
//        }
//    }

    public static void main(String[] args) {
        MMC3416xPJ mag = new MMC3416xPJ();
        System.out.println(mag);

        TimingUtils.delay(TimeUnit.SECONDS, 1);

        Scanner userInput = new Scanner(System.in);
        while (true) {
            Values values = mag.getHeading();
            System.out.println(values);
            double D = Math.atan(values.getGauss(Axis.X)/values.getGauss(Axis.Y)) * (180/Math.PI);
            System.out.format("Degrees: %s%n", D);
            String input = userInput.nextLine();
            TimingUtils.delay(TimeUnit.MILLISECONDS, 250);
        }
    }


    private class DebugFunction implements Function<Status, Boolean>
    {
        Function<Status, Boolean> function;
        DebugFunction(Function<Status, Boolean> function) {
            this.function = function;
        }

        @Override
        public Boolean apply(Status status) {
            System.out.println(status);
            return function.apply(status);
        }
    }
}
