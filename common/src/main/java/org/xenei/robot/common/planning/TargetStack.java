package org.xenei.robot.common.planning;

import org.locationtech.jts.geom.Coordinate;

import java.util.Stack;

public class TargetStack extends Stack<Coordinate> {
    public TargetStack() {
        super();
    }

    @Override
    public Coordinate push(Coordinate item) {
        if (this.size() == 2) {
            this.pop();
        }
        if (this.contains(item)) {
            while (!item.equals2D(this.pop())) {
                // all activity in the while statement
            }
        }
        return super.push(item);
    }
}
