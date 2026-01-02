package org.xenei.robot.common.planning;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.xenei.robot.common.mapping.MapLocation;
import org.xenei.robot.common.mapping.MapTargetData;

public class Solution {

    private final List<MapLocation> path;

    public Solution() {
        path = new ArrayList<>();
    }

    public MapLocation end() {
        return get(path.size() - 1);
    }

    private MapLocation get(int idx) {
        return !path.isEmpty() ? path.get(idx) : null;
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
    public void add(MapLocation coordinate) {
        if (!path.contains(coordinate)) {
            path.add(coordinate);
        }
    }

    public int stepCount() {
        return path.size() - 1;
    }

    public double cost() {
        if (isEmpty()) {
            return Double.POSITIVE_INFINITY;
        }
        double cost = 0;
        MapLocation start = path.get(0);
        for (int i = 1; i <= stepCount(); i++) {
            MapLocation end = path.get(i);
            double dist = start.distance(end);
            System.out.format("%s to %s dist=%s%n", start, end, dist);
            cost += start.distance(end);
            start = end;
        }
        return cost;
    }

    public MapLocation start() {
        return get(0);
    }

    public Stream<MapLocation> stream() {
        return path.stream();
    }

    private void removeUnnecessarySteps() {
        List<MapLocation> result = new ArrayList<>();
        int[] targetData = new int[path.size()];

        for (int i=0; i < path.size() -1; i++) {
            for (int j = i+1; j < path.size(); j++) {
                MapTargetData data = path.get(i).getTargetData(path.get(j));
                if (!data.indirect()) {
                    targetData[i] = j;
                }
            }
        }
        result.add(path.get(0));
        int idx = 0;
        while (idx != targetData.length -1) {
            int nxt = targetData[idx];
            result.add(path.get(nxt));
            idx = nxt;
        }
        path.retainAll(result);
    }

    /**
     * Builds the shortest path based on the path stack and the target.
     */
    public void simplify() {
        if (path.size() > 2) {
            removeUnnecessarySteps();
        }
    }
}
