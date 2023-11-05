package org.xenei.robot.map;

import org.xenei.robot.navigation.Coordinates;

public class PlanRecord  {
    private Coordinates position;
    private double cost;
    
    public PlanRecord(Coordinates position, double cost) {
        this.position = position;
        this.cost = cost;
    }
    @Override
    public int hashCode() {
        return position.hashCode();
    }
    
    @Override
    public boolean equals(Object pr) {
        if (pr instanceof PlanRecord) {
            return position.equals(((PlanRecord)pr).position);
        }
        return false;
    }
    
    public double cost() {
        return cost;
    }
    
    public Coordinates position() {
        return position;
    }
    
    public void setImpossible() {
        cost = Double.POSITIVE_INFINITY;
    }
    @Override
    public String toString() {
        return String.format("PlanRecord[x:%s, y:%s, d:%s]", position.getX(), position.getY(), cost);
    }
}