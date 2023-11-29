package org.xenei.robot.rpi;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.concurrent.TimeUnit;

import com.diozero.api.I2CDevice;

public class Magneto5Click {
    
    private static final int CONTROLLER = 1;
    private static final int ADDRESS = 0x30;
    
    private static byte REG_PRODUCT_ID =        0x20;
    private static byte PRODUCT_ID =            0x06;
    
    public static byte OK =          0x00;
    public static byte ERROR =  (byte)0xFF;
    
    public enum Axis { X, Y, Z}
       
   
    public enum Resolution { _16bits_8ms(2048f, (byte)0), _16bits_4ms(2048f,(byte)0), _14bits_2ms(512f, (byte)2), _12bits_1ms(128f, (byte)4);
        private float max;
        private byte flag;
        
        Resolution(float max, byte flag) {
            this.max=max;
            this.flag = flag;
        }

        public float getMax() {
            return max;
        }

        public float getFlag() {
            return flag;
        }
        
    };

    private final I2CDevice device;
    private Resolution resolution;
    private boolean continuous;
    
    public Magneto5Click() {
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

    public Status setResolution(Resolution resolution) {
        return new InternalControl().setResolution(resolution).execute();
    }
    
    public Status selfTest() {
        return new InternalControl().setSelfTest().execute();
    }
    
    public Status swReset() {
        return new InternalControl().setSWReset().execute();
    }
    
    private boolean checkMask(byte result, byte mask) {
        return (result & mask) != 0;
    }

    private byte writeThenRead(byte cmd) {
        device.writeByte(cmd);
        return device.readByte();
    }
    
    private Status getStatus()
    {
        return new Status();
    }
    
    private void delay(int ms) {
        try {
            TimeUnit.MILLISECONDS.sleep(ms);
        } catch (InterruptedException e) {
            // log error
        }
    }
    
    public byte getProductId ()
    {    
        byte result = 0;
        if (getStatus().readDone())
        {
            result = writeThenRead(REG_PRODUCT_ID);
            
            if (result != PRODUCT_ID)
            {
                result = ERROR;
            }
        }
        delay(10);
        return result;
    }

    public Values getData () { 
        return new Values();
    }
    
    public void reset()
    {
        new Configuration().setReset().execute();
    }
    
    public class Status {
        private static byte REG_STATUS =            0x06;
        /**
         * Check Status
         */
        private static byte MEASUREMENT_DONE =0x01;
        private static byte PUMP_ON =         0x02;
        private static byte READ_DONE =       0x04;
        private static byte SELFTEST_OK =     0x08;

        
        private final byte status;
        
        Status() {
            status = writeThenRead(REG_STATUS);
        }
        
        private Status(byte b) {
            this.status = b;
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
    }
    
    enum Frequency {
        HZ1_5, HZ13, HZ25, HZ50 
    }
    
    public class Configuration {
        private static byte INTERNAL_CONTROL_0 =0x07;
        
        private static byte RESET =             0x40;
        private static byte SET =               0x20;
        private static byte REFILL_CAP =       (byte) 0x80;
        private static byte NO_BOOST =          0x10;
        private static byte CONTINUOUS_MODE =   0x02;
        private static byte TAKE_MEASUREMENT =  0x01;
        
        private byte value = 0;
        
        public Configuration setCapRefill() { value |= REFILL_CAP; return this; }
        public Configuration setReset() { value |= RESET; return this;}
        public Configuration setSet() { value |= SET; return this;}
        public Configuration setDisableBoost() { value |= NO_BOOST; return this;}
        public Configuration setFrequency(Frequency freq) {
            value |= (byte)(freq.ordinal() << 2);
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
            device.writeByteData(INTERNAL_CONTROL_0, value);
            continuous = checkMask(value,CONTINUOUS_MODE);
            if (!checkMask(value,RESET)) {
                delay(100);
            }
            return new Status();
        }
    }
    
    private class InternalControl {
        private static byte INTERNAL_CONTROL_1 =0x08;
        private static byte SOFT_RESET =       (byte) 0x80;
        private static byte SELFTEST =      0x20;

        private byte value = 0;
        
        InternalControl setSWReset() { value |= SOFT_RESET ; return this; }
        InternalControl setSelfTest() { value |= SELFTEST; return this; }
        InternalControl setResolution(Resolution resolution) {
            value |= resolution.flag; return this;
        }
        
        Status execute() {
            device.writeByteData(INTERNAL_CONTROL_1, value);
            delay(100);
            return new Status();
        }
    }
    
    public class Values {
        ShortBuffer data;
        
        Values() {
            byte[] buffer = new byte[6];
            Status status = new Status(Status.READ_DONE);

            if (! continuous) {
                new Configuration().setCapRefill().setTakeMeasurement().execute();
                status = getStatus();
            }

            if (status.measurementDone() || status.readDone())
            {    
                device.writeByte((byte)0x00);
                device.readBytes(buffer);
            }
            data =  ByteBuffer.wrap(buffer).asShortBuffer();
        }
        
        public ShortBuffer getData() {
            return data.asReadOnlyBuffer();
        }
        
        public FloatBuffer getValues() {
         FloatBuffer fb = FloatBuffer.allocate(3);
            for (int i=0;i<3; i++) {
                fb.put( i, data.get(i) / resolution.max);
            }
            return fb;
        }
        
        public float getAxisValue(Axis axis)
        {
            return data.get(axis.ordinal()) / resolution.max;
        }

        public short getAxisData(Axis axis)
        {
            return data.get(axis.ordinal());
        }
    }

}
