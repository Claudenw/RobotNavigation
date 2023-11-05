package org.xenei.robot.map.builder;


import org.junit.jupiter.api.Test;
import org.xenei.robot.map.ActiveMap;
import org.xenei.robot.navigation.Coordinates;

public class BuilderTest {

    @Test
    public void x() {
        ActiveMap map = new ActiveMap(10);
        Builder builder = new Builder();
        builder.updateMap(map);
        System.out.println( map.toString() );
        Coordinates target = Coordinates.fromXY(24,56);
        
    }
}
