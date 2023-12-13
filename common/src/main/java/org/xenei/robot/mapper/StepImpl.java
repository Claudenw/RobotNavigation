package org.xenei.robot.mapper;

import java.util.Objects;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.xenei.robot.common.AbstractFrontsCoordinate;
import org.xenei.robot.common.FrontsCoordinate;
import org.xenei.robot.common.planning.Step;
import org.xenei.robot.common.utils.CoordUtils;

public class StepImpl extends AbstractFrontsCoordinate implements Step {
    private final double cost;
    private final Geometry geom;

    public StepImpl(Coordinate position, Coordinate target) {
        this(position, position.distance(target));
    }
    
    public StepImpl(Coordinate position, Coordinate target, Geometry geom) {
        this(position, position.distance(target), geom);
    }
    public StepImpl(FrontsCoordinate position, Coordinate target) {
        this(position.getCoordinate(), position.distance(target));
    }

    public StepImpl(FrontsCoordinate position, Coordinate target, Geometry geom) {
        this(position.getCoordinate(), position.distance(target), geom);
    }

    public StepImpl(Coordinate position, FrontsCoordinate target) {
        this(position, position.distance(target.getCoordinate()));
    }
    
    public StepImpl(Coordinate position, FrontsCoordinate target, Geometry geom) {
        this(position, position.distance(target.getCoordinate()), geom);
    }
    
    public StepImpl(FrontsCoordinate position, FrontsCoordinate target) {
        this(position.getCoordinate(), position.distance(target));
    }
    
    public StepImpl(FrontsCoordinate position, FrontsCoordinate target, Geometry geom) {
        this(position.getCoordinate(), position.distance(target), geom);
    }

    public StepImpl(FrontsCoordinate point, double cost) {
        this(point.getCoordinate(), cost);
    }
    
    public StepImpl(FrontsCoordinate point, double cost, Geometry geom) {
        this(point.getCoordinate(), cost, geom);
    }
    
    public StepImpl(Coordinate point, double cost, Geometry geom) {
        super(point);
        Objects.requireNonNull(geom);
        this.cost = cost;
        this.geom = geom;
    }
  
    public StepImpl(Coordinate point, double cost) {
        this(point, cost, new GeometryFactory().createPoint(point));
    }

    @Override
    protected StepImpl fromCoordinate(Coordinate base) {
        return new StepImpl(base, 0);
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
        if (obj instanceof StepImpl) {
            return compareTo((StepImpl)obj) == 0;
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        return 37 * getCoordinate().hashCode() + Coordinate.hashCode(cost);
    }

    @Override
    public int compareTo(Step other) {
        return Step.compare.compare(this, other);
    }

    @Override
    public Step copy() {
        return new StepImpl(getCoordinate(), cost, geom);
    }
}
