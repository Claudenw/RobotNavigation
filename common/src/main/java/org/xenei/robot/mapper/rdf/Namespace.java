package org.xenei.robot.mapper.rdf;

import java.io.IOException;

import org.apache.jena.atlas.logging.Log;
import org.apache.jena.geosparql.implementation.GeometryWrapper;
import org.apache.jena.geosparql.implementation.datatype.WKTDatatype;
import org.apache.jena.geosparql.implementation.vocabulary.Geo;
import org.apache.jena.geosparql.implementation.vocabulary.GeoSPARQL_URI;
import org.apache.jena.geosparql.implementation.vocabulary.Geof;
import org.apache.jena.geosparql.implementation.vocabulary.SpatialExtension;
import org.apache.jena.query.ARQ;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.pfunction.PropertyFunctionRegistry;
import org.apache.jena.vocabulary.RDF;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.xenei.robot.common.FrontsCoordinate;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.Position;


public class Namespace {
    public static final String URI = "urn:org.xenei.robot:";
    public static final Resource UnionModel = ResourceFactory.createResource("urn:x-arq:UnionGraph");
    public static final Resource BaseModel = ResourceFactory.createResource(URI + "model:BaseModel");
    public static final Resource PlanningModel = ResourceFactory.createResource(URI + "model:PlanningModel");
    public static final Resource Coord = ResourceFactory.createResource(URI + "Coord");
    public static final Resource Obst = ResourceFactory.createResource(URI + "Obstacle");
    public static final Resource Path = ResourceFactory.createResource(URI + "Path");
    
    public static final Property x = ResourceFactory.createProperty(URI + "x");
    public static final Property y = ResourceFactory.createProperty(URI + "y");
    public static final Property distance = ResourceFactory.createProperty(URI + "distance");
    public static final Property adjustment =  ResourceFactory.createProperty(URI + "adjustment");
    public static final Property cost = ResourceFactory.createProperty(URI + "cost");

    
    public static final Resource Point = ResourceFactory.createResource(GeoSPARQL_URI.SF_URI+"Point");
    
    public static final Property nearbyF = ResourceFactory.createProperty(SpatialExtension.NEARBY_PROP );
    //public static final Property intersectF = Geo.SF_INTERSECTS_PROP;
   // public static final Property distanceF = ResourceFactory.createProperty(Geof.DISTANCE_NAME);

    public static final Var s = Var.alloc("s");
    public static final Var p = Var.alloc("p");
    public static final Var o = Var.alloc("o");

    private static final String POINT_URI_FMT = URI+"point:%.0f:%.0f";
    
    public static String urlStr(Coordinate p) {
        return String.format(POINT_URI_FMT, p.getX(), p.getY());
    }
    
    public static Resource urlOf(Coordinate p) {
        return ResourceFactory.createResource(urlStr(p));
    }
    
    public static Resource urlOf(FrontsCoordinate p) {
        return urlOf(p.getCoordinate());
    }
}