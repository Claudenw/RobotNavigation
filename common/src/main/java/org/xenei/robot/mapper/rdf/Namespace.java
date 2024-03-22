package org.xenei.robot.mapper.rdf;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.sparql.core.Var;
import org.xenei.robot.common.utils.RobutContext;
import org.xenei.robot.mapper.rdf.functions.Functions;

public class Namespace {
    public static final String URI = "urn:org.xenei.robut:";
    public static final String FUNC_URI = URI + "function:";
    public static final String MODEL_URI = URI + "model:";

    public static final Resource UnionModel = ResourceFactory.createResource("urn:x-arq:UnionGraph");
    public static final Resource BaseModel = ResourceFactory.createResource(MODEL_URI + "BaseModel");
    public static final Resource KnownModel = ResourceFactory.createResource(MODEL_URI + "KnownModel");
    public static final Resource PlanningModel = ResourceFactory.createResource(MODEL_URI + "PlanningModel");

    public static final Resource Coord = ResourceFactory.createResource(URI + "Coord");
    public static final Resource Obst = ResourceFactory.createResource(URI + "Obstacle");
    public static final Resource Path = ResourceFactory.createResource(URI + "Path");

    public static final Property x = ResourceFactory.createProperty(URI + "x");
    public static final Property y = ResourceFactory.createProperty(URI + "y");
    public static final Property distance = ResourceFactory.createProperty(URI + "distance");
    public static final Property point = ResourceFactory.createProperty(URI + "point");
    public static final Property visited = ResourceFactory.createProperty(URI + "visited");
    public static final Property isIndirect = ResourceFactory.createProperty(URI + "isIndirect");

    public static Resource overlapsF = ResourceFactory.createResource(FUNC_URI + "overlaps");
    public static Resource intersectsF = ResourceFactory.createResource(FUNC_URI + "intersects");
    public static Resource touchesF = ResourceFactory.createResource(FUNC_URI + "touches");
    public static Resource distanceF = ResourceFactory.createResource(FUNC_URI + "distance");
    public static Resource nearbyF = ResourceFactory.createResource(FUNC_URI + "nearby");
    public static Resource intersectDistF = ResourceFactory.createResource(FUNC_URI + "intersectionDist");

    public static final Var s = Var.alloc("s");
    public static final Var p = Var.alloc("p");
    public static final Var o = Var.alloc("o");

    public static void init(RobutContext context) {
        new WktDataType(context.geometryFactory, context.cache);
        new Functions().register();
    }

}