package org.xenei.robot.common.planning;

import java.util.Comparator;

import org.locationtech.jts.geom.Geometry;
import org.xenei.robot.common.GeometricObject;
import org.xenei.robot.common.mapping.MapCoordinate;
import org.xenei.robot.common.mapping.MapLocation;

/**
 * An implementation of {@link MapCoordinate} that identifies an open path
 * segment from the current position to Segment coordinate.
 * @param nextLocation the Location after this step.
 * @param cost  the cost of this step.
 * @param distance The distance from this coordinate to the target.
 */
public record Segment(MapLocation nextLocation, double cost,
                      double distance) implements Comparable<Segment>, GeometricObject {
    /**
     * The default comparator for Segments
     */
    public static final Comparator<Segment> COMPARATOR = Comparator.comparing(Segment::nextLocation).thenComparing(Segment::distance)
            .thenComparing(Segment::cost);


    @Override
    public int compareTo(Segment o) {
        return COMPARATOR.compare(this, o);
    }

    @Override
    public Geometry getGeometry() {
        return nextLocation.getGeometry();
    }
}
