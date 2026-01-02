package org.xenei.robot.mapper.rdf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.arq.querybuilder.ExprFactory;
import org.apache.jena.arq.querybuilder.SelectBuilder;
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
import org.locationtech.jts.geom.GeometryFactory;
import org.xenei.robot.common.ChassisInfoTest;
import org.xenei.robot.common.ScaleInfo;
import org.xenei.robot.common.utils.GeometryUtils;
import org.xenei.robot.common.utils.RobutContext;

public class GraphGeomFactoryTest {

    GeometryFactory geometryFactory = new GeometryFactory(ScaleInfo.DEFAULT.getPrecisionModel());
    GeometryUtils geometryUtils = new GeometryUtils(geometryFactory, ScaleInfo.DEFAULT);
    GraphGeomFactory graphGeomFactory = new GraphGeomFactory(geometryUtils);

    private Dataset createDataset(Model m) {
        Dataset ds = DatasetFactory.create(m);
        try {
            SpatialIndex.buildSpatialIndex(ds, "http://www.opengis.net/def/crs/OGC/1.3/CRS84");
        } catch (SpatialIndexException e) {
            throw new RuntimeException(e);
        }
        return ds;
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

        Model m = graphGeomFactory.asRDF(c, Namespace.Coord).getModel();
        m.add(graphGeomFactory.asRDF(b, Namespace.Coord).getModel());
        Dataset ds = createDataset(m);
        Literal testWkt = graphGeomFactory.asWKT(c);

        ExprFactory exprF = new ExprFactory(m);
        SelectBuilder query = new SelectBuilder()
                .addVar("?cost")
                .addWhere(Namespace.s, Geo.AS_WKT_PROP, "?wkt")
                .addBind(graphGeomFactory.calcDistance(exprF, "?wkt", testWkt), "?cost");

        AskBuilder ask = new AskBuilder().addWhere(Namespace.s, Geo.AS_WKT_PROP, "?wkt")
                .addBind(graphGeomFactory.calcDistance(exprF, "?wkt", testWkt), "?cost")
                .addFilter(exprF.eq("?cost", 4));

        try (QueryExecution qexec = QueryExecutionFactory.create(query.build(), ds)) {
            qexec.execSelect().forEachRemaining(System.out::println);
        }

        try (QueryExecution qexec = QueryExecutionFactory.create(ask.build(), ds)) {
            assertThat(qexec.execAsk()).as(ask.toString()).isTrue();
        }
    }

    @Test
    void asRDFTest() {
        Coordinate p = new Coordinate(-1, 3);

        Resource r = graphGeomFactory.asRDF(p, Namespace.Coord);
        assertTrue(r.hasLiteral(Namespace.x, -1.0));
        assertTrue(r.hasLiteral(Namespace.y, 3.0));
        assertTrue(r.hasProperty(RDF.type, Namespace.Coord));
        assertTrue(r.hasProperty(Geo.AS_WKT_PROP, graphGeomFactory.asWKT(geometryUtils.asPoint(p))));

        r = graphGeomFactory.asRDF(p, Namespace.Coord, geometryUtils.asPolygon(p, 3));
        assertTrue(r.hasLiteral(Namespace.x, -1.0));
        assertTrue(r.hasLiteral(Namespace.y, 3.0));
        assertTrue(r.hasProperty(RDF.type, Namespace.Coord));
        assertTrue(r.hasProperty(Geo.AS_WKT_PROP, graphGeomFactory.asWKT(geometryUtils.asPolygon(p, 3))));
    }
}
