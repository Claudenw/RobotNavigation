package org.xenei.robot.mapper;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.arq.querybuilder.ExprFactory;
import org.apache.jena.geosparql.implementation.vocabulary.Geo;
import org.apache.jena.geosparql.spatial.SpatialIndex;
import org.apache.jena.geosparql.spatial.SpatialIndexException;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.xenei.robot.common.utils.GeometryUtils;
import org.xenei.robot.mapper.rdf.Namespace;

public class GraphGeomFactoryTest {

    private Dataset createDataset(Model m) {
        Dataset ds = DatasetFactory.create(m);
        try {
            SpatialIndex.buildSpatialIndex(ds, "http://www.opengis.net/def/crs/OGC/1.3/CRS84");
        } catch (SpatialIndexException e) {
            throw new RuntimeException(e);
        }
        return ds;
    }

    @Test
    public void checkCollisionTest() {
        Coordinate c = new Coordinate(1, 1);
        Model m = GraphGeomFactory.asRDF(c, Namespace.Obst, GeometryUtils.asPolygon(c, 1)).getModel();
        Dataset ds = createDataset(m);
        Literal testWkt = GraphGeomFactory.asWKT(c);

        ExprFactory exprF = new ExprFactory(m);
        AskBuilder ask = new AskBuilder().addWhere(Namespace.s, Geo.AS_WKT_PROP, "?wkt")
                .addFilter(GraphGeomFactory.checkCollision(exprF, "?wkt", testWkt, 0));
        try (QueryExecution qexec = QueryExecutionFactory.create(ask.build(), ds)) {
            assertTrue(qexec.execAsk());
        }
    }

    @Test
    public void calcDistanceTest() {
        Coordinate c = new Coordinate(1, 1);
        Coordinate b = new Coordinate(1, 5);

        Model m = GraphGeomFactory.asRDF(c, Namespace.Coord).getModel();
        m.add(GraphGeomFactory.asRDF(b, Namespace.Coord).getModel());
        Dataset ds = createDataset(m);
        Literal testWkt = GraphGeomFactory.asWKT(c);

        ExprFactory exprF = new ExprFactory(m);
        AskBuilder ask = new AskBuilder().addWhere(Namespace.s, Geo.AS_WKT_PROP, "?wkt")
                .addBind(GraphGeomFactory.calcDistance(exprF, "?wkt", testWkt), "?cost")
                .addFilter(exprF.eq("?cost", 4));

        try (QueryExecution qexec = QueryExecutionFactory.create(ask.build(), ds)) {
            assertTrue(qexec.execAsk());
        }
    }

    @Test
    public void asRDFTest() {
        Coordinate p = new Coordinate(-1, 3);

        Resource r = GraphGeomFactory.asRDF(p, Namespace.Coord);
        assertTrue(r.hasLiteral(Namespace.x, -1.0));
        assertTrue(r.hasLiteral(Namespace.y, 3.0));
        assertTrue(r.hasProperty(RDF.type, Namespace.Coord));
        assertTrue(r.hasProperty(Geo.AS_WKT_PROP, GraphGeomFactory.asWKT(GeometryUtils.asPoint(p))));

        r = GraphGeomFactory.asRDF(p, Namespace.Coord, GeometryUtils.asPolygon(p, 3));
        assertTrue(r.hasLiteral(Namespace.x, -1.0));
        assertTrue(r.hasLiteral(Namespace.y, 3.0));
        assertTrue(r.hasProperty(RDF.type, Namespace.Coord));
        assertTrue(r.hasProperty(Geo.AS_WKT_PROP, GraphGeomFactory.asWKT(GeometryUtils.asPolygon(p, 3))));
    }
}
