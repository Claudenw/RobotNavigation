package org.xenei.robot.navigation;

public final class Point {
    public final double x;
    public final double y;
    public final int hashCode;

    public Point(double x, double y) {
        this.x = x;
        this.y = y;
        this.hashCode = Double.hashCode(Math.sqrt(x * 2 + y * 2));
    }

    @Override
    public String toString() {
        return String.format("{%.0f,%.0f}", x, y);
    }

    public String toString(int precision) {
        String fmt = String.format("{%%.%sf, %%.%sf}", precision, precision);
        return String.format(fmt, x, y);
    }
    
    @Override
    public boolean equals(Object other) {
        if (other instanceof Point) {
            Point p = (Point) other;
            return x == p.x && y == p.y;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }
    
    public Point minus(Point other) {
        return new Point( x - other.x, this.y-other.y);
    }
    
    public Point plus(Point other) {
        return new Point( x+other.x, this.y+other.y);
    }
    
    public double distance(Point other) {
        double dx = x-other.x;
        double dy = y-other.y;
        return Math.sqrt( dx*dx + dy*dy);
    }
}