package org.xenei.robot.common.utils;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.operation.buffer.BufferOp;
import org.locationtech.jts.operation.buffer.BufferParameters;
import org.xenei.robot.common.FrontsCoordinate;
import org.xenei.robot.common.Location;

public class GeometryUtils {

    private final RobutContext ctxt;

    public GeometryUtils(RobutContext ctxt) {
        this.ctxt = ctxt;
    }

    public Polygon asPolygon(Coordinate coord, double radius) {
        return asPolygon(coord, radius, 6);
    }

    public Polygon asPolygon(Coordinate coord, double radius, int edges) {
        double angle = edges == 4 ? AngleUtils.RADIANS_45 : 0.0;
        if (edges == 4) {
            radius *= DoubleUtils.SQRT2;
        }
        double radians = Math.PI * 2.0 / edges;
        Coordinate[] cell = new Coordinate[edges + 1];
        Location l = Location.from(coord);
        for (int i = 0; i < edges; i++) {
            cell[i] = l.plus(CoordUtils.fromAngle(angle, radius));
            angle += radians;
        }
        cell[edges] = cell[0];
        return ctxt.geometryFactory.createPolygon(cell);
    }

    public Polygon asPolygon(FrontsCoordinate coord, double radius) {
        return asPolygon(coord.getCoordinate(), radius);
    }

    public Polygon asPolygon(FrontsCoordinate coord, double radius, int edges) {
        return asPolygon(coord.getCoordinate(), radius, edges);
    }

    public Polygon asPolygon(FrontsCoordinate... coord) {
        return asPolygon(Arrays.stream(coord).map(FrontsCoordinate::getCoordinate).collect(Collectors.toList()));
    }

    public Polygon asPolygon(Coordinate... coord) {
        return ctxt.geometryFactory.createPolygon(coord);
    }

    public Polygon asPolygon(Collection<Coordinate> coord) {
        return ctxt.geometryFactory.createPolygon(coord.toArray(new Coordinate[coord.size()]));
    }

    public Geometry addBuffer(double buffer, Geometry initial) {
        BufferOp bufOp = new BufferOp(initial);
        bufOp.setEndCapStyle(BufferParameters.CAP_ROUND);// BufferOp.CAP_BUTT);
        return bufOp.getResultGeometry(buffer / 2);
    }

    public Geometry asPath(double buffer, Coordinate... points) {
        return addBuffer(buffer, asLine(points));
    }

    public Geometry asPath(double buffer, Collection<Coordinate> points) {
        return asPath(buffer, points.toArray(new Coordinate[points.size()]));
    }

    public Geometry asPath(double buffer, FrontsCoordinate... points) {
        return asPath(buffer, Arrays.stream(points).map(FrontsCoordinate::getCoordinate).collect(Collectors.toList()));
    }

    public Point asPoint(Coordinate c) {
        return ctxt.geometryFactory.createPoint(c);
    }

    public Point asPoint(FrontsCoordinate c) {
        return asPoint(c.getCoordinate());
    }

    public LineString asLine(Coordinate... coords) {
        return ctxt.geometryFactory.createLineString(coords);
    }
}
