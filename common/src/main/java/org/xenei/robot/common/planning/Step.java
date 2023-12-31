package org.xenei.robot.common.planning;

import java.util.Comparator;
import java.util.Objects;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.xenei.robot.common.FrontsCoordinate;
import org.xenei.robot.common.UnmodifiableCoordinate;
import org.xenei.robot.common.utils.CoordUtils;
import org.xenei.robot.common.utils.GeometryUtils;
import org.xenei.robot.mapper.StepImpl;
import org.xenei.robot.mapper.StepImpl.Builder;

public interface Step extends FrontsCoordinate, Comparable<Step> {

    public static Comparator<Step> compare = (one, two) -> {
        int x = Double.compare(one.cost(), two.cost());
        return x == 0 ? CoordUtils.XYCompr.compare(one.getCoordinate(), two.getCoordinate()) : x;
    };
    

    public double cost();
    
    public double distance();

    public Geometry getGeometry();

}
