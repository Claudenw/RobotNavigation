package org.xenei.robot.map;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.Stack;

import org.xenei.robot.navigation.Coordinates;

public class PlanHistory {
    Stack<Plateau> plateaus = new Stack<>();
    
    PlanHistory(PlanRecord initial) {
        newPlateau();
        add(initial);
        newPlateau();
    }
    
    public void add(PlanRecord record) {
        plateaus.peek().add(record);
    }

    PlanRecord pop() {
        while (!plateaus.isEmpty()) {
            PlanRecord result = plateaus.peek().pop();
            if (result != null) {
                return result;
            } 
            plateaus.pop();
        }
        return null;
    }
    
    void newPlateau() {
        plateaus.push( new Plateau() );
    }

    class Plateau {
        Set<PlanRecord> positions = new HashSet<PlanRecord>();
    
        public boolean isEmpty() {
            return positions.isEmpty();
        }
        public void add(PlanRecord pr) {
            positions.add(pr);
        }
        
        public void remove(PlanRecord pr) {
            positions.remove(pr);
        }
        
        public PlanRecord pop() {
            // find lowest cost
            Optional<PlanRecord> result =  positions.stream().max((x,y) -> Double.compare( y.cost(), x.cost()));
            if (result.isPresent()) {
                PlanRecord rec = result.get();
                return Double.isInfinite(rec.cost()) ? null : rec;
            }
            return null;
        }
    }
}
