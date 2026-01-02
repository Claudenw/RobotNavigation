package org.xenei.robot.mapper.rdf;

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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.xenei.robot.common.ChassisInfoTest;
import org.xenei.robot.common.ScaleInfo;
import org.xenei.robot.common.utils.RobutContext;

public class GraphGeomFactoryTest {

    private RobutContext ctxt;

    private Dataset createDataset(Model m) {
        Dataset ds = DatasetFactory.create(m);
        try {
            SpatialIndex.buildSpatialIndex(ds, "http://www.opengis.net/def/crs/OGC/1.3/CRS84");
        } catch (SpatialIndexException e) {
            throw new RuntimeException(e);
        }
        return ds;
    }

    @BeforeEach
    void setup() {
        RobutContext.Builder builder = RobutContext.builder();
        builder.setOptions(builder.defaultOptions())
                .setChassisInfo(ChassisInfoTest.DEFAULT);
        ctxt = builder.build();
    }

    @AfterEach
    void teardown() {
        ctxt.close();
    }

    // @Test
    // public void checkCollisionTest() {
    // Coordinate c = new Coordinate(1, 1);
    // Model m = ctxt.graphGeomFactory.asRDF(c, Namespace.Obst,
    // GeometryUtils.asPolygon(c, 1)).getModel();
    // Dataset ds = createDataset(m);
    // Literal testWkt = ctxt.graphGeomFactory.asWKT(c);
    //
    // ExprFactory exprF = new ExprFactory(m);
    // AskBuilder ask = new AskBuilder().addWhere(Namespace.s, Geo.AS_WKT_PROP,
    // "?wkt")
    // .addFilter(ctxt.graphGeomFactory.checkCollision(exprF, "?wkt", testWkt, 0));
    // try (QueryExecution qexec = QueryExecutionFactory.create(ask.build(), ds)) {
    // assertTrue(qexec.execAsk());
    // }
    // }

    @Test
    void calcDistanceTest() {
        Coordinate c = new Coordinate(1, 1);
        Coordinate b = new Coordinate(1, 5);

        Model m = ctxt.graphGeomFactory.asRDF(c, Namespace.Coord).getModel();
        m.add(ctxt.graphGeomFactory.asRDF(b, Namespace.Coord).getModel());
        Dataset ds = createDataset(m);
        Literal testWkt = ctxt.graphGeomFactory.asWKT(c);

        ExprFactory exprF = new ExprFactory(m);
        AskBuilder ask = new AskBuilder().addWhere(Namespace.s, Geo.AS_WKT_PROP, "?wkt")
                .addBind(ctxt.graphGeomFactory.calcDistance(exprF, "?wkt", testWkt), "?cost")
                .addFilter(exprF.eq("?cost", 4));

        try (QueryExecution qexec = QueryExecutionFactory.create(ask.build(), ds)) {
            assertTrue(qexec.execAsk());
        }
    }

    @Test
    void asRDFTest() {
        Coordinate p = new Coordinate(-1, 3);

        Resource r = ctxt.graphGeomFactory.asRDF(p, Namespace.Coord);
        assertTrue(r.hasLiteral(Namespace.x, -1.0));
        assertTrue(r.hasLiteral(Namespace.y, 3.0));
        assertTrue(r.hasProperty(RDF.type, Namespace.Coord));
        assertTrue(r.hasProperty(Geo.AS_WKT_PROP, ctxt.graphGeomFactory.asWKT(ctxt.geometryUtils.asPoint(p))));

        r = ctxt.graphGeomFactory.asRDF(p, Namespace.Coord, ctxt.geometryUtils.asPolygon(p, 3));
        assertTrue(r.hasLiteral(Namespace.x, -1.0));
        assertTrue(r.hasLiteral(Namespace.y, 3.0));
        assertTrue(r.hasProperty(RDF.type, Namespace.Coord));
        assertTrue(r.hasProperty(Geo.AS_WKT_PROP, ctxt.graphGeomFactory.asWKT(ctxt.geometryUtils.asPolygon(p, 3))));
    }
}
