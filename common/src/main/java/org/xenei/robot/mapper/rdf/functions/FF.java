package org.xenei.robot.mapper.rdf.functions;

import java.util.function.Function;

import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.function.FunctionBase1;
import org.locationtech.jts.geom.Geometry;

public class FF extends FunctionBase1 {

    private final Function<Geometry, NodeValue> func;

    protected FF(Function<Geometry, NodeValue> func) {
        this.func = func;
    }

    @Override
    public NodeValue exec(NodeValue v1) {
        Geometry gw1 = WktCache.INSTANCE.get(v1.getNode().getLiteralLexicalForm());
        try {
            return func.apply(gw1);
        } catch (Exception e) {
            throw new GeometryException(e);
        }
    }
}
