package org.xenei.robot.common.planning;

import java.util.Objects;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.xenei.robot.common.AbstractFrontsCoordinate;
import org.xenei.robot.common.FrontsCoordinate;
import org.xenei.robot.common.utils.CoordUtils;

public class Step extends AbstractFrontsCoordinate<Step> implements Comparable<Step> {
    private final double cost;
    private final Geometry geom;

    public Step(Coordinate position, Coordinate target) {
        this(position, position.distance(target));
    }
    
    public Step(Coordinate position, Coordinate target, Geometry geom) {
        this(position, position.distance(target), geom);
    }
    public Step(FrontsCoordinate position, Coordinate target) {
        this(position.getCoordinate(), position.distance(target));
    }

    public Step(FrontsCoordinate position, Coordinate target, Geometry geom) {
        this(position.getCoordinate(), position.distance(target), geom);
    }

    public Step(Coordinate position, FrontsCoordinate target) {
        this(position, position.distance(target.getCoordinate()));
    }
    
    public Step(Coordinate position, FrontsCoordinate target, Geometry geom) {
        this(position, position.distance(target.getCoordinate()), geom);
    }
    
    public Step(FrontsCoordinate position, FrontsCoordinate target) {
        this(position.getCoordinate(), position.distance(target));
    }
    
    public Step(FrontsCoordinate position, FrontsCoordinate target, Geometry geom) {
        this(position.getCoordinate(), position.distance(target), geom);
    }

    public Step(FrontsCoordinate point, double cost) {
        this(point.getCoordinate(), cost);
    }
    
    public Step(FrontsCoordinate point, double cost, Geometry geom) {
        this(point.getCoordinate(), cost, geom);
    }
    
    public Step(Coordinate point, double cost, Geometry geom) {
        super(point);
        Objects.requireNonNull(geom);
        this.cost = cost;
        this.geom = geom;
    }
  
    public Step(Coordinate point, double cost) {
        this(point, cost, new GeometryFactory().createPoint(point));
    }

    @Override
    protected Step fromCoordinate(Coordinate base) {
        return new Step(base, 0);
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
