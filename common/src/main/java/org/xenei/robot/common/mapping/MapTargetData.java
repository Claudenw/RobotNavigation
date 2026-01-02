package org.xenei.robot.common.mapping;

import org.xenei.robot.common.planning.Segment;

public class MapTargetData {
    ///  change this to Location
    private final MapLocation[] locations;
    private final double distance;
    private final boolean indirect;

    MapTargetData(MapLocation first, MapLocation second) {
        locations = new MapLocation[] {first, second };
        int comp = first.compareTo(second);

        if (comp == 0) {
            indirect = false;
            distance = 0.0;
        } else {
            if (comp > 0) {
                locations[0] = second;
                locations[1] = first;
            }
            distance = first.distance(second);
            indirect = !first.getMap().isClearPath(first, second);
        }
    }

    public final MapLocation getTarget(MapLocation from) {
        return locations[0].equals(from) ? locations[1] : locations[0];
    }

    public final double distance() { return distance; }

    public final boolean indirect() { return indirect; }

    public final Segment asSegment(MapLocation from) {
        return new Segment(getTarget(from), indirect ? 2 * distance : distance, distance);
    }

}
