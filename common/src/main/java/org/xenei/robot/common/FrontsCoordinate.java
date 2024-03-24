package org.xenei.robot.common;

import java.util.Comparator;

import org.locationtech.jts.geom.Coordinate;
import org.xenei.robot.common.utils.CoordUtils;

public interface FrontsCoordinate {
    
    /**
     * Returns true if the location represents a point of infnite distance.
     * @param coord the Location to check.
     * @return true if the location is not finite, false otherwise.
     */
    static boolean isInfinite(FrontsCoordinate loc) {
        return CoordUtils.isInfinite(loc.getCoordinate());
    }


    public static final FrontsCoordinate ORIGIN = new FrontsCoordinate() {
        UnmodifiableCoordinate zero = UnmodifiableCoordinate.make(new Coordinate(0,0));
        @Override
        public UnmodifiableCoordinate getCoordinate() {
            return zero; 
        }};
        
        
    /**
     * A representative infinite value.  Testing for infinite values should be done
     * using the isInfinite() method.
     * @see #isInfinite()
     */
    public static FrontsCoordinate INFINITE = new FrontsCoordinate() {
        UnmodifiableCoordinate infinity = UnmodifiableCoordinate.make(
                new Coordinate(Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY));
        @Override
        public UnmodifiableCoordinate getCoordinate() {
            return infinity; 
        }};
         

    /**
     * Compares Coordinates by XY positions.
     * 
     * @see CoordUtils#XYCompr
     */
    static Comparator<FrontsCoordinate> XYCompr = (one, two) -> {
        return CoordUtils.XYCompr.compare(one.getCoordinate(), two.getCoordinate());
    };

    UnmodifiableCoordinate getCoordinate();

    default double getX() {
        return getCoordinate().getX();
    }

    default double getY() {
        return getCoordinate().getY();
    }

    default boolean equals2D(Coordinate other) {
        return getCoordinate().equals2D(other);
    }

    default boolean equals2D(FrontsCoordinate other) {
        return equals2D(other.getCoordinate());
    }

    default boolean equals2D(Coordinate c, double tolerance) {
        return getCoordinate().equals2D(c, tolerance);
    }

    default boolean equals2D(FrontsCoordinate c, double tolerance) {
        return equals2D(c.getCoordinate(), tolerance);
    }

    default int compareTo(Coordinate o) {
        return getCoordinate().compareTo(o);
    }

    default int compareTo(FrontsCoordinate o) {
        return compareTo(o.getCoordinate());
    }

    default double distance(Coordinate c) {
        return getCoordinate().distance(c);
    }

    default double distance(FrontsCoordinate c) {
        return distance(c.getCoordinate());
    }

    default double angleBetween(FrontsCoordinate other) {
        return angleBetween(other.getCoordinate());
    }

    default double angleBetween(Coordinate dest) {
        return CoordUtils.angleBetween(this.getCoordinate(), dest);
    }

    default Coordinate minus(Coordinate other) {
        return new Coordinate(getX() - other.getX(), getY() - other.getY());
    }

    default Coordinate minus(FrontsCoordinate other) {
        return new Coordinate(getX() - other.getX(), getY() - other.getY());
    }

    default Coordinate plus(Coordinate other) {
        return new Coordinate(getX() + other.getX(), getY() + other.getY());
    }

    default Coordinate plus(FrontsCoordinate other) {
        return new Coordinate(getX() + other.getX(), getY() + other.getY());
    }
    
    default boolean near(Coordinate other, double tolerance) {
        return this.distance(other) <= tolerance;
    }
    
    default boolean near(FrontsCoordinate other, double tolerance) {
        return near(other.getCoordinate(),tolerance);
    }
    
    
    default boolean isInfinite() {
        return isInfinite(this);
    }
}
