package org.xenei.robot.mapper.rdf.functions;

import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.function.FunctionBase3;
import org.locationtech.jts.geom.Geometry;

public class FF_Nearby extends FunctionBase3 {

    protected FF_Nearby() {
    }

    @Override
    public NodeValue exec(NodeValue v1, NodeValue v2, NodeValue v3) {
        Geometry gw1 = WktCache.INSTANCE.get(v1.getNode().getLiteralLexicalForm());
        Geometry gw2 = WktCache.INSTANCE.get(v2.getNode().getLiteralLexicalForm());
        try {
            return NodeValue.makeBoolean(gw1.isWithinDistance(gw2, v3.getDouble()));
        } catch (Exception e) {
            throw new GeometryException(e);
        }
    }
}
