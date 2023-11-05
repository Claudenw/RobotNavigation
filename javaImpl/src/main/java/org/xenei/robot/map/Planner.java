package org.xenei.robot.map;

import org.xenei.robot.navigation.Coordinates;
import org.xenei.robot.navigation.Position;
import org.xenei.robot.map.builder.Builder;
import org.xenei.robot.map.utils.Sensor;

public class Planner {
    PlanHistory planHistory;
    ActiveMap activeMap;
    Position position;
    Coordinates target;
    Builder builder;
    Sensor sensor;
    
    Planner(Coordinates initial, Coordinates target, Sensor sensor) {
        position = new Position(initial);
        activeMap = new ActiveMap(10);
        builder = new Builder();
        this.target = target;
        this.sensor = sensor;
        
        planHistory = new PlanHistory(new PlanRecord(initial, initial.distanceTo(target)));
        builder.populateMap(activeMap, planHistory, initial, target);
    }
    
    public boolean step() {
        PlanRecord record = planHistory.pop();
        if (record == null) {
            return false;
        }
        Coordinates change = position.minus(record.position());
        Position nextPosition = position.nextPosition(change);
        activeMap.shift(change);
        
    }
}
