package org.xenei.robot.rpi;

import java.util.concurrent.TimeUnit;

import org.xenei.robot.rpi.Magneto5Click.Axis;
import org.xenei.robot.rpi.Magneto5Click.Frequency;
import org.xenei.robot.rpi.Magneto5Click.Status;
import org.xenei.robot.rpi.Magneto5Click.Values;

public class SensorTest {
    public static void main(String[] args) {
        Magneto5Click mag = new Magneto5Click();
        
        Magneto5Click.Configuration cfg = mag.getConfiguration();
        cfg.setContinuousMode().setFrequency(Frequency.HZ1_5);
        Status status = cfg.execute();
        print(mag);
        print(status);
        
        delay(TimeUnit.SECONDS, 1);
        
        while (true) {
            Values values = mag.getData();
            print(values);
            delay(TimeUnit.MILLISECONDS, 250 );
        }
    }
    
    private static void print(Status status) {
        System.out.format( "Status: measure:%s read:%s pump-on:%s self-test:%s\n", status.measurementDone(), status.readDone(), 
                status.pumpOn(), status.selfTestOk());
    }
    
    private static void print(Magneto5Click mag) {
        System.out.format( "Magneto: continuous:%s product:%s \n", mag.isContinuous(), mag.getProductId()); 
    }
    
    private static void print(Values values) {
        for (Axis axis : Axis.values()) {
            System.out.format( "%s{ %s %.5f} ", axis, values.getAxisData(axis), values.getAxisValue(axis)); 
        }
        System.out.println();
    }
    
    private static void delay(TimeUnit unit, int ms) {
        try {
            unit.sleep(ms);
        } catch (InterruptedException e) {
            // log error
        }
    }
}
