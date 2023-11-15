package org.xenei.robot.planner.rdf;

import org.apache.jena.query.ARQ;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.sparql.pfunction.PropertyFunctionRegistry;
import org.xenei.robot.navigation.Coordinates;
import org.xenei.robot.navigation.Point;
import org.xenei.robot.navigation.Position;

public class Namespace {
    public static final String URI = "urn:org.xenei.robot:";
    public static final Resource BaseModel = ResourceFactory.createResource(URI + "model:BaseModel");
    public static final Resource PlanningModel = ResourceFactory.createResource(URI + "model:PlanningModel");
    public static final Resource Coord = ResourceFactory.createResource(URI + "Coord");
    public static final Resource Obst = ResourceFactory.createResource(URI + "Obstacle");
    public static final Property x = ResourceFactory.createProperty(URI + "x");
    public static final Property y = ResourceFactory.createProperty(URI + "y");
    public static final Property path = ResourceFactory.createProperty(URI + "path");
    public static final Property weight = ResourceFactory.createProperty(URI + "weight");
    public static final Property distF = ResourceFactory.createProperty(URI + "fn:dist");

    private static final String POINT_URI_FMT = URI+"point:%.0f:%.0f";
    static {
        final PropertyFunctionRegistry reg = PropertyFunctionRegistry.chooseRegistry(ARQ.getContext());
        reg.put(distF.getURI(), Dist.class);
        PropertyFunctionRegistry.set(ARQ.getContext(), reg);
    }
    
    public static Resource urlOf(Point p) {
        return ResourceFactory.createResource( String.format(POINT_URI_FMT, p.x, p.y) );
    }
    
    public static Resource urlOf(Coordinates c) {
        return urlOf(c.getPoint());
    }
    
    public static Resource urlOf(Position p) {
        return urlOf(p.coordinates().getPoint());
    }

    public static Resource asRDF(Coordinates a, Resource type) {
        Coordinates qA = a.quantize();
        Model result = ModelFactory.createDefaultModel();
        String uri = String.format(POINT_URI_FMT, qA.getX(), qA.getY());
        Resource r = result.createResource(uri, type);
        r.addLiteral(x, qA.getX());
        r.addLiteral(y, qA.getY());
        return r;
    }
    
    public static Resource asRDF(Position p, Resource type) {
        return asRDF(p.coordinates(), type);
    }
    
    public static Resource asRDF(Point p, Resource type) {
        return asRDF(Coordinates.fromXY(p), type);
    }
}