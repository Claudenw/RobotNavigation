package org.xenei.robot.rpi.drivers;


import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.xenei.robot.rpi.drivers.Motor.SteppingStatus;

import com.diozero.internal.spi.NativeDeviceFactoryInterface;
import com.diozero.sbc.DeviceFactoryHelper;
import com.diozero.util.Diozero;
import uk.pigpioj.*;

public class TestDriver {
    ULN2003 motor;
    public TestDriver() throws InterruptedException {
        motor = new ULN2003(ULN2003.Mode.WAVE_DRIVE, ULN2003.STEPPER_28BYJ48, 17, 27, 22, 23 );
    }
    
    public static void main(String[] args) throws InterruptedException {
        runDriver();
    }
    
    public static void blinkLights() {

                int gpio = 17;
                try (PigpioInterface pigpio_impl = PigpioJ.autoDetectedImplementation()) {
                    pigpio_impl.setMode(gpio, PigpioConstants.MODE_PI_OUTPUT);
                    pigpio_impl.write(gpio, true);
                    Thread.sleep(1000);
                    pigpio_impl.write(gpio, false);
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
    }
    
    public static void runDriver() throws InterruptedException {
        TestDriver driver = new TestDriver();
        SteppingStatus status = driver.motor.prepareRun(60000, 100);
        ExecutorService executor = Executors.newFixedThreadPool(1);
        Future<SteppingStatus> future = executor.submit(status);
        while (!future.isDone()) {
            System.out.println( String.format( "s:%s r:%s ", 
                    status.fwdSteps(), status.fwdRotation()));
        }
        System.out.println( "done");
        try {
            driver.motor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Diozero.shutdown();
        System.out.println( "shutdown complete");
    }
    
    public static void systemInfo() {
        NativeDeviceFactoryInterface factory = DeviceFactoryHelper.getNativeDeviceFactory();
        System.out.println( "Board name: "+factory.getBoardInfo().getLongName());
        System.out.println( "Board make: "+factory.getBoardInfo().getMake());
        System.out.println( "Board model: "+factory.getBoardInfo().getModel());
        System.out.println( "Board os: "+factory.getBoardInfo().getOperatingSystemId());
        System.out.println( "Board os version: "+factory.getBoardInfo().getOperatingSystemVersion());
        System.out.println( "Board memory (Kb): "+factory.getBoardInfo().getMemoryKb());
        
        factory.getBoardPinInfo().getHeaders().forEach((s,m) -> { System.out.println(s);
        m.forEach((i,p) -> { System.out.format( "  %s gpio:%s name:%s pin:%s %s\n", i, p.getDeviceNumber(),
                p.getName(), p.getPhysicalPin(), p.toString());
        });
        });
    }

}
