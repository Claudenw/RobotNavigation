package org.xenei.robot.common.planning;

import java.util.Comparator;

import org.locationtech.jts.geom.Geometry;
import org.xenei.robot.common.FrontsCoordinate;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.utils.CoordUtils;

/**
 * An implementation of {@link FrontsCoordinate} that identifies an open path segment
 * from the current position to Segment coordinate.
 */
public interface Segment extends FrontsCoordinate, Comparable<Segment> {

    /**
     * The default comparator for Steps
     */
    Comparator<Segment> compare = (one, two) -> {
        int x = Double.compare(one.cost(), two.cost());
        return x == 0 ? CoordUtils.XYCompr.compare(one.getCoordinate(), two.getCoordinate()) : x;
    };

    /**
     * The cost of this step.
     * @return the cost of this step.
     */
    double cost();

    /**
     * The distance from this coordinate to the target.
     * @return the distance for this step
     */
    double distance();

    /**
     * The geometry of this step.
     * @return the gemetry associated with this step.
     */
    Geometry getGeometry();
    
    default Position nextPosition(Position currentPosition) {
        double heading = currentPosition.headingTo(this);
        return Position.from(getCoordinate(), heading);
    }

}
