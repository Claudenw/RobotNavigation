package org.xenei.robot.common.planning;

import java.util.Stack;

public class TargetStack extends Stack<Segment> {
    public TargetStack() {
        super();
    }

    @Override
    public Segment push(Segment item) {
        if (this.size() == 2) {
            this.pop();
        }
        if (this.contains(item)) {
            while (!item.getNextLocation().sameCoordinate(this.pop().getNextLocation())) {
                // all activity in the while statement
            }
        }
        return super.push(item);
    }
}
