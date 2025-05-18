package org.xenei.robot.rpi;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.common.Compass;
import org.xenei.robot.common.utils.AngleUtils;
import org.xenei.robot.common.utils.DoubleUtils;
import org.xenei.robot.rpi.sensors.mmc3416xpj.MMC3416xPJ;
import org.xenei.robot.rpi.sensors.mmc3416xpj.Axis;
import org.xenei.robot.rpi.sensors.mmc3416xpj.Values;

public class CompassImpl implements Compass {
    private static final Logger LOG = LoggerFactory.getLogger(CompassImpl.class);
    private final MMC3416xPJ compass = new MMC3416xPJ();
    private final static int SAMPLE_SIZE = 10;
    private final static int POLL_INTERVAL = 250;
    private final Values[] samples;
    private int position = 0;
    private double XSum = 0.0;
    private double YSum = 0.0;
    private final Timer timer;
    private final ReentrantLock lock;
    private static final int accuracy = 2;
    private Supplier<Boolean> pauseFunc;

    public CompassImpl() {
        this.pauseFunc = () -> false;
        lock = new ReentrantLock();
        samples = new Values[SAMPLE_SIZE];
        for (int i = 0; i < SAMPLE_SIZE; i++) {
            samples[i] = compass.getHeading();
            XSum += samples[i].getGauss(Axis.X);
            YSum += samples[i].getGauss(Axis.Y);
        }
        position = 0;
        timer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                    Values oldSample = samples[position];
                    samples[position] = compass.getHeading();
                    lock.lock();
                    try {
                        XSum += samples[position].getGauss(Axis.X) - oldSample.getGauss(Axis.X);
                        YSum += samples[position].getGauss(Axis.Y) - oldSample.getGauss(Axis.Y);
                    } finally {
                        lock.unlock();
                    }
                    position = Math.floorMod(position + 1, SAMPLE_SIZE);
            }
        };
        timer.schedule(task, 0, POLL_INTERVAL);
        LOG.info("Compass: {}", compass);
    }

    public void setPauseFunc(Supplier<Boolean> pauseFunc) {
        this.pauseFunc = pauseFunc;
    }

    /* package private for testing */
    /**
     *
     * @param xGauss the xGauss
     * @param yGauss the yGauss
     * @return the heading
     */
    static double heading(double xGauss, double yGauss) {

        /*
        Calculate the direction D by first checking to see if the X Gauss data is equal to 0 to prevent divide by 0 zero
         errors in the future calculations. If the X Gauss data is 0, check to see if the Y Gauss data is less than 0.
         If Y is less than 0 Gauss, the direction D is 90 degrees; if Y is greater than or equal to 0 Gauss, the direction
         D is 0 degrees.
         */
        if (xGauss == 0) {
            return yGauss < 0 ? AngleUtils.RADIANS_90 : 0d;
        }

        /*
        If the X Gauss data is not zero, calculate the arctangent of the Y Gauss and X Gauss data and convert from polar coordinates to degrees.
        D = arctan(yGaussData/xGaussData)∗(180/π)
        */
        double result = Math.atan(yGauss / xGauss);

        /*
        If the direction D is greater than 360 degrees, subtract 360 degrees from that value.
        */
        if (result > AngleUtils.PI_x_2) {
            result -= AngleUtils.PI_x_2;
        } else if (result < 0) {
            result += AngleUtils.PI_x_2;
        }
        return result;
    }

    public void settle() throws InterruptedException {
        Thread.sleep(SAMPLE_SIZE * POLL_INTERVAL);
    }
    
    @Override
    public int decimalPlaces() {
        return accuracy;
    }

    @Override
    public double heading() {
        double x;
        double y;
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
        Values values = compass.getHeading();
        return DoubleUtils.round(heading(values.getGauss(Axis.X), values.getGauss(Axis.Y)), accuracy);
    }
    
    @Override
    public double sd() {
        double mean = 0;
        double[] headings = new double[SAMPLE_SIZE];
        lock.lock();
        try {
            for (int i = 0; i < SAMPLE_SIZE; i++) {
                headings[i] = heading(samples[i].getGauss(Axis.X), samples[i].getGauss(Axis.Y));
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
        double oldDeg = -1;
        while (true) {
            double h = c.heading();
            double deg = DoubleUtils.round(Math.toDegrees(h), accuracy + 1);
            if (deg != oldDeg) {
                System.out.format("Compass[Heading: %s %s degrees]%n", h, deg);
                oldDeg = deg;
            }
            Thread.sleep(500);
        }
    }
}
