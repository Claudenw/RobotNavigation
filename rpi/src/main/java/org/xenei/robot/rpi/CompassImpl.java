package org.xenei.robot.rpi;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.common.Compass;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.ScaleInfo;
import org.xenei.robot.common.utils.AngleUtils;
import org.xenei.robot.common.utils.DoubleUtils;
import org.xenei.robot.rpi.sensors.MMC3416xPJ;
import org.xenei.robot.rpi.sensors.MMC3416xPJ.Axis;

public class CompassImpl implements Compass {
    private static final Logger LOG = LoggerFactory.getLogger(CompassImpl.class);
    private MMC3416xPJ compass = new MMC3416xPJ();
    private final int limit = 10;
    private final MMC3416xPJ.Values[] samples;
    private int position = 0;
    private Timer timer;;
    private float XSum = 0.0f;
    private float YSum = 0.0f;
    private final ReentrantLock lock;
    private static final int accuracy = 2;
    private TimerTask task = new TimerTask() {
        @Override
        public void run() {
            MMC3416xPJ.Values oldSample = samples[position];
            samples[position] = compass.getHeading();
            lock.lock();
            try {
                XSum += samples[position].getAxisValue(Axis.X) - oldSample.getAxisValue(Axis.X);
                YSum += samples[position].getAxisValue(Axis.Y) - oldSample.getAxisValue(Axis.Y);
            } finally {
                lock.unlock();
            }
            position = Math.floorMod(position + 1, limit);
        }
    };

    public CompassImpl() {
        lock = new ReentrantLock();
        samples = new MMC3416xPJ.Values[limit];
        for (int i = 0; i < limit; i++) {
            samples[i] = compass.getHeading();
            XSum += samples[i].getAxisValue(Axis.X);
            YSum += samples[i].getAxisValue(Axis.Y);
        }
        position = 0;
        timer = new Timer();
        timer.schedule(task, 0, 250);
        LOG.info("Compass: "+compass);
    }

    /* package private for testing */
    static double heading(double x, double y) {

        if (x == 0 && y == 0) {
            return 0;
        }

        double hX = x == 0 ? 0 : -x;
        double hY = y == 0 ? 0 : -y;

        double theta = Math.atan(hY / hX);
        boolean yNeg = DoubleUtils.isNeg(hY);
        boolean tNeg = DoubleUtils.isNeg(theta);

        if (yNeg && !tNeg) {
            theta -= Math.PI;
        } else if (!yNeg && tNeg) {
            theta += Math.PI;
        }
        // angle will be pointing the wrong way, so reverse it.
        return AngleUtils.normalize(theta + Math.PI);
    }
    
    @Override
    public int decimalPlaces() {
        return accuracy;
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
        return DoubleUtils.round(heading(x, y), accuracy);
    }

    @Override
    public double instantaneousHeading() {
        MMC3416xPJ.Values values = compass.getHeading();
        return DoubleUtils.round(heading(values.getAxisValue(Axis.X), values.getAxisValue(Axis.Y)), accuracy);
    }
    
    @Override
    public double sd() {
        double mean = 0;
        double headings[] = new double[limit];
        lock.lock();
        try {
            for (int i = 0; i < limit; i++) {
                headings[i] = heading(samples[i].getAxisValue(Axis.X), samples[i].getAxisValue(Axis.Y));
                mean += headings[i];
            }
        } finally {
            lock.unlock();
        }
        mean /= limit;
        double sum = 0;
        double value = 0;
        for (int i = 0; i < limit; i++) {
            value = headings[i] - mean;
            sum += (value * value);
        }
        return DoubleUtils.round(Math.sqrt(sum / (limit - 1)), accuracy + 1);
    }

    @Override
    public String toString() {
        double h = heading();
        double sd = sd();
        return String.format("Compass[Heading: %s %s degrees  sd:%s]", h, DoubleUtils.round(Math.toDegrees(h), accuracy + 1), sd);
    }

    public static void main(String[] args) throws InterruptedException {
        CompassImpl c = new CompassImpl();
        while (true) {
            double h = c.heading();
            System.out.println( c.toString() );
            Thread.sleep(500);
        }
    }

}
