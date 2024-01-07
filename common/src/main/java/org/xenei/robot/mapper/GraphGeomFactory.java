package org.xenei.robot.mapper;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.collections4.map.LRUMap;
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

public final class GraphGeomFactory {

    private final GeometryUtils geometryUtils;
    
    
    public GraphGeomFactory(GeometryUtils geometryUtils) {
        this.geometryUtils = geometryUtils;
    }
    
    public Geometry fromWkt(Literal wkt) {
        return (Geometry) wkt.getValue();
    }

    public Literal asWKT(Geometry geom) {
        return ResourceFactory.createTypedLiteral(geom);
    }

    public Literal asWKT(Coordinate point) {
        return asWKT(geometryUtils.asPoint(point));
    }

    /** TODO rename as Line */
    public Literal asWKTString(Coordinate... points) {
        return asWKT(geometryUtils.asLine(points));
    }

    public Literal asWKTPath(double buffer, Coordinate... points) {
        return asWKT(geometryUtils.asPath(buffer, points));
    }
    
    public Literal asWKTPolygon(Coordinate[] points) {
        return asWKT(geometryUtils.asPolygon(points));
    }

    public Literal asWKTPolygon(Coordinate points, double buffer) {
        return asWKT(geometryUtils.asPolygon(points, buffer));
    }
    
    public Literal asWKTPolygon(Coordinate points, double buffer, int edges) {
        return asWKT(geometryUtils.asPolygon(points, buffer, edges));
    }
    
    public Expr calcDistance(ExprFactory expF, Object geo1, Object geo2) {
        return expF.call(Namespace.distanceF, geo1, geo2);
    }

    public Expr overlaps(ExprFactory expF, Object geo1, Object geo2) {
        return expF.call(Namespace.overlapsF, geo1, geo2);
    }
    
    public Expr intersects(ExprFactory expF, Object geo1, Object geo2) {
        return expF.call(Namespace.intersectsF, geo1, geo2);
    }
    
    public Expr touches(ExprFactory expF, Object geo1, Object geo2) {
        return expF.call(Namespace.touchesF, geo1, geo2);
    }
    
    @SuppressWarnings("unchecked")
    public Expr isNearby(ExprFactory exprF, Object geo1, Object geo2, Object distance) {
        return exprF.call(Namespace.nearbyF, exprF.asExpr(geo1), exprF.asExpr(geo2), exprF.asExpr(distance));
    }

    public Resource asRDF(Coordinate a, Resource type) {
        return asRDF(a, type, geometryUtils.asPoint(a));
    }

    public Resource asRDF(FrontsCoordinate a, Resource type) {
        return asRDF(a.getCoordinate(), type, geometryUtils.asPoint(a));
    }

    public Resource asRDF(Coordinate a, Resource type, Geometry geom) {
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

    public Resource asRDF(FrontsCoordinate p, Resource type, org.locationtech.jts.geom.Geometry geom) {
        return asRDF(p.getCoordinate(), type, geom);
    }
    
   
}