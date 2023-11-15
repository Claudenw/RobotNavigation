package org.xenei.robot.planner;

import org.xenei.robot.navigation.Coordinates;

public class PlanRecord implements Comparable<PlanRecord> {
    private Coordinates coordinates;
    private double cost;
    private double maskingCost;

    /**
     * 
     * @param position Coordinates of the record.
     * @param cost distance to target.
     */
    public PlanRecord(Coordinates position, double cost) {
        this.coordinates = position;
        this.cost = cost;
        this.maskingCost = Double.NaN;
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

    public void setMaskingCost(double value) {
        maskingCost = value;
    }

    public void clearMaskingCost() {
        maskingCost = Double.NaN;
    }

    /**
     * 
     * @return distance to target
     */
    public double cost() {
        return Double.isNaN(maskingCost) ? cost : maskingCost;
    }

    public Coordinates coordinates() {
        return coordinates;
    }

    public void setImpossible() {
        cost = Double.POSITIVE_INFINITY;
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