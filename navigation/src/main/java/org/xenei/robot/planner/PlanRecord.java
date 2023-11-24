package org.xenei.robot.planner;

import org.xenei.robot.common.Coordinates;

public class PlanRecord implements Comparable<PlanRecord> {
    private Coordinates coordinates;
    private double cost;
    
    public PlanRecord(Coordinates position, Coordinates target) {
        this(position,  position.distanceTo(target));
    }

    /**
     * 
     * @param position Coordinates of the record.
     * @param cost distance to target.
     */
    public PlanRecord(Coordinates position, double cost) {
        this.coordinates = position;
        this.cost = cost;
    }

    @Override
    public int hashCode() {
        return coordinates.hashCode();
    }

    @Override
    public boolean equals(Object pr) {
        if (pr instanceof PlanRecord) {
            return coordinates.equals(((PlanRecord) pr).coordinates);
        }
        return false;
    }

    /**
     * 
     * @return distance to target
     */
    public double cost() {
        return cost;
    }

    public Coordinates coordinates() {
        return coordinates;
    }

    @Override
    public String toString() {
        return String.format("PlanRecord[%s, cost:%.4f]", coordinates, cost());
    }

    @Override
    public int compareTo(PlanRecord other) {
        int x = Double.compare(cost, other.cost);
        return x == 0 ? Coordinates.XYCompr.compare(coordinates, other.coordinates) : x;
    }
}