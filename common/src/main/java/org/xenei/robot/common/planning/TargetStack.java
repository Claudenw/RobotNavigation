package org.xenei.robot.common.planning;

import org.xenei.robot.common.FrontsCoordinate;

import java.util.Stack;

public class TargetStack extends Stack<FrontsCoordinate> {
	public TargetStack() {
		super();
	}

	@Override
	public FrontsCoordinate push(FrontsCoordinate item) {
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
