package org.xenei.robot.planner;

import java.util.Comparator;

import org.xenei.robot.navigation.Coordinates;

public class PlanRecord {
    private Coordinates position;
    private double cost;
    private double maskingCost;

    public static Comparator<PlanRecord> CostCompr = (one, two) -> {
        int x = Double.compare(one.cost, two.cost);
        return x == 0 ? Coordinates.RangeCompr.compare(one.position, two.position) : x;
    };

    /**
     * 
     * @param position Coordinates of the record.
     * @param cost distance to target.
     */
    public PlanRecord(Coordinates position, double cost) {
        this.position = position;
        this.cost = cost;
        this.maskingCost = Double.NaN;
    }

    @Override
    public int hashCode() {
        return position.hashCode();
    }

    @Override
    public boolean equals(Object pr) {
        if (pr instanceof PlanRecord) {
            return position.equals(((PlanRecord) pr).position);
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

    public Coordinates position() {
        return position;
    }

    public void setImpossible() {
        cost = Double.POSITIVE_INFINITY;
    }

    @Override
    public String toString() {
        return String.format("PlanRecord[%s, cost:%.4f]", position, cost());
    }
}