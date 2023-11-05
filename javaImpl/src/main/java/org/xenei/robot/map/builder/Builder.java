package org.xenei.robot.map.builder;

import org.xenei.robot.map.ActiveMap;
import org.xenei.robot.map.PlanHistory;
import org.xenei.robot.map.PlanRecord;
import org.xenei.robot.navigation.Coordinates;

public class Builder {
    double degrees = 360/16.0;
    ActiveMap map = new ActiveMap();
    
    public void populateMap(ActiveMap map, PlanHistory planHistory, Coordinates absolute, Coordinates target) {
        for (int i=0;i<16;i++) {
            Coordinates cmd = Coordinates.fromDegrees(i*degrees, i*20);
            map.enable( cmd );
            Coordinates cmdAbs = absolute.plus(cmd);
            planHistory.add( new PlanRecord( cmdAbs, cmdAbs.distanceTo(target)));
            System.out.println( cmd );
        }
    }

}
