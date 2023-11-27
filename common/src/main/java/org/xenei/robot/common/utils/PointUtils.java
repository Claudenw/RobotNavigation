package org.xenei.robot.common.utils;

import java.util.Comparator;

import org.apache.commons.math3.util.Precision;
import org.xenei.robot.common.AngleUnits;
import org.xenei.robot.common.AngleUtils;
import org.xenei.robot.common.Coordinates;
import org.xenei.robot.common.DoubleUtils;

import mil.nga.sf.Point;

public class PointUtils {
    private PointUtils() {}
    
    public static Comparator<Point> XYCompr = (one, two) -> {
        int x = Double.compare(one.getX(), two.getX());
        return x == 0 ? Double.compare(one.getY(), two.getY()) : x;
    };
    
    public static String toString(Point p) {
        return String.format("{%f,%f}", p.getX(), p.getY());
    }

    public static String toString(Point p, int precision) {
        String fmt = String.format("{%%.%sf, %%.%sf}", precision, precision);
        return String.format(fmt, p.getX(), p.getY());
    }

    public static Point minus(Point one, Point other) {
        return new Point( one.getX() - other.getX(), one.getY()-other.getY());
    }
    
    public static Point plus(Point one, Point other) {
        return new Point( one.getX()+other.getX(), one.getY()+other.getY());
    }
    
    public static double angleTo(Point from, Point dest) {
        Point diff = minus(from, dest);
        double theta = AngleUtils.normalize(Math.atan(diff.getY() / diff.getX()));
        boolean yNeg = DoubleUtils.isNeg(diff.getY());
        boolean tNeg = DoubleUtils.isNeg(theta);

        if (yNeg && !tNeg) {
            theta -= Math.PI;
        } else if (!yNeg && tNeg) {
            theta += Math.PI;
        }
        // angle will be pointing the wront way, so reverse it.
        return AngleUtils.normalize(theta + Math.PI);
    }
    
    
    public static double distance(Point one, Point other) {
        double dx = one.getX()-other.getX();
        double dy = one.getY()-other.getY();
        return Math.sqrt( dx*dx + dy*dy);
    }
    
    public static boolean equivalent(Point one, Point other, double delta) {
        return Precision.equals(one.getX(), other.getX(), delta) &&
                Precision.equals(one.getY(), other.getY(), delta);
    }
}
