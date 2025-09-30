package org.xenei.robot.mapper;

import org.locationtech.jts.geom.Coordinate;

public class CostModelEntry {
    public final Coordinate a;
    public final Coordinate b;
    public final double weight;

    public CostModelEntry(Coordinate a, Coordinate b, double weight) {
        this.a = a;
        this.b = b;
        this.weight = weight;
    }

    public double cost() {
        return dist() + weight;
    }

    public double dist() {
        return a.distance(b);
    }

    @Override
    public String toString() {
        return String.format("%s -> %s (%.4f)", a, b, weight);
    }
}