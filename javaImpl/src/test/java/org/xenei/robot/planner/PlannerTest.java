package org.xenei.robot.planner;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.xenei.robot.testUtils.DoubleUtils.DELTA;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.xenei.robot.navigation.Coordinates;
import org.xenei.robot.navigation.Position;
import org.xenei.robot.testUtils.FakeSensor;
import org.xenei.robot.testUtils.MapLibrary;
import org.xenei.robot.utils.CoordinateMap;

public class PlannerTest {
    
    private CoordinateMap map = MapLibrary.map1('#');
    private FakeSensor sensor = new FakeSensor(map);
    
    private Planner underTest;


    @Test
    public void SenseTest() {
        Coordinates origin = Coordinates.fromXY(0,0);
        for (int x=0;x<=13;x++) {
            for (int y=0;y<=15;y++) {
                Coordinates c = Coordinates.fromXY(x, y);
                if (!map.isEnabled(c)) {
                    underTest = new Planner( sensor, origin);
                    underTest.sense(new Position(c, 0));
                    for( Coordinates c2 : underTest.getSensed()) {
                        assertTrue( "error with: "+c.toString(), map.isEnabled(c2));
                    }
                }
            }
        }
    }

    @Test
    public void setTargetTest() {
        Coordinates origin = Coordinates.fromXY(0,0);
        Position position = new Position(origin, 0);
        for (int x=0;x<=13;x++) {
            for (int y=0;y<=15;y++) {
                Coordinates c = Coordinates.fromXY(x, y);
                if (!map.isEnabled(c)) {
                    underTest = new Planner( sensor, origin);
                    underTest.setTarget(c, position);
                    verifyState();
                    assertEquals( position.coordinates(), underTest.getPath().get(0).coordinates());
                }
            }
        }
    }
    
    private CoordinateMap verifyState() {
        for( Coordinates c : underTest.getSensed()) {
            assertTrue( c+" should have been sensed.", map.isEnabled(c));
        }
        CoordinateMap sensedMap = new CoordinateMap(map.scale());
        sensedMap.enable(underTest.getSensed(), 'x');
        
        for (PlanRecord pr : underTest.getPlanRecords()) {
            assertFalse( "Plan record "+pr+" should not have been sensed", sensedMap.isEnabled(pr.coordinates()));
        }
        
        for (PlanRecord pr : underTest.getPath()) {
            assertFalse( "Path record "+pr+" should not have been enabled", sensedMap.isEnabled(pr.coordinates()));
        }
        return sensedMap;
    }
    
    @Test
    public void stepTest() {        
        for (int x=0;x<=13;x++) {
            for (int y=0;y<=15;y++) {
                Coordinates target = (x == 0 && y==0) ? Coordinates.fromXY(13, 15) : 
                    Coordinates.fromXY(0, 0);
                Position position = new Position( Coordinates.fromXY(x, y), 0);
                if (!map.isEnabled(position.coordinates())) {
                    underTest = new Planner(sensor, target);
                    Optional<Coordinates> result = underTest.step(position);
                    CoordinateMap sensedMap = verifyState();
                    assertTrue(result.isPresent());
                    assertFalse(sensedMap.isEnabled(result.get()));
                }
            }
        }
    }
}
