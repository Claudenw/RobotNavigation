package org.xenei.robot.common.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.OctagonalEnvelope;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.util.GeometryEditor;
import org.locationtech.jts.operation.buffer.BufferOp;
import org.locationtech.jts.operation.buffer.BufferParameters;
import org.xenei.robot.common.GeometricObject;
import org.xenei.robot.common.Location;

public final class GeometryUtils {

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
        for (int i = 0; i < edges; i++) {
            cell[i] = CoordUtils.plus(coord, CoordUtils.fromAngle(angle, radius));
            angle += radians;
        }
        cell[edges] = cell[0];
        return ctxt.geometryFactory.createPolygon(cell);
    }

    public Polygon asPolygon(Location coord, double radius) {
        return asPolygon(coord.getCoordinate(), radius);
    }

    public Polygon asPolygon(Location coord, double radius, int edges) {
        return asPolygon(coord.getCoordinate(), radius, edges);
    }

    public Polygon asPolygon(Location... coord) {
        return asPolygon(Arrays.stream(coord).map(Location::getCoordinate).collect(Collectors.toList()));
    }

    public Polygon asPolygon(Coordinate... coord) {
        return ctxt.geometryFactory.createPolygon(coord);
    }

    public Polygon asPolygon(Collection<Coordinate> coord) {
        return ctxt.geometryFactory.createPolygon(coord.toArray(new Coordinate[0]));
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
        return asPath(buffer, points.toArray(new Coordinate[0]));
    }

    public Geometry asPath(double buffer, Location... points) {
        return asPath(buffer, Arrays.stream(points).map(Location::getCoordinate).collect(Collectors.toList()));
    }

    public Point asPoint(Coordinate c) {
        return ctxt.geometryFactory.createPoint(c);
    }

    public Point asPoint(Location c) {
        return asPoint(c.getCoordinate());
    }

    public LineString asLine(Coordinate... coords) {
        return ctxt.geometryFactory.createLineString(coords);
    }

    public LineString asLine(Stream<? extends Location> coords) {
        return ctxt.geometryFactory.createLineString(coords.map(Location::getCoordinate).toArray(Coordinate[]::new));
    }

    public LineString asLine(Location... coords) {
        return ctxt.geometryFactory.createLineString(
                Arrays.stream(coords).map(Location::getCoordinate).toArray(Coordinate[]::new));
    }

    public Geometry scale(Geometry geometry) {
        GeometryEditor.CoordinateOperation  operation = new GeometryEditor.CoordinateOperation() {
            @Override
            public Coordinate[] edit(Coordinate[] coordinates, Geometry geometry) {
                return Arrays.stream(geometry.getCoordinates()).map(ctxt.scaleInfo::scale).toArray(Coordinate[]::new);
            }
        };
        GeometryEditor editor = new GeometryEditor(ctxt.geometryFactory);
        return editor.edit(geometry, operation);
    }


    /**
     * Converts a collection of points into line segments joining each coordinate to its
     * closest neighbors.
     */
    public Geometry makeCloud(double resolution, Stream<? extends GeometricObject> geometries) {
        // make the distance across the diagonal of a square of resolution the maxDistance for joining.
        double maxDistance = resolution * DoubleUtils.SQRT2;

        Set<Coordinate> pointSet = geometries.flatMap(geom-> Arrays.stream(geom.getGeometry().getCoordinates()))
                .collect(Collectors.toSet());

        if (pointSet.isEmpty()) {
            throw new IllegalArgumentException("Geometries must not be empty");
        }
        if (pointSet.size() == 1) {
            return asPoint(pointSet.iterator().next());
        }

        /* matrix of flags for the shortest distances */
        final Coordinate[] points = pointSet.toArray(new Coordinate[0]);
        // populate the distance matrix and the shortest distance count
        List<LineString> lst = new ArrayList<>();
        for (int i = 0; i < points.length - 1; i++) {
            for (int j = i + 1; j < points.length; j++) {
                double d = points[i].distance(points[j]);
                if (d <= maxDistance) {
                    lst.add(ctxt.geometryFactory
                            .createLineString(new Coordinate[]{points[i], points[j]}));
                }
            }
        }
        Geometry result = ctxt.geometryFactory.createMultiLineString(lst.toArray(new LineString[0]));
        result.normalize();
        return result;
    }

    public static OctagonalEnvelope createBoundingBox(Location start, Location target) {
        return createBoundingBox(start.getCoordinate(), target.getCoordinate());
    }

    public static OctagonalEnvelope createBoundingBox(Coordinate start, Coordinate target) {
        double theta = CoordUtils.angleBetween(start, target);
        double range = start.distance(target);

        OctagonalEnvelope envelope = new OctagonalEnvelope(start);
        envelope.expandToInclude(CoordUtils.add(start, CoordUtils.fromAngle(theta + AngleUtils.RADIANS_45, range)));
        envelope.expandToInclude(CoordUtils.add(start, CoordUtils.fromAngle(theta, 2 * range)));
        envelope.expandToInclude(CoordUtils.add(start, CoordUtils.fromAngle(theta - AngleUtils.RADIANS_45, range)));
        return envelope;
    }
}
