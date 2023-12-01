package org.xenei.robot.common.utils;

import java.util.concurrent.TimeUnit;

public class TimingUtils {
    
    private TimingUtils() {}
    

    public static void delay(TimeUnit unit, int length) {
        try {
            unit.sleep(length);
        } catch (InterruptedException e) {
            // log error
        }
    }

    public static void delay(int ms) {
        delay(TimeUnit.MILLISECONDS, ms);
    }


}
