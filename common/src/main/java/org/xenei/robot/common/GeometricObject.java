package org.xenei.robot.common;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;

public interface GeometricObject {
    Comparator<GeometricObject> comp = (x, y) -> x.getGeometry().compareTo(y.getGeometry());

    /**
     * Creates a geometric objeect from a geometry that has been scaled to the resolution.
     * @param geometry the geometry to use.
     * @return a GeometricObject
     */
    static GeometricObject of(Geometry geometry) {
        return new GeometricObject() {

            @Override
            public Geometry getGeometry() {
                return geometry;
            }

            @Override
            public int hashCode() {
                return geometry.hashCode();
            }

            @Override
            public boolean equals(Object obj) {
                return GeometricObject.equals(this, obj);
            }
        };
    }
    /**
     * Hashcode should be implemented as wkt().hashCode()
     *
     * @param obj
     *            the obstacle
     * @return the hashCode
     */
    static int hashCode(GeometricObject obj) {
        return obj.getGeometry().hashCode();
    }

    static boolean equals(GeometricObject obj1, Object obj2) {
        if (obj1 == obj2) {
            return true;
        }
        if (obj2 != null && GeometricObject.class.isAssignableFrom(obj2.getClass())) {
            return comp.compare(obj1, (GeometricObject) obj2) == 0;
        }
        return false;
    }

    /**
     * Gets the geometry associated with this location.
     *
     * @return the geometry for the object.
     */
    Geometry getGeometry();

    default Set<Point> getPoints() {
        GeometryFactory factory = getGeometry().getFactory();
        return Arrays.stream(getGeometry().getCoordinates()).map(factory::createPoint).collect(Collectors.toSet());
    }

}
