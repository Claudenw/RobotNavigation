package org.xenei.robot.common;

import java.util.Comparator;

import org.locationtech.jts.geom.Coordinate;
import org.xenei.robot.common.utils.CoordUtils;

public interface FrontsCoordinate {
    
    /**
     * Compares Coordinates by XY positions.
     * @see CoordUtils#XYCompr
     */
    static Comparator<FrontsCoordinate> XYCompr = (one, two) -> {
        return CoordUtils.XYCompr.compare(one.getCoordinate(), two.getCoordinate());
    };

    UnmodifiableCoordinate getCoordinate();
    
    double getX();

    double getY();

    double getZ();

    double getM();

    double getOrdinate(int ordinateIndex);

    boolean equals2D(Coordinate other);
    
    boolean equals2D(FrontsCoordinate other);

    boolean equals2D(Coordinate c, double tolerance);

    boolean equals2D(FrontsCoordinate c, double tolerance);

    boolean equals3D(Coordinate other) ;

    boolean equals3D(FrontsCoordinate other) ;

    boolean equalInZ(Coordinate c, double tolerance);

    boolean equalInZ(FrontsCoordinate c, double tolerance);

    int compareTo(Coordinate o);

    int compareTo(FrontsCoordinate o);
    
    FrontsCoordinate copy();

    double distance(Coordinate c);

    double distance(FrontsCoordinate c);

    double distance3D(FrontsCoordinate c);
    
    double distance3D(Coordinate c);

}
