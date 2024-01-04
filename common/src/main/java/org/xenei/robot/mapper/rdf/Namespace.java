package org.xenei.robot.mapper.rdf;

import java.util.UUID;

import org.apache.jena.datatypes.TypeMapper;
import org.apache.jena.geosparql.implementation.vocabulary.SpatialExtension;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.sparql.core.Var;

public class Namespace {
    public static final String URI = "urn:org.xenei.robot:";
    public static final Resource UnionModel = ResourceFactory.createResource("urn:x-arq:UnionGraph");
    public static final Resource BaseModel = ResourceFactory.createResource(URI + "model:BaseModel");
    public static final Resource PlanningModel = ResourceFactory.createResource(URI + "model:PlanningModel");
    public static final Resource Coord = ResourceFactory.createResource(URI + "Coord");
    public static final Resource Obst = ResourceFactory.createResource(URI + "Obstacle");
    public static final Resource Path = ResourceFactory.createResource(URI + "Path");
    public static final Resource Cluster = ResourceFactory.createResource(URI + "Cluster");

    public static final Property x = ResourceFactory.createProperty(URI + "x");
    public static final Property y = ResourceFactory.createProperty(URI + "y");
    public static final Property distance = ResourceFactory.createProperty(URI + "distance");
    public static final Property point = ResourceFactory.createProperty(URI + "point");
    public static final Property visited = ResourceFactory.createProperty(URI + "visited");
    public static final Property isIndirect = ResourceFactory.createProperty(URI + "isIndirect");

    // public static final Resource Point =
    // ResourceFactory.createResource(GeoSPARQL_URI.SF_URI+"Point");

    public static final Property nearbyF = ResourceFactory.createProperty(SpatialExtension.NEARBY_PROP);

    public static final Var s = Var.alloc("s");
    public static final Var p = Var.alloc("p");
    public static final Var o = Var.alloc("o");



}