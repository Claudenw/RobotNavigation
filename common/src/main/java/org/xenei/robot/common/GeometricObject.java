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
     * Hashcode should be implemented as wkt().hashCode()
     *
     * @param o
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
