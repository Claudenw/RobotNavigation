package org.xenei.robot.common.planning;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.xenei.robot.common.AbstractFrontsCoordinate;
import org.xenei.robot.common.FrontsCoordinate;
import org.xenei.robot.common.utils.CoordUtils;

public class Step extends AbstractFrontsCoordinate<Step> implements Comparable<Step> {
    private final double cost;
    private final Geometry geom;

    public Step(Coordinate position, Coordinate target, Geometry geom) {
        this(position, position.distance(target), geom);
    }
    
    public Step(FrontsCoordinate position, Coordinate target, Geometry geom) {
        this(position.getCoordinate(), position.distance(target), geom);
    }

    public Step(Coordinate position, FrontsCoordinate target, Geometry geom) {
        this(position, position.distance(target.getCoordinate()), geom);
    }
    
    public Step(FrontsCoordinate position, FrontsCoordinate target, Geometry geom) {
        this(position.getCoordinate(), position.distance(target), geom);
    }

    
    public Step(FrontsCoordinate point, double cost, Geometry geom) {
        this(point.getCoordinate(), cost, geom);
    }
    
    public Step(Coordinate point, double cost, Geometry geom) {
        super(point);
        this.cost = cost;
        this.geom = geom == null? new GeometryFactory().createPoint(point) : geom;
    }
  

    @Override
    protected Step fromCoordinate(Coordinate base) {
        return new Step(base, 0, null);
    }

    public double cost() {
        return cost;
    }
    
    public Geometry getGeometry() {
        return geom;
    }

    @Override
    public String toString() {
        return String.format("Target {%s cost:%.4f}", CoordUtils.toString(this, 1), cost);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof Step) {
            return compareTo((Step)obj) == 0;
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        return 37 * getCoordinate().hashCode() + Coordinate.hashCode(cost);
    }

    @Override
    public int compareTo(Step other) {
        int x = Double.compare(cost, other.cost);
        return x == 0 ? CoordUtils.XYCompr.compare(this.getCoordinate(), other.getCoordinate()) : x;
    }

    @Override
    public Step copy() {
        return new Step(getCoordinate(), cost, geom);
    }
}
