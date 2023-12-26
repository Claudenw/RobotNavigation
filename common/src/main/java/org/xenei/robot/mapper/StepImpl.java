package org.xenei.robot.mapper;

import java.util.Objects;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.xenei.robot.common.FrontsCoordinate;
import org.xenei.robot.common.UnmodifiableCoordinate;
import org.xenei.robot.common.planning.Step;
import org.xenei.robot.common.utils.CoordUtils;

public class StepImpl implements Step {
    private final UnmodifiableCoordinate coord;
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
        coord = UnmodifiableCoordinate.make(point);
        Objects.requireNonNull(geom);
        this.cost = cost;
        this.geom = geom;
    }

    public StepImpl(Coordinate point, double cost) {
        this(point, cost, new GeometryFactory().createPoint(point));
    }

    @Override
    public UnmodifiableCoordinate getCoordinate() {
        return coord;
    }

    @Override
    public double cost() {
        return cost;
    }

    @Override
    public Geometry getGeometry() {
        return geom;
    }

    @Override
    public String toString() {
        return String.format("Step {%s cost:%.4f}", CoordUtils.toString(this, 3), cost);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof StepImpl) {
            return compareTo((StepImpl) obj) == 0;
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
}
