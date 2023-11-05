package org.xenei.robot.map;

import static org.junit.Assert.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xenei.robot.navigation.Coordinates;

public class ActiveMapTest {
    
    private ActiveMap underTest;
    
    @BeforeEach
    public void setup() {
        underTest = new ActiveMap(10);
        for (int i=-ActiveMap.halfdim;i<ActiveMap.halfdim;i++) {
            underTest.enable(Coordinates.fromXY(i*underTest.scale(),i*underTest.scale()));
        }
    }
    
    @Test
    public void shiftUpTest() {
        Coordinates cmd = Coordinates.fromDegrees(0, 10);
        underTest.shift(cmd);
        assertEquals( 0, underTest.map[0]);
        for (int i=1; i<ActiveMap.dim; i++) {
            assertEquals( 1L<<(i-1), underTest.map[i]);
        }
    }

    @Test
    public void shiftDownTest() {
        assertEquals( 1L<<32, underTest.map[32]);
        Coordinates cmd = Coordinates.fromDegrees(180, 10);
        underTest.shift(cmd);
        assertEquals( 0, underTest.map[ActiveMap.dim-1]);
        for (int i=0; i<ActiveMap.dim-1; i++) {
            assertEquals( 1L<<(i+1), underTest.map[i]);
        }
    }
    
    @Test
    public void shiftLeftTest() {
        Coordinates cmd = Coordinates.fromDegrees(-90, 10);
        underTest.shift(cmd);
        assertEquals( 0, underTest.map[0]);
        for (int i=1; i<ActiveMap.dim-1; i++) {
            assertEquals( 1L<<(i-1), underTest.map[i]);
        }
    }
    
    @Test
    public void shiftRightTest() {
        Coordinates cmd = Coordinates.fromDegrees(0, 10);
        underTest.shift(cmd);
        assertEquals( 0, underTest.map[0]);
        for (int i=1; i<ActiveMap.dim; i++) {
            assertEquals( 1L<<(i-1), underTest.map[i]);
        }
    }
    
    @Test
    public void largeShiftUpTest() {
        Coordinates cmd = Coordinates.fromDegrees(0, 10*ActiveMap.dim);
        underTest.shift(cmd);
        for (int i=1; i<ActiveMap.dim; i++) {
            assertEquals( 0, underTest.map[i]);
        }
    }

    @Test
    public void largeShiftDownTest() {
        assertEquals( 1L<<32, underTest.map[32]);
        Coordinates cmd = Coordinates.fromDegrees(180, 10*ActiveMap.dim);
        underTest.shift(cmd);
        for (int i=1; i<ActiveMap.dim; i++) {
            assertEquals( 0, underTest.map[i]);
        }
    }
    
    @Test
    public void largeShiftLeftTest() {
        Coordinates cmd = Coordinates.fromDegrees(-90, 10*ActiveMap.dim);
        underTest.shift(cmd);
        for (int i=1; i<ActiveMap.dim; i++) {
            assertEquals( 0, underTest.map[i]);
        }
    }
    
    @Test
    public void largeShiftRightTest() {
        Coordinates cmd = Coordinates.fromDegrees(0, 10*ActiveMap.dim);
        underTest.shift(cmd);
        for (int i=1; i<ActiveMap.dim; i++) {
            assertEquals( 0, underTest.map[i]);
        }
    }

}
