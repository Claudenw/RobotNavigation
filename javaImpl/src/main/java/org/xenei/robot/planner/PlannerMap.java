package org.xenei.robot.planner;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.navigation.Coordinates;

public class PlannerMap {
    private static final Logger LOG = LoggerFactory.getLogger(PlannerMap.class);
    private Set<PlanRecord> points;

    PlannerMap() {
        points = new HashSet<>();
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

    public void add(PlanRecord record) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Adding: {}", record);
        }
        points.add(record);
    }

    public Coordinates getBest(Coordinates position) {
        if (points.isEmpty()) {
            LOG.debug("No map points");
            return null;
        }
        if (points.size() == 1) {
            PlanRecord rec = points.iterator().next();
            LOG.debug("getBest() -> {}", rec );
            return rec.position();
        }

        Optional<Pair<PlanRecord, Double>> result = points.stream().map(c -> Pair.of(c, calcBestCost(c, position)))
                .min((p1, p2) -> p2.getRight().compareTo(p1.getRight()));

        if (LOG.isDebugEnabled()) {
            LOG.debug("getBest() -> {}", result.isEmpty() ? "null" : result.get().getLeft());
        }
        return result.isEmpty() ? null : result.get().getLeft().position();
    }

    private double calcBestCost(PlanRecord rec, Coordinates position) {
        return rec.cost() + position.distanceTo(rec.position());
    }
}
