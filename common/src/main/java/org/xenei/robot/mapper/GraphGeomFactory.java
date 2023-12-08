package org.xenei.robot.mapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.jena.arq.querybuilder.ExprFactory;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.arq.querybuilder.clauses.WhereClause;
import org.apache.jena.geosparql.implementation.GeometryWrapper;
import org.apache.jena.geosparql.implementation.datatype.WKTDatatype;
import org.apache.jena.geosparql.implementation.vocabulary.Geo;
import org.apache.jena.geosparql.implementation.vocabulary.Geof;
import org.apache.jena.geosparql.implementation.vocabulary.Unit_URI;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.vocabulary.RDF;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.xenei.robot.common.FrontsCoordinate;
import org.xenei.robot.common.utils.GeometryUtils;
import org.xenei.robot.mapper.rdf.Namespace;

public class GraphGeomFactory {
    private static Resource INTERSECTS = ResourceFactory.createResource(Geof.SF_INTERSECTS);
    private static Resource DISTANCE = ResourceFactory.createProperty(Geof.DISTANCE_NAME);
    private static Resource METERS = ResourceFactory.createResource(Unit_URI.METRE_URL);
    private static Resource DEGREES = ResourceFactory.createResource(Unit_URI.DEGREE_URL);

    static Geometry fromWkt(Literal wkt) {
        GeometryWrapper wrapper = WKTDatatype.INSTANCE.parse(wkt.getLexicalForm());
        return wrapper.getParsingGeometry();
    }

    static Literal asWKT(Geometry geom) {
        return ResourceFactory.createTypedLiteral(geom.toText(), WKTDatatype.INSTANCE);
    }

    static Literal asWKT(Coordinate point) {
        return asWKT(GeometryUtils.asPoint(point));
    }

    static Literal asWKTString(Coordinate... points) {
        return asWKT(GeometryUtils.asPath(points));
    }

    static Literal asWKTPolygon(Coordinate[] points) {
        return asWKT(GeometryUtils.asPolygon(points));
    }

    static UpdateBuilder addPath(Resource model, Coordinate... points) {
        Resource rA = Namespace.urlOf(points[0]);
        Resource rB = Namespace.urlOf(points[points.length - 1]);
        Literal path = asWKTString(points);
        Node tn = NodeFactory.createTripleNode(rA.asNode(), Namespace.Path.asNode(), rB.asNode());
        return new UpdateBuilder().addInsert(model, tn, RDF.type, Namespace.Path).addInsert(model, tn, Geo.AS_WKT_PROP,
                path);
    }

    static WhereBuilder findPath(Node o, Geometry point) {
        return addNearby(new WhereBuilder(), o, point, 1).addWhere(o, RDF.type, Namespace.Path);
    }

    static Expr checkCollision(ExprFactory expF, Object geo1, Object geo2) {
        return expF.call(INTERSECTS, geo1, geo2);
    }

    static Expr calcDistance(ExprFactory expF, Object geo1, Object geo2) {
        return expF.call(DISTANCE, geo1, geo2, DEGREES);
    }

    @SuppressWarnings("unchecked")
    static <T extends WhereClause<?>> T addNearby(T whereClause, Node s, Geometry geo, double distance) {
        List<Object> args = new ArrayList<>();
        args.add(asWKT(geo));
        args.add(NodeFactory.createLiteralByValue(distance));
        args.add(METERS);
        return (T) whereClause.addWhere(s, Namespace.nearbyF, args);
    }

    public static Resource asRDF(Coordinate a) {
        return asRDF(a, Namespace.Point, GeometryUtils.asPoint(a));
    }
    
    public static Resource asRDF(Coordinate a, Resource type) {
        return asRDF(a, type, GeometryUtils.asPoint(a));
    }
    
    public static Resource asRDF(Coordinate a, Resource type, Geometry geom) {
        Objects.requireNonNull(a);
        Objects.requireNonNull(type);
        Objects.requireNonNull(geom);
        Model result = ModelFactory.createDefaultModel();
        Resource r = result.createResource(Namespace.urlStr(a), type);
        r.addLiteral(Namespace.x, a.getX());
        r.addLiteral(Namespace.y, a.getY());
        r.addLiteral(Geo.AS_WKT_PROP, asWKT(geom));
        return r;
    }

    static Resource asRDF(FrontsCoordinate p, Resource type, org.locationtech.jts.geom.Geometry geom) {
        return asRDF(p.getCoordinate(), type, geom);
    }

}