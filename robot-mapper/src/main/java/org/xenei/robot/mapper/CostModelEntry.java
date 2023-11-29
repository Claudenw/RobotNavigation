package org.xenei.robot.mapper;

import org.xenei.robot.common.utils.PointUtils;

import mil.nga.sf.Point;

public class CostModelEntry {
    public final Point a;
    public final Point b;
    public final double weight;

    public CostModelEntry(Point a, Point b, double weight) {
        this.a = a;
        this.b = b;
        this.weight = weight;
    }

    public double cost() {
        return dist() + weight;
    }

    public double dist() {
        return PointUtils.distance(a, b);
    }

    @Override
    public String toString() {
        return String.format("%s -> %s (%.4f)", a, b, weight);
    }
}