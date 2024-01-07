package org.xenei.robot.mapper;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.arq.querybuilder.ExprFactory;
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
import org.xenei.robot.common.ScaleInfo;
import org.xenei.robot.common.utils.RobutContext;
import org.xenei.robot.common.utils.GeometryUtils;
import org.xenei.robot.mapper.rdf.Namespace;

public class GraphGeomFactoryTest {

    private static RobutContext ctxt = new RobutContext(ScaleInfo.DEFAULT);
    
    private Dataset createDataset(Model m) {
        Dataset ds = DatasetFactory.create(m);
        return ds;
    }

//    @Test
//    public void checkCollisionTest() {
//        Coordinate c = new Coordinate(1, 1);
//        Model m = ctxt.graphGeomFactory.asRDF(c, Namespace.Obst, GeometryUtils.asPolygon(c, 1)).getModel();
//        Dataset ds = createDataset(m);
//        Literal testWkt = ctxt.graphGeomFactory.asWKT(c);
//
//        ExprFactory exprF = new ExprFactory(m);
//        AskBuilder ask = new AskBuilder().addWhere(Namespace.s, Namespace.wkt, "?wkt")
//                .addFilter(ctxt.graphGeomFactory.checkCollision(exprF, "?wkt", testWkt, 0));
//        try (QueryExecution qexec = QueryExecutionFactory.create(ask.build(), ds)) {
//            assertTrue(qexec.execAsk());
//        }
//    }

    @Test
    public void calcDistanceTest() {
        Coordinate c = new Coordinate(1, 1);
        Coordinate b = new Coordinate(1, 5);

        Model m = ctxt.graphGeomFactory.asRDF(c, Namespace.Coord).getModel();
        m.add(ctxt.graphGeomFactory.asRDF(b, Namespace.Coord).getModel());
        Dataset ds = createDataset(m);
        Literal testWkt = ctxt.graphGeomFactory.asWKT(c);

        ExprFactory exprF = new ExprFactory(m);
        AskBuilder ask = new AskBuilder().addWhere(Namespace.s, Namespace.wkt, "?wkt")
                .addBind(ctxt.graphGeomFactory.calcDistance(exprF, "?wkt", testWkt), "?cost")
                .addFilter(exprF.eq("?cost", 4));

        try (QueryExecution qexec = QueryExecutionFactory.create(ask.build(), ds)) {
            assertTrue(qexec.execAsk());
        }
    }

    @Test
    public void asRDFTest() {
        Coordinate p = new Coordinate(-1, 3);

        Resource r = ctxt.graphGeomFactory.asRDF(p, Namespace.Coord);
        assertTrue(r.hasLiteral(Namespace.x, -1.0));
        assertTrue(r.hasLiteral(Namespace.y, 3.0));
        assertTrue(r.hasProperty(RDF.type, Namespace.Coord));
        assertTrue(r.hasProperty(Namespace.wkt, ctxt.graphGeomFactory.asWKT(ctxt.geometryUtils.asPoint(p))));

        r = ctxt.graphGeomFactory.asRDF(p, Namespace.Coord, ctxt.geometryUtils.asPolygon(p, 3));
        assertTrue(r.hasLiteral(Namespace.x, -1.0));
        assertTrue(r.hasLiteral(Namespace.y, 3.0));
        assertTrue(r.hasProperty(RDF.type, Namespace.Coord));
        assertTrue(r.hasProperty(Namespace.wkt, ctxt.graphGeomFactory.asWKT(ctxt.geometryUtils.asPolygon(p, 3))));
    }
}
