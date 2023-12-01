package org.xenei.robot.common.planning;

import org.xenei.robot.common.Coordinates;
import org.xenei.robot.common.utils.PointUtils;

import mil.nga.sf.Point;

public class Target extends Point implements Comparable<Target> {
    private double cost;

    public Target(Coordinates position, Coordinates target) {
        this(position, position.distanceTo(target));
    }

    public Target(Point point, double cost) {
        super(point);
        this.cost = cost;
    }

    public double cost() {
        return cost;
    }

    @Override
    public String toString() {
        return String.format("Target {%s cost:%.4f}", PointUtils.toString(this, 1), cost);
    }

    @Override
    public boolean equals(Object pr) {
        if (pr == this) {
            return true;
        }
        if (pr instanceof Target) {
            return super.equals(pr);
        }
        return false;
    }

    @Override
    public int compareTo(Target other) {
        int x = Double.compare(cost, other.cost);
        return x == 0 ? PointUtils.XYCompr.compare(this, other) : x;
    }
}
