package org.xenei.robot.common.planning;

import java.util.Comparator;

import org.locationtech.jts.geom.Geometry;
import org.xenei.robot.common.GeometricObject;
import org.xenei.robot.common.mapping.MapCoordinate;
import org.xenei.robot.common.mapping.MapLocation;

/**
 * An implementation of {@link MapCoordinate} that identifies an open path
 * segment from the current position to Segment coordinate.
 */
public final class Segment implements Comparable<Segment>, GeometricObject {

    private final MapLocation nextLocation;
    private final double cost;
    private final double distance;
    /**
     * The default comparator for Segments
     */
    public static final Comparator<Segment> COMPARATOR = Comparator.comparing(Segment::getNextLocation).thenComparing(Segment::distance)
            .thenComparing(Segment::cost);

    public Segment(MapLocation nextLocation, double cost, double distance) {
        this.nextLocation = nextLocation;
        this.cost = cost;
        this.distance = distance;
    }

    public MapLocation getNextLocation() {
        return nextLocation;
    }
    /**
     * The cost of this step.
     *
     * @return the cost of this step.
     */
    public double cost() {
        return cost;
    }

    /**
     * The distance from this coordinate to the target.
     *
     * @return the distance for this step
     */
    public double distance() {
        return distance;
    }

    @Override
    public int compareTo(Segment o) {
        return COMPARATOR.compare(this, o);
    }


    @Override
    public Geometry getGeometry() {
        return nextLocation.getGeometry();
    }
}
