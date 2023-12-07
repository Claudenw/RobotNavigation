package org.xenei.robot.common.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.xenei.robot.common.FrontsCoordinate;
import org.xenei.robot.common.Location;
import org.xenei.robot.mapper.GraphGeomFactory;

public class GeometryUtils {

    private static GeometryFactory geometryFactory = new GeometryFactory();

    private GeometryUtils() {}

    public static Polygon asPolygon(Coordinate coord, double radius) {
        double angle = 0.0;
        double radians = Math.toRadians(60);
        Coordinate[] cell = new Coordinate[7];
        Location l = new Location(coord);
        for (int i = 0; i < 7; i++) {
            cell[i] = l.plus(CoordUtils.fromAngle(angle, radius)).getCoordinate();
            angle += radians;
        }
        cell[6] = cell[0];
        return geometryFactory.createPolygon(cell);
    }

    public static Polygon asPolygon(FrontsCoordinate coord, double radius) {
        return asPolygon(coord.getCoordinate(), radius);
    }

    private static Collection<Coordinate> asCollection(FrontsCoordinate... coord) {
        return Arrays.stream(coord).map(FrontsCoordinate::getCoordinate).collect(Collectors.toList());
    }
    
    public static Polygon asPolygon(FrontsCoordinate... coord) {
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
        double r = width/2;
        Coordinate cUp = CoordUtils.fromAngle(angle+AngleUtils.RADIANS_90, r);
        Coordinate cDown = CoordUtils.fromAngle(angle-AngleUtils.RADIANS_90, r);
        return new Coordinate[] { CoordUtils.add(a, cUp),
                CoordUtils.add(b, cUp),
                CoordUtils.add(a, cDown),
                CoordUtils.add(b, cDown) };
    }
    public static Polygon asPath(Coordinate... points) {
        int limit = points.length-1;
        Stack<Coordinate> down = new Stack<>();
        List<Coordinate> all = new ArrayList<>();
        all.add(points[0]);
        down.push(points[0]);
        for (int i=0;i<limit;i++) {
            Coordinate[] segment = pathSegment(.5, points[i], points[i+1]);
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
    public static Polygon asPath(FrontsCoordinate... points) {
        return asPath(asCollection(points));
    }
    
    public static Point asPoint(Coordinate c) {
        return geometryFactory.createPoint(c);
    }

    public static Point asPoint(FrontsCoordinate c) {
        return asPoint(c.getCoordinate());
    }
}
