package org.xenei.robot.rpi;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

import org.locationtech.jts.geom.Coordinate;
import org.xenei.robot.common.Compass;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.utils.DoubleUtils;
import org.xenei.robot.rpi.sensors.MMC3416xPJ;
import org.xenei.robot.rpi.sensors.MMC3416xPJ.Axis;

public class CompassImpl implements Compass {
    private MMC3416xPJ compass = new MMC3416xPJ();
    private final int limit = 10;
    private final MMC3416xPJ.Values[] samples;
    private int position = 0;
    private Timer timer;;
    private float XSum = 0.0f;
    private float YSum = 0.0f;
    private final ReentrantLock lock;
    private TimerTask task = new TimerTask() {
        @Override
        public void run() {
            MMC3416xPJ.Values oldSample = samples[position];
            samples[position] = compass.getHeading();
            lock.lock();
            try {
                XSum += samples[position].getAxisValue(Axis.X)-oldSample.getAxisValue(Axis.X);
                YSum += samples[position].getAxisValue(Axis.Y)-oldSample.getAxisValue(Axis.Y);
            } finally {
                lock.unlock();
            }
            position = Math.floorMod(position+1, limit);
        }};
    
    public CompassImpl() {
        lock = new ReentrantLock();
        samples = new MMC3416xPJ.Values[limit];
        for (int i=0;i<limit;i++) {
            samples[i] = compass.getHeading();
            XSum += samples[i].getAxisValue(Axis.X);
            YSum += samples[i].getAxisValue(Axis.Y);
        }
        position = 0;
        timer = new Timer();
        timer.schedule(task, 0, 250);
        
    }
    
    @Override
    public double heading() {
        float x;
        float y;
        lock.lock();
        try {
            x = XSum;
            y = YSum;
        } finally {
            lock.unlock();
        }
        Location heading = Location.from(x,y);
        return DoubleUtils.truncate(heading.theta(), 2);
    }
    
    public static void main(String[] args) throws InterruptedException {
        Compass c = new CompassImpl();
        while (true) {
            System.out.format( "Heading: %s\n", c.heading());
            Thread.sleep(500);
        }
    }

}
