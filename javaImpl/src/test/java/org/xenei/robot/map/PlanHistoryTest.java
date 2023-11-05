package org.xenei.robot.map;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.xenei.robot.navigation.Coordinates;

public class PlanHistoryTest {
    @Test
    public void x() {
        ActiveMap map = new ActiveMap(10);
        map.enable(Coordinates.fromXY(240,160));
        Coordinates target = Coordinates.fromXY(0,0);
        Coordinates candidate = Coordinates.fromXY(240,160);
        PlanHistory history = new PlanHistory(new PlanRecord(candidate, candidate.distanceTo(target)));
        
        candidate = Coordinates.fromXY(240,120);
        map.enable(candidate);
        PlanRecord rec = new PlanRecord(candidate,candidate.distanceTo(target));
        history.add(rec);
        
        candidate = Coordinates.fromXY(210,160);
        map.enable(candidate);
        rec = new PlanRecord(candidate,candidate.distanceTo(target));
        history.add(rec);
        
        PlanRecord working1 = history.pop();
        assertEquals(Coordinates.fromXY(210,160), working1.position());
        
        history.newPlateau();
        candidate = Coordinates.fromXY(150,100);
        map.enable(candidate);
        rec = new PlanRecord(candidate,candidate.distanceTo(target));
        history.add(rec);
        
        candidate = Coordinates.fromXY(210,90);
        map.enable(candidate);
        rec = new PlanRecord(candidate,candidate.distanceTo(target));
        history.add(rec);
        
        PlanRecord working2 = history.pop();
        assertEquals( Coordinates.fromXY(150,100), working2.position());
        working2.setImpossible();
        
        working2 = history.pop();
        assertEquals( Coordinates.fromXY(210,90), working2.position());
        working2.setImpossible();
        
        working2 = history.pop();
        assertEquals( working1.position(), working2.position());
        working1.setImpossible();
        
        working1 = history.pop();
        assertEquals( working1.position(), Coordinates.fromXY(240,120));
        working1.setImpossible();
        
        working1 = history.pop();
        assertEquals( working1.position(), Coordinates.fromXY(240,160));
        working1.setImpossible();
        
        assertNull(history.pop());
    }
}
