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
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.OctagonalEnvelope;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.util.GeometryEditor;
import org.locationtech.jts.operation.buffer.BufferOp;
import org.locationtech.jts.operation.buffer.BufferParameters;
import org.xenei.robot.common.GeometricObject;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.ScaleInfo;

public final class GeometryUtils {

    private final GeometryFactory geometryFactory;
    private final ScaleInfo scaleInfo;

    public GeometryUtils(GeometryFactory geometryFactory, ScaleInfo scaleInfo) {
        this.geometryFactory = geometryFactory;
        this.scaleInfo = scaleInfo;
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
        return geometryFactory.createPolygon(cell);
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
        return geometryFactory.createPolygon(coord);
    }

    public Polygon asPolygon(Collection<Coordinate> coord) {
        return geometryFactory.createPolygon(coord.toArray(new Coordinate[0]));
    }

    public Geometry addBuffer(double buffer, Geometry initial) {
        BufferOp bufOp = new BufferOp(initial);
        bufOp.setEndCapStyle(BufferParameters.CAP_ROUND);// BufferOp.CAP_BUTT);
        return bufOp.getResultGeometry(buffer / 2);
    }

    public Geometry asPath(double buffer, Coordinate... coordinates) {
        return addBuffer(buffer, asLine(coordinates));
    }

    public Geometry asPath(double buffer, Collection<Coordinate> coordinates) {
        return addBuffer(buffer, asLine(coordinates.toArray(new Coordinate[0])));
    }

    public Geometry asPath(double buffer, Location... locations) {
        return addBuffer(buffer, asLine(Arrays.stream(locations)));
    }

    public Geometry asPath(double buffer, Stream<? extends Location> coords) {
        return addBuffer(buffer, asLine(coords));
    }

    public Point asPoint(Coordinate c) {
        return geometryFactory.createPoint(c);
    }

    public Point asPoint(Location c) {
        return asPoint(c.getCoordinate());
    }

    public LineString asLine(Coordinate... coords) {
        return geometryFactory.createLineString(coords);
    }

    public LineString asLine(Stream<? extends Location> coords) {
        return geometryFactory.createLineString(coords.map(Location::getCoordinate).toArray(Coordinate[]::new));
    }

    public Geometry scale(Geometry geometry) {
        GeometryEditor.CoordinateOperation  operation = new GeometryEditor.CoordinateOperation() {
            @Override
            public Coordinate[] edit(Coordinate[] coordinates, Geometry geometry) {
                return Arrays.stream(geometry.getCoordinates()).map(scaleInfo::scale).toArray(Coordinate[]::new);
            }
        };
        GeometryEditor editor = new GeometryEditor(geometryFactory);
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
                    lst.add(geometryFactory
                            .createLineString(new Coordinate[]{points[i], points[j]}));
                }
            }
        }
        Geometry result = geometryFactory.createMultiLineString(lst.toArray(new LineString[0]));
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
