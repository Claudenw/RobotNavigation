package org.xenei.robot.planner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xenei.robot.navigation.Coordinates;
import org.xenei.robot.testUtils.MapLibrary;

public class SolutionTest {

    private Solution underTest;
    
    @BeforeEach
    public void setup() {
        underTest = new Solution();
        underTest.add(  Coordinates.fromXY(-1, -3));
        underTest.add( Coordinates.fromXY(-1, -2));
        underTest.add( Coordinates.fromXY(-2, -2));
        underTest.add( Coordinates.fromXY(-2, -2));
        underTest.add( Coordinates.fromXY(0, -2));
        underTest.add( Coordinates.fromXY(0, -2));
        underTest.add( Coordinates.fromXY(2, -1));
        underTest.add( Coordinates.fromXY(-1, 1));
    }
    
    @Test
    public void testRetrieval() {
        assertTrue(new Solution().isEmpty());
        assertFalse(underTest.isEmpty());
        assertEquals( Coordinates.fromXY(-1, 1), underTest.end() );
        assertEquals( Coordinates.fromXY(-1, -3), underTest.start() );
        assertEquals(5, underTest.stepCount());
    }
    
    @Test
    public void simplifyTest() {
        PlannerMap map = new PlannerMap();
        MapLibrary.map2('#').populate(map);
        underTest.simplify(map);
        assertEquals( 3, underTest.stepCount());
        assertEquals( Coordinates.fromXY(-1, 1), underTest.end() );
        assertEquals( Coordinates.fromXY(-1, -3), underTest.start() );
    }
    
}
