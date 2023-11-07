package org.xenei.robot.planner;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.navigation.Coordinates;

public class PlannerMap {
    private static final Logger LOG = LoggerFactory.getLogger(PlannerMap.class);
    private Set<PlanRecord> points;
    private Set<Coordinates> complete;

    PlannerMap() {
        points = new HashSet<>();
        complete = new HashSet<>();
    }

    public boolean isEmpty() {
        return points.isEmpty();
    }

    public void update(Coordinates target) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Starting Update to target: {}", target);
        }
        points = points.stream().map(rec -> new PlanRecord(rec.position(), rec.position().distanceTo(target)))
                .collect(Collectors.toSet());
        LOG.debug("Update complete");
    }

    public boolean add(PlanRecord record) {
        if (!complete.contains(record.position())) {
            complete.add(record.position());
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Adding: {}", record);
        }
        return points.add(record);
    }
    
    public void remove(Coordinates coord) {
        points.remove( new PlanRecord( coord, 0.0 ));
    }

    public Optional<PlanRecord> getBest(Coordinates position) {
        if (points.isEmpty()) {
            LOG.debug("No map points");
            return Optional.empty();
        }
        Optional<PlanRecord> result;
        if (points.size() == 1) {
            PlanRecord rec = points.iterator().next();
            LOG.debug("getBest() -> {}", rec );
            result = Optional.of(rec);
        }

        Comparator<Pair<PlanRecord,Double>> comp = (p1, p2) -> p1.getRight().compareTo(p2.getRight());
        Function<PlanRecord,Pair<PlanRecord,Double>> mapper = pr -> Pair.of( pr, calcBestCost(pr, position));
        Predicate<PlanRecord> fltr = p -> !p.position().equals(position);
        
        if (LOG.isDebugEnabled()) {
            List<Pair<PlanRecord,Double>> l = points.stream().filter(fltr).map(mapper).sorted(comp).collect(Collectors.toList());
            l.forEach(p -> LOG.debug( "cost:{} {}", p.getRight(), p.getLeft() ));
            result = Optional.of(l.get(0).getLeft());
        } else {
            result = points.stream().filter(fltr).map(mapper).min(comp).map(Pair::getLeft);
        }
        
        if (LOG.isDebugEnabled()) {
            LOG.debug("getBest() -> {}", result.isEmpty() ? "null" : result.get());
        }
        
        return result;
    }

    private double calcBestCost(PlanRecord rec, Coordinates position) {
        return rec.cost() + position.distanceTo(rec.position());
    }
    
    public PlanRecord getPlanRecord(Coordinates position) {
        for (PlanRecord p : points) {
            if (p.position().equals(position)) {
                return p;
            }
        }
        return null;
    }
    
    public Collection<Coordinates> getPlanRecords() {
        return points.stream().map( PlanRecord::position ).collect(Collectors.toList());
    }
}
