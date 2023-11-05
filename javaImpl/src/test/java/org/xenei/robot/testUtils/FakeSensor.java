package org.xenei.robot.testUtils;

import static org.junit.Assert.assertArrayEquals;

import org.junit.jupiter.api.Test;
import org.xenei.robot.map.ActiveMap;
import org.xenei.robot.map.utils.Sensor;
import org.xenei.robot.navigation.Coordinates;
import org.xenei.robot.navigation.Position;

public class FakeSensor implements Sensor {

    private static final int blocksize = 17;
    private static final double radians = Math.toRadians( 360.0/blocksize );
    private final ActiveMap map;
    
    public FakeSensor(ActiveMap map) {
        this.map = map;
    }
    
    public Coordinates[] sense(Position position) {
        Coordinates[] result = new Coordinates[blocksize];
        
        for (int i=0;i<blocksize;i++) {
            result[i] = look(position, position.getHeadingRadians()+(radians*i) );
        }
        return result;
    }
    
    private Coordinates look(Coordinates position, double heading) {
        Coordinates c = null;
        for (int range=1;range<=ActiveMap.maxRange;range++) {
            c = Coordinates.fromRadians(heading, range*map.scale());
            if (map.isEnabled(position.plus(c))) {
                return c;
            }
        }
        return Coordinates.fromRadians(heading, Double.POSITIVE_INFINITY);
    }
    
   
}
