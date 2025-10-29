package org.xenei.robot.common.planning;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.stream.Stream;

import org.xenei.robot.common.mapping.MapCoordinate;

public class Solution {

    private final List<SolutionRecord> path;

    public Solution() {
        path = new ArrayList<>();
    }

    public MapCoordinate end() {
        return get(path.size() - 1);
    }

    private MapCoordinate get(int idx) {
        return !path.isEmpty() ? path.get(idx).coord : null;
    }

    public boolean isEmpty() {
        return path.isEmpty();
    }

    /**
     * Add a map coordinate to the solution.
     *
     * @param coordinate
     *            the coordinate to add.
     */
    public void add(MapCoordinate coordinate) {
        SolutionRecord sr = new SolutionRecord(coordinate);
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
        return recalculateCost();
    }

    public MapCoordinate start() {
        return get(0);
    }

    public Stream<MapCoordinate> stream() {
        return path.stream().map(s -> s.coord);
    }

    /**
     * Walks the solution backwards and recalculates the cost for each segment in
     * the solution..
     *
     * @return the total cost of the solution.
     */
    private double recalculateCost() {
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

    private void removeUnnecessarySteps(BiPredicate<MapCoordinate, MapCoordinate> clearCheck) {
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
     * @param clearCheck
     *            a predicate that returns clear if the path between the two
     *            coordinates is clear.
     */
    public void simplify(BiPredicate<MapCoordinate, MapCoordinate> clearCheck) {
        if (path.size() > 2) {
            recalculateCost();
            removeUnnecessarySteps(clearCheck);
        }
    }

    private static class SolutionRecord {
        final MapCoordinate coord;
        final double cost;

        SolutionRecord(MapCoordinate p) {
            this(p, 0.0);
        }

        SolutionRecord(MapCoordinate p, double cost) {
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
