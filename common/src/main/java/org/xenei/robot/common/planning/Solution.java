package org.xenei.robot.common.planning;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.stream.Stream;

import org.locationtech.jts.geom.Coordinate;
import org.xenei.robot.common.Location;

public class Solution {

    private final List<SolutionRecord> path;

    public Solution() {
        path = new ArrayList<>();
    }

    public Coordinate end() {
        return get(path.size() - 1);
    }

    private Coordinate get(int idx) {
        return path.size() > 0 ? path.get(idx).coord : null;
    }

    public boolean isEmpty() {
        return path.isEmpty();
    }

    public void add(Location c) {
        SolutionRecord sr = new SolutionRecord(c.getCoordinate());
        if (!path.contains(sr)) {
            path.add(sr);
        }
    }

    public int stepCount() {
        return path.size() - 1;
    }

    public double cost() {
        if (isEmpty()) {
            return Double.POSITIVE_INFINITY;
        }
        Coordinate target = end();
        return recalculateCost(target);
    }

    public Coordinate start() {
        return get(0);
    }

    public Stream<Coordinate> stream() {
        return path.stream().map(s -> s.coord);
    }

    private double recalculateCost(Coordinate target) {
        int limit = stepCount();
        SolutionRecord oldPr;
        double accumulator = 0.0;
        SolutionRecord newPr = path.get(limit);
        path.set(limit, newPr);
        for (int i = limit - 1; i >= 0; i--) {
            oldPr = path.get(i);
            accumulator += oldPr.coord.distance(newPr.coord);
            newPr = new SolutionRecord(oldPr.coord, accumulator);
            path.set(i, newPr);
        }
        return accumulator;
    }

    private void removeUnnecessarySteps(BiPredicate<Coordinate, Coordinate> clearCheck) {
        List<SolutionRecord> result = new ArrayList<>();
        result.add(path.get(0));
        int idx = 0;
        int limit = stepCount();
        while (idx < limit) {
            SolutionRecord current = path.get(idx);
            double minCost = current.cost;
            int nextIdx = limit;
            for (int scan = idx + 1; scan < limit; scan++) {
                SolutionRecord scanning = path.get(scan);
                if (clearCheck.test(current.coord, scanning.coord)) {
                    if (scanning.cost < minCost) {
                        minCost = scanning.cost;
                        nextIdx = scan;
                    }
                }
            }
            result.add(path.get(nextIdx));
            idx = nextIdx;
        }
        setPath(result);
    }

    private void setPath(List<SolutionRecord> lst) {
        path.clear();
        path.addAll(lst);
    }

    /**
     * Builds the shortest path based on the path stack and the target.
     * 
     * @param clearCheck a predicate that returns clear if the path between the two
     * coordinates is clear.
     */
    public void simplify(BiPredicate<Coordinate, Coordinate> clearCheck) {
        if (path.size() > 2) {
            recalculateCost(end());
            removeUnnecessarySteps(clearCheck);
        }
    }

    private class SolutionRecord {
        final Coordinate coord;
        final double cost;

        SolutionRecord(Coordinate p) {
            this(p, 0.0);
        }

        SolutionRecord(Coordinate p, double cost) {
            this.coord = p;
            this.cost = cost;
        }

        @Override
        public String toString() {
            return String.format("%s c:%.4f", coord, cost);
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof SolutionRecord) {
                return coord.equals(((SolutionRecord) o).coord);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return coord.hashCode();
        }
    }
}
