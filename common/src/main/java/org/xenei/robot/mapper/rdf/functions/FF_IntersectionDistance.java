package org.xenei.robot.mapper.rdf.functions;

import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.function.FunctionBase3;
import org.locationtech.jts.geom.Geometry;

public class FF_IntersectionDistance extends FunctionBase3 {

    protected FF_IntersectionDistance() {
    }

    @Override
    public NodeValue exec(NodeValue v1, NodeValue v2, NodeValue v3) {
        Geometry gw2 = WktCache.INSTANCE.get(v2.getNode().getLiteralLexicalForm());
        Geometry gw3 = WktCache.INSTANCE.get(v3.getNode().getLiteralLexicalForm());
        try {
            Geometry intersection = gw2.intersection(gw3);
            if (intersection.isEmpty()) {
                return NodeValue.makeDouble(Double.POSITIVE_INFINITY);
            }
            Geometry gw1 = WktCache.INSTANCE.get(v1.getNode().getLiteralLexicalForm());
            return NodeValue.makeDouble(gw1.distance(intersection));
        } catch (Exception e) {
            throw new GeometryException(e);
        }
    }
}
