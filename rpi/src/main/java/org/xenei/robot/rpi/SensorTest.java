package org.xenei.robot.rpi;

import java.util.HexFormat;
import java.util.concurrent.TimeUnit;

import org.xenei.robot.common.AngleUnits;
import org.xenei.robot.common.Coordinates;
import org.xenei.robot.rpi.MMC3416xPJ.Axis;
import org.xenei.robot.rpi.MMC3416xPJ.Frequency;
import org.xenei.robot.rpi.MMC3416xPJ.Status;
import org.xenei.robot.rpi.MMC3416xPJ.Values;

public class SensorTest {
    public static void main(String[] args) {
        MMC3416xPJ mag = new MMC3416xPJ();
        mag.reset();
        //Magneto5Click.Configuration cfg = mag.getConfiguration();
        //cfg.setContinuousMode().setFrequency(Frequency.HZ1_5);
        Status status = mag.getStatus();
        print(mag);
        print(status);
        
        delay(TimeUnit.SECONDS, 1);
        
        while (true) {
            Values values = mag.getHeading();
            print(values);
            Coordinates c = Coordinates.fromXY( values.getAxisValue(Axis.X), values.getAxisValue(Axis.Y));
            Coordinates d = Coordinates.fromXY( values.getAxisData(Axis.X), values.getAxisData(Axis.Y));
            System.out.format( " value: %s  data: %s\n", c.getTheta(AngleUnits.DEGREES), d.getTheta(AngleUnits.DEGREES) );
            delay(TimeUnit.MILLISECONDS, 250 );
        }
    }
    
    public static void print(Status status) {
        System.out.format( "Status: measure:%s read:%s pump-on:%s self-test:%s\n", status.measurementDone(), status.readDone(), 
                status.pumpOn(), status.selfTestOk());
    }
    
    private static void print(MMC3416xPJ mag) {
        System.out.format( "Magneto: continuous:%s product:%s resolution:%s\n", mag.isContinuous(), mag.getProductId(),
                mag.getResolution()); 
    }
    
    private static void print(Values values) {
        for (Axis axis : Axis.values()) {
            System.out.format( "%s{ 0x%x %s %.5f} ", axis,  values.getAxisData(axis), values.getAxisData(axis), values.getAxisValue(axis)); 
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
