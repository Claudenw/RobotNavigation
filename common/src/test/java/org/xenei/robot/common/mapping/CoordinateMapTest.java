package org.xenei.robot.common.mapping;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.testUtils.MapLibrary;

public class CoordinateMapTest {

    CoordinateMap underTest;
    
    @Test
    public void lookTest() {
        underTest = MapLibrary.map2('#');
        Position l = Position.from( -2, -2);
        Location t = Location.from( -1, 1);
        double heading = l.headingTo(t);
        double range = l.distance(t);
        Optional<Location> result = underTest.look(l, heading, range );
        assertTrue( result.isPresent());
    }
}
