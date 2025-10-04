package org.xenei.robot.mapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sorts a collection of points into line segments joining each point to its
 * closest neighbors.
 */
public class PointCloudSorter {
    private static final Logger LOG = LoggerFactory.getLogger(PointCloudSorter.class);

    final double resolution;

    public PointCloudSorter(Double resolution) {
        this.resolution = resolution;
    }

    public Geometry process(Set<Point> pointSet) {
        if (pointSet.isEmpty()) {
            throw new IllegalArgumentException("Points must not be empty");
        }
        Point firstPoint = pointSet.iterator().next();
        if (pointSet.size() == 1) {
            return firstPoint;
        }
        final GeometryFactory geometryFactory = pointSet.iterator().next().getFactory();
        /* matrix of flags for the shortest distances */
        final Point[] points = pointSet.toArray(new Point[0]);
        // populate the distance matrix and the shortest distance count
        List<LineString> lst = new ArrayList<>();
        for (int i = 0; i < points.length - 1; i++) {
            for (int j = i + 1; j < points.length; j++) {
                double d = points[i].distance(points[j]);
                if (d <= resolution) {
                    lst.add(geometryFactory
                            .createLineString(new Coordinate[]{points[i].getCoordinate(), points[j].getCoordinate()}));
                }
            }
        }
        Geometry result = geometryFactory.createMultiLineString(lst.toArray(new LineString[0]));
        result.normalize();
        return result;
    }
}
