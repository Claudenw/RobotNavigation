package org.xenei.robot.mapper;

import java.util.Objects;

import org.apache.jena.arq.querybuilder.ExprFactory;
import org.apache.jena.geosparql.implementation.GeometryWrapper;
import org.apache.jena.geosparql.implementation.datatype.WKTDatatype;
import org.apache.jena.geosparql.implementation.vocabulary.Geo;
import org.apache.jena.geosparql.implementation.vocabulary.Geof;
import org.apache.jena.geosparql.implementation.vocabulary.SpatialExtension;
import org.apache.jena.geosparql.implementation.vocabulary.Unit_URI;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.sparql.expr.Expr;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.xenei.robot.common.FrontsCoordinate;
import org.xenei.robot.common.utils.GeometryUtils;
import org.xenei.robot.mapper.rdf.Namespace;

public class GraphGeomFactory {
    private static Resource OVERLAPS = ResourceFactory.createResource(Geof.SF_OVERLAPS);
    private static Resource INTERSECTS = ResourceFactory.createResource(Geof.SF_INTERSECTS);
    private static Resource TOUCH = ResourceFactory.createResource(Geof.SF_TOUCHES);
    private static Resource DISTANCE = ResourceFactory.createProperty(Geof.DISTANCE_NAME);
    private static Resource NEARBY = ResourceFactory.createProperty(SpatialExtension.NEARBY);
    private static Resource METERS = ResourceFactory.createResource(Unit_URI.METRE_URL);
    private static Resource DEGREES = ResourceFactory.createResource(Unit_URI.DEGREE_URL);

    static Geometry fromWkt(Literal wkt) {
        GeometryWrapper wrapper = WKTDatatype.INSTANCE.parse(wkt.getLexicalForm());
        return wrapper.getParsingGeometry();
    }

    public static Literal asWKT(Geometry geom) {
        return ResourceFactory.createTypedLiteral(geom.toText(), WKTDatatype.INSTANCE);
    }

    static Literal asWKT(Coordinate point) {
        return asWKT(GeometryUtils.asPoint(point));
    }

    public static Literal asWKTString(Coordinate... points) {
        return asWKT(GeometryUtils.asLine(points));
    }

    public static Literal asWKTPath(double buffer, Coordinate... points) {
        return asWKT(GeometryUtils.asPath(buffer, points));
    }
    
    static Literal asWKTPolygon(Coordinate[] points) {
        return asWKT(GeometryUtils.asPolygon(points));
    }

    static Literal asWKTPolygon(Coordinate points, double buffer) {
        return asWKT(GeometryUtils.asPolygon(points, buffer));
    }
    
    static Literal asWKTPolygon(Coordinate points, double buffer, int edges) {
        return asWKT(GeometryUtils.asPolygon(points, buffer, edges));
    }
    
    public static Expr calcDistance(ExprFactory expF, Object geo1, Object geo2) {
        return expF.call(DISTANCE, geo1, geo2, DEGREES);
    }

    public static Expr overlaps(ExprFactory expF, Object geo1, Object geo2) {
        return expF.call(OVERLAPS, geo1, geo2);
    }
    
    public static Expr intersects(ExprFactory expF, Object geo1, Object geo2) {
        return expF.call(INTERSECTS, geo1, geo2);
    }
    
    public static Expr touches(ExprFactory expF, Object geo1, Object geo2) {
        return expF.call(TOUCH, geo1, geo2);
    }
    
    @SuppressWarnings("unchecked")
    static Expr isNearby(ExprFactory exprF, Object geo1, Object geo2, Object distance) {
        return exprF.call(NEARBY, geo1, geo2, distance, METERS);
    }

    public static Resource asRDF(Coordinate a, Resource type) {
        return asRDF(a, type, GeometryUtils.asPoint(a));
    }

    public static Resource asRDF(FrontsCoordinate a, Resource type) {
        return asRDF(a.getCoordinate(), type, GeometryUtils.asPoint(a));
    }

    public static Resource asRDF(Coordinate a, Resource type, Geometry geom) {
        Objects.requireNonNull(a);
        Objects.requireNonNull(type);
        Objects.requireNonNull(geom);
        Model result = ModelFactory.createDefaultModel();
        Resource r = result.createResource(type);
        r.addLiteral(Namespace.x, a.getX());
        r.addLiteral(Namespace.y, a.getY());
        r.addLiteral(Geo.AS_WKT_PROP, asWKT(geom));
        return r;
    }

    static Resource asRDF(FrontsCoordinate p, Resource type, org.locationtech.jts.geom.Geometry geom) {
        return asRDF(p.getCoordinate(), type, geom);
    }
}