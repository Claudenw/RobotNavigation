package org.xenei.robot.planner;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.xenei.robot.common.Coordinates;

public class Solution {

    private final List<SolutionRecord> path;

    Solution() {
        path = new ArrayList<>();
    }

    public Coordinates end() {
        return get(path.size() - 1);
    }

    private Coordinates get(int idx) {
        return path.size() > 0 ? path.get(idx).coord : null;
    }

    public boolean isEmpty() {
        return path.isEmpty();
    }

    public void add(Coordinates c) {
        SolutionRecord sr = new SolutionRecord(c);
        if (!path.contains(sr)) {
            path.add(sr);
        }
    }

    public int stepCount() {
        return path.size() - 1;
    }

    public Coordinates start() {
        return get(0);
    }

    public Stream<Coordinates> stream() {
        return path.stream().map(s -> s.coord);
    }

    private void recalculateCost(Coordinates target) {
        int limit = stepCount();
        SolutionRecord oldPr;
        double accumulator = 0.0;
        SolutionRecord newPr = path.get(limit);
        path.set(limit, newPr);
        for (int i = limit - 1; i >= 0; i--) {
            oldPr = path.get(i);
            accumulator += oldPr.coord.distanceTo(newPr.coord);
            newPr = new SolutionRecord(oldPr.coord, accumulator);
            path.set(i, newPr);
        }
    }

    private void removeUnnecessarySteps(PlannerMap map) {
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
                if (map.clearView(current.coord, scanning.coord)) {
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
     * @param target
     */
    public void simplify(PlannerMap map) {
        if (path.size() > 2) {
            Coordinates target = end();
            recalculateCost(target);
            removeUnnecessarySteps(map);
        }
    }

    private class SolutionRecord {
        final Coordinates coord;
        final double cost;

        SolutionRecord(Coordinates p) {
            this(p, 0.0);
        }

        SolutionRecord(Coordinates p, double cost) {
            this.coord = p;
            this.cost = cost;
        }

        @Override
        public String toString() {
            return String.format("%s c:%.4f", coord.getPoint(), cost);
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
