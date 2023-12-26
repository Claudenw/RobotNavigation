package org.xenei.robot.common.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.xenei.robot.common.HasCoordinate;
import org.xenei.robot.common.Location;

public class GeometryUtils {

    private static GeometryFactory geometryFactory = new GeometryFactory();

    private GeometryUtils() {
    }

    public static Geometry asCluster(Collection<Coordinate> c) {
        return asCluster(c.toArray(new Coordinate[c.size()]));
    }

    public static Geometry asCluster(Coordinate[] c) {
        Geometry g = geometryFactory.createMultiPointFromCoords(c).convexHull();
        if (g instanceof LineString) {
            return asPath( g.getCoordinates() );
        }
        if (g instanceof Point) {
            return asPolygon(g.getCoordinate());
        }
        return g;
    }

    public static Polygon asPolygon(Coordinate coord, double radius) {
        return asPolygon(coord, radius, 6);
    }

    public static Polygon asPolygon(Coordinate coord, double radius, int edges) {
        double angle = 0.0;
        double radians = Math.PI * 2.0 / edges;
        Coordinate[] cell = new Coordinate[edges + 1];
        Location l = Location.from(coord);
        for (int i = 0; i < edges; i++) {
            cell[i] = l.plus(CoordUtils.fromAngle(angle, radius));
            angle += radians;
        }
        cell[edges] = cell[0];
        return geometryFactory.createPolygon(cell);
    }

    public static Polygon asPolygon(HasCoordinate coord, double radius) {
        return asPolygon(coord.getCoordinate(), radius);
    }

    public static Polygon asPolygon(HasCoordinate coord, double radius, int edges) {
        return asPolygon(coord.getCoordinate(), radius, edges);
    }

    public static Collection<Coordinate> asCollection(Collection<? extends HasCoordinate> coord) {
        return asCollection(coord.toArray(new HasCoordinate[coord.size()]));
    }

    public static Collection<Coordinate> asCollection(HasCoordinate... coord) {
        return Arrays.stream(coord).map(HasCoordinate::getCoordinate).collect(Collectors.toList());
    }

    public static Polygon asPolygon(HasCoordinate... coord) {
        return asPolygon(asCollection(coord));
    }

    public static Polygon asPolygon(Coordinate... coord) {
        return geometryFactory.createPolygon(coord);
    }

    public static Polygon asPolygon(Collection<Coordinate> coord) {
        return geometryFactory.createPolygon(coord.toArray(new Coordinate[coord.size()]));
    }

    public static Coordinate[] pathSegment(double width, Coordinate a, Coordinate b) {

        double angle = CoordUtils.angleBetween(a, b);
        double r = width / 2;
        Coordinate cUp = CoordUtils.fromAngle(angle + AngleUtils.RADIANS_90, r);
        Coordinate cDown = CoordUtils.fromAngle(angle - AngleUtils.RADIANS_90, r);
        return new Coordinate[] { CoordUtils.add(a, cUp), CoordUtils.add(b, cUp), CoordUtils.add(a, cDown),
                CoordUtils.add(b, cDown) };
    }

    public static Polygon asPath(Coordinate... points) {
        int limit = points.length - 1;
        Stack<Coordinate> down = new Stack<>();
        List<Coordinate> all = new ArrayList<>();
        all.add(points[0]);
        down.push(points[0]);
        for (int i = 0; i < limit; i++) {
            Coordinate[] segment = pathSegment(.5, points[i], points[i + 1]);
            all.add(segment[0]);
            all.add(segment[1]);
            down.push(segment[2]);
            down.push(segment[3]);
        }
        all.add(points[limit]);
        while (!down.empty()) {
            all.add(down.pop());
        }
        return asPolygon(all);
    }

    public static Polygon asPath(Collection<Coordinate> points) {
        return asPath(points.toArray(new Coordinate[points.size()]));
    }

    public static Polygon asPath(HasCoordinate... points) {
        return asPath(asCollection(points));
    }

    public static Point asPoint(Coordinate c) {
        return geometryFactory.createPoint(c);
    }

    public static Point asPoint(HasCoordinate c) {
        return asPoint(c.getCoordinate());
    }

    public static LineString asLine(Coordinate... coords) {
        return geometryFactory.createLineString(coords);
    }
}
