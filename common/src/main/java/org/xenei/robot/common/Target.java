package org.xenei.robot.common;

public class Target implements Comparable<Target> {
    private Coordinates coordinates;
    private double cost;

    public Target(Coordinates position, Coordinates target) {
        this(position, position.distanceTo(target));
    }

    public Target(Coordinates coordinates, double cost) {
        this.coordinates = coordinates;
        this.cost = cost;
    }

    public Coordinates coordinates() {
        return coordinates;
    }

    public double cost() {
        return cost;
    }

    @Override
    public String toString() {
        return String.format("Target {%s cost:%.4f}", coordinates, cost);
    }

    @Override
    public int hashCode() {
        return coordinates.hashCode();
    }

    @Override
    public boolean equals(Object pr) {
        if (pr == this) {
            return true;
        }
        if (pr instanceof Target) {
            return coordinates.equals(((Target) pr).coordinates);
        }
        return false;
    }

    @Override
    public int compareTo(Target other) {
        int x = Double.compare(cost, other.cost);
        return x == 0 ? Coordinates.XYCompr.compare(coordinates, other.coordinates) : x;
    }
}
