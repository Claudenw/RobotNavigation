package org.xenei.robot.common;

import org.locationtech.jts.geom.Coordinate;

public class NavigationSnapshot {
    public final Position currentPosition;
    public final Coordinate target;
    
    public NavigationSnapshot(Position currentPosition, Coordinate target) {
        this.currentPosition = currentPosition;
        this.target = target; 
    }
    
    public double heading() {
        return currentPosition.getHeading();
    }
}
