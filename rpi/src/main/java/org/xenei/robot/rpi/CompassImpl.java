package org.xenei.robot.rpi;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.common.Compass;
import org.xenei.robot.common.utils.AngleUtils;
import org.xenei.robot.common.utils.DoubleUtils;
import org.xenei.robot.rpi.sensors.MMC3416xPJ;
import org.xenei.robot.rpi.sensors.MMC3416xPJ.Axis;

public class CompassImpl implements Compass {
    private static final Logger LOG = LoggerFactory.getLogger(CompassImpl.class);
    private final MMC3416xPJ compass = new MMC3416xPJ();
    private final static int SAMPLE_SIZE = 10;
    private final MMC3416xPJ.Values[] samples;
    private int position = 0;
    private float XSum = 0.0f;
    private float YSum = 0.0f;
    private final ReentrantLock lock;
    private static final int accuracy = 2;

    public CompassImpl() {
        lock = new ReentrantLock();
        samples = new MMC3416xPJ.Values[SAMPLE_SIZE];
        for (int i = 0; i < SAMPLE_SIZE; i++) {
            samples[i] = compass.getHeading();
            XSum += samples[i].getAxisValue(Axis.X);
            YSum += samples[i].getAxisValue(Axis.Y);
        }
        position = 0;
        Timer timer = new Timer();
        TimerTask task = new TimerTask() {
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
                position = Math.floorMod(position + 1, SAMPLE_SIZE);
            }
        };
        timer.schedule(task, 0, 250);
        LOG.info("Compass: {}", compass);
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
        double headings[] = new double[SAMPLE_SIZE];
        lock.lock();
        try {
            for (int i = 0; i < SAMPLE_SIZE; i++) {
                headings[i] = heading(samples[i].getAxisValue(Axis.X), samples[i].getAxisValue(Axis.Y));
                mean += headings[i];
            }
        } finally {
            lock.unlock();
        }
        mean /= SAMPLE_SIZE;
        double sum = 0;
        double value = 0;
        for (int i = 0; i < SAMPLE_SIZE; i++) {
            value = headings[i] - mean;
            sum += (value * value);
        }
        return DoubleUtils.round(Math.sqrt(sum / (SAMPLE_SIZE - 1)), accuracy + 1);
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
            System.out.println(c);
            double h = c.heading();
            double rawdeg = Math.toDegrees(h);
            double deg =  (rawdeg < 0) ? rawdeg + 360 : rawdeg;
            System.out.format("Compass[Heading: %s %s (%s) degrees]%n", h, DoubleUtils.round(rawdeg, accuracy + 1), DoubleUtils.round(deg, accuracy + 1));
            Thread.sleep(500);
        }
    }
}
