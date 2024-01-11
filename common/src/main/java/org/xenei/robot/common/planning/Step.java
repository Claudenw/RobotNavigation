package org.xenei.robot.common.planning;

import java.util.Comparator;

import org.locationtech.jts.geom.Geometry;
import org.xenei.robot.common.FrontsCoordinate;
import org.xenei.robot.common.utils.CoordUtils;

public interface Step extends FrontsCoordinate, Comparable<Step> {

    /**
     * The default comparator for Steps
     */
    public static Comparator<Step> compare = (one, two) -> {
        int x = Double.compare(one.cost(), two.cost());
        return x == 0 ? CoordUtils.XYCompr.compare(one.getCoordinate(), two.getCoordinate()) : x;
    };

    /**
     * The cost of this step.
     * @return the cost of this step.
     */
    public double cost();

    /**
     * The distance from this coordinate to the target.
     * @return
     */
    public double distance();

    /**
     * The geometry of this step.
     * @return
     */
    public Geometry getGeometry();

}
