package org.xenei.robot.mapper.rdf;

import java.io.IOException;

import org.apache.jena.atlas.logging.Log;
import org.apache.jena.query.ARQ;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.pfunction.PropertyFunctionRegistry;
import org.apache.jena.vocabulary.RDF;
import org.xenei.robot.common.Coordinates;
import org.xenei.robot.common.Position;

import mil.nga.sf.Point;
import mil.nga.sf.wkt.GeometryWriter;

public class Namespace {
    public static final String URI = "urn:org.xenei.robot:";
    public static final Resource UnionModel = ResourceFactory.createResource("urn:x-arq:UnionGraph");
    public static final Resource BaseModel = ResourceFactory.createResource(URI + "model:BaseModel");
    public static final Resource PlanningModel = ResourceFactory.createResource(URI + "model:PlanningModel");
    public static final Resource Coord = ResourceFactory.createResource(URI + "Coord");
    public static final Resource Obst = ResourceFactory.createResource(URI + "Obstacle");
    public static final Property x = ResourceFactory.createProperty(URI + "x");
    public static final Property y = ResourceFactory.createProperty(URI + "y");
    public static final Property path = ResourceFactory.createProperty(URI + "path");
    public static final Property distance = ResourceFactory.createProperty(URI + "distance");
    public static final Property adjustment =  ResourceFactory.createProperty(URI + "adjustment");
    public static final Property distF = ResourceFactory.createProperty(URI + "fn:dist");
    
    public static final Resource Point = ResourceFactory.createResource("http://www.opengis.net/ont/sf#Point");
    public static final Property asWKT = ResourceFactory.createProperty("http://www.opengis.net/ont/geosparql#asWKT");
    
    public static final Var s = Var.alloc("s");
    public static final Var p = Var.alloc("p");
    public static final Var o = Var.alloc("o");

    private static final String POINT_URI_FMT = URI+"point:%.0f:%.0f";
    static {
        final PropertyFunctionRegistry reg = PropertyFunctionRegistry.chooseRegistry(ARQ.getContext());
        reg.put(distF.getURI(), Dist.class);
        PropertyFunctionRegistry.set(ARQ.getContext(), reg);
    }
    
    
    public static Resource urlOf(Point p) {
        return ResourceFactory.createResource(String.format(POINT_URI_FMT, p.getX(), p.getY()));
    }
    

    public static Resource asRDF(Coordinates a, Resource type) {
        Coordinates qA = a.quantize();
        Model result = ModelFactory.createDefaultModel();
        String uri = String.format(POINT_URI_FMT, qA.getX(), qA.getY());
        Resource r = type == null ? result.createResource(uri) : result.createResource(uri, type);
        r.addLiteral(x, qA.getX());
        r.addLiteral(y, qA.getY());
        r.addProperty(RDF.type, Point);
        try {
            r.addLiteral(asWKT, GeometryWriter.writeGeometry(a.asPoint()));
        } catch (IOException e) {
            Log.error(e, a.toString());
        }
        
        return r;
    }
    
    public static Resource asRDF(Point p, Resource type) {
        return asRDF(Coordinates.fromXY(p), type);
    }
}