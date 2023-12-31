package org.xenei.robot.mapper;

import java.util.Objects;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.xenei.robot.common.FrontsCoordinate;
import org.xenei.robot.common.UnmodifiableCoordinate;
import org.xenei.robot.common.planning.Step;
import org.xenei.robot.common.utils.CoordUtils;
import org.xenei.robot.common.utils.GeometryUtils;

public class StepImpl implements Step {
    private final UnmodifiableCoordinate coord;
    private final double cost;
    private final Geometry geom;
    private final double distance;
    
    public static class Builder {
        private final static  double UNSET = -1;
        private UnmodifiableCoordinate coord;
        private double cost = UNSET;
        private Geometry geom;
        private double distance = UNSET;
        
        public Builder setCoordinate(Coordinate coord) {
            this.coord = UnmodifiableCoordinate.make(coord);
            return this;
        }
        
        public Builder setCoordinate(FrontsCoordinate coord) {
            this.coord = coord.getCoordinate();
            return this;
        }
        
        public Builder setCost(double cost) {
            this.cost = cost;
            return this;
        }
        
        public Builder setDistance(double cost) {
            this.distance = cost;
            return this;
        }
        
        public Builder setGeometry(Geometry geom) {
            this.geom = geom;
            return this;
        }

        @Override
        public String toString() {
            return String.format("StepBuilder {%s cost:%.4f}", CoordUtils.toString(coord, 3), cost);
        }
        
        public boolean isValid() {
            return isValid(false);
        }
        
        public boolean isValid(boolean throwExceptions) {
            if (coord == null) {
                if (throwExceptions) {
            Objects.requireNonNull(coord, "Coordinates may not be null");
                }
                return false;
            }
//            if (cost <= UNSET) {
//                cost = distance;
//            }
//            if (distance <= UNSET) {
//                distance = cost;
//            }
//            if (cost <= UNSET) {
//                if (throwExceptions) {
//                throw new RuntimeException( "'cost' or 'distance' must be set");
//                } 
//                return false;
//            }
            if (cost <= 0) {
                if (throwExceptions) {
                throw new RuntimeException("'cost' must be greater than 0");
                }
                return false;
            }
            if (distance <= 0) {
                if (throwExceptions) {
                throw new RuntimeException("'distance' must be greater than 0");
                }
                return false;
            }
            if (geom == null) {
                geom = GeometryUtils.asPoint(coord);
            }
            return true;
        }
        
        public Step build() {
            isValid(true);
            return new StepImpl(coord, cost, distance, geom);
        }
    }

    static Builder builder() {
         return new Builder();
    }

    private StepImpl(UnmodifiableCoordinate coord, double cost, double distance, Geometry geom) {
        this.coord = coord;
        this.cost = cost;
        this.geom = geom;
        this.distance = distance;
    }

    @Override
    public UnmodifiableCoordinate getCoordinate() {
        return coord;
    }

    @Override
    public double cost() {
        return cost;
    }
    
    public double distance() {
        return distance;
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
