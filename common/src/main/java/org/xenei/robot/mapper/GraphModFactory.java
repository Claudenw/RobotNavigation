package org.xenei.robot.mapper;

import java.util.ArrayList;
import java.util.List;

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
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.xenei.robot.common.FrontsCoordinate;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.utils.CoordUtils;
import org.xenei.robot.mapper.rdf.Namespace;

public class GraphModFactory {
    private static Resource INTERSECTS = ResourceFactory.createResource(Geof.SF_INTERSECTS);
    private static Resource DISTANCE = ResourceFactory.createProperty(Geof.DISTANCE_NAME);
    private static Resource METERS = ResourceFactory.createResource(Unit_URI.METRE_URL);
    private static Resource DEGREES = ResourceFactory.createResource(Unit_URI.DEGREE_URL);

    static GeometryFactory geometryFactory = new GeometryFactory();

    static Geometry fromWkt(Literal wkt) {
        GeometryWrapper wrapper = WKTDatatype.INSTANCE.parse(wkt.getLexicalForm());
        return wrapper.getParsingGeometry();
    }

    static Literal asWKT(Geometry geom) {
        return ResourceFactory.createTypedLiteral(geom.toText(), WKTDatatype.INSTANCE);
    }

    static Literal asWKT(Coordinate point) {
        return asWKT(geometryFactory.createPoint(point));
    }

    static Literal asWKTString(Coordinate... points) {
        return asWKT(geometryFactory.createLineString(points));
    }

    static Literal asWKTPolygon(Coordinate[] points) {
        return asWKT(geometryFactory.createPolygon(points));
    }

    static UpdateBuilder addPath(Resource model, Coordinate... points) {
        Resource rA = Namespace.urlOf(points[0]);
        Resource rB = Namespace.urlOf(points[points.length - 1]);
        Literal path = asWKTString(points);
        Node tn = NodeFactory.createTripleNode(rA.asNode(), Namespace.Path.asNode(), rB.asNode());
        return new UpdateBuilder().addInsert(model, tn, RDF.type, Namespace.Path).addInsert(model, tn, Geo.AS_WKT_PROP,
                path);
    }

//        UpdateBuilder breakPath(Resource model, Coordinate point) {
//            Resource p = Namespace.urlOf(point);
//            Var wkt = Var.alloc("wkt");
//            WhereBuilder wb = findPath(Namespace.o, asPolygon(point, 1));
//            wb.addWhere(Namespace.o, Geo.AS_WKT_PROP, wkt);
//            SelectBuilder sb = new SelectBuilder().addGraph(model, wb).addVar(Namespace.o).addVar(wkt);
//            try (QueryExecution qexec = QueryExecutionFactory.create(sb.build(), data.getUnionModel())) {
//                ResultSet rs = qexec.execSelect();
//                while (rs.hasNext()) {
//                    /// parse the WKT and find the line segment that contains point.
//                    // remove point from that line segment.
//                }
//            }
//            return new UpdateBuilder();
//        }

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

    public static Resource asRDF(Coordinate a, Resource type, Geometry geom) {
        if (geom == null) {
            geom = asPoint(a);
        }
        Model result = ModelFactory.createDefaultModel();
        Resource r = type == null ? result.createResource(Namespace.urlStr(a))
                : result.createResource(Namespace.urlStr(a), type);
        r.addLiteral(Namespace.x, a.getX());
        r.addLiteral(Namespace.y, a.getY());
        // r.addProperty(RDF.type, Namespace.Point);
        // r.addProperty(RDF.type, Geo.GEOMETRY_RES);
        // try {
        r.addLiteral(Geo.AS_WKT_PROP, asWKT(geom));
//            } catch (Exception e) {
//                Log.error(e, a.toString());
//            }
        return r;
    }

    static Resource asRDF(FrontsCoordinate p, Resource type, org.locationtech.jts.geom.Geometry geom) {
        return asRDF(p.getCoordinate(), type, geom);
    }

    static Polygon asPolygon(Coordinate coord, double range) {
        double angle = 0.0;
        double radians = Math.toRadians(60);
        Coordinate[] cell = new Coordinate[6];
        Location l = new Location(coord);
        for (int i = 0; i < 6; i++) {
            cell[i] = l.plus(CoordUtils.fromAngle(angle, range)).getCoordinate();
            angle += radians;
        }
        cell[5] = cell[0];
        return geometryFactory.createPolygon(cell);
    }

    static Polygon asPolygon(FrontsCoordinate coord, double range) {
        return asPolygon(coord.getCoordinate(), range);
    }

    public static Point asPoint(Coordinate c) {
        return geometryFactory.createPoint(c);
    }

    static Point asPoint(FrontsCoordinate c) {
        return asPoint(c.getCoordinate());
    }

}