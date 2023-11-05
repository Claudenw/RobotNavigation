package org.xenei.robot.map.utils;

import org.xenei.robot.map.ActiveMap;
import org.xenei.robot.navigation.Coordinates;

public class ActiveMapBuilder {

    private ActiveMap activeMap;
    
    public ActiveMapBuilder(int scale) {
        activeMap = new ActiveMap(scale);
    }
    
    public void set(int x, int y) {
        Coordinates c = Coordinates.fromXY(x*activeMap.scale(), y*activeMap.scale());
        activeMap.enable(c);
    }
    
    public void setY(int x, int start, int end) {
        for (int y=start;y<=end;y++) 
            set(x,y);
    }
    
    public void setX(int y ,int start, int end) {
        for (int x=start;x<=end;x++)
            set(x,y);
    }
    
    public void clear(int x, int y) {
        Coordinates c = Coordinates.fromXY(x*activeMap.scale(), y*activeMap.scale());
        activeMap.disable(c);
    }
    
    public ActiveMap getMap() {
        return activeMap;
    }
}
