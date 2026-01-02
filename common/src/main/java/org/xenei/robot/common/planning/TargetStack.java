package org.xenei.robot.common.planning;

import org.xenei.robot.common.mapping.MapLocation;

import java.util.Stack;

public class TargetStack extends Stack<MapLocation> {
    public TargetStack() {
        super();
    }

    @Override
    public MapLocation push(MapLocation item) {
        if (this.size() == 2) {
            this.pop();
        }
        if (this.contains(item)) {
            while (!item.sameCoordinate(this.pop())) {
                // all activity in the while statement
            }
        }
        return super.push(item);
    }
}
