package org.xenei.robot.mapper.rdf.functions;

import java.util.function.BiFunction;

import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.function.FunctionBase2;
import org.locationtech.jts.geom.Geometry;

public class FF2 extends FunctionBase2 {

    private final BiFunction<Geometry, Geometry, NodeValue> func;

    protected FF2(BiFunction<Geometry, Geometry, NodeValue> func) {
        this.func = func;
    }

    @Override
    public NodeValue exec(NodeValue v1, NodeValue v2) {
        Geometry gw1 = WktCache.INSTANCE.get(v1.getNode().getLiteralLexicalForm());
        Geometry gw2 = WktCache.INSTANCE.get(v2.getNode().getLiteralLexicalForm());
        try {
            return func.apply(gw1, gw2);
        } catch (Exception e) {
            throw new GeometryException(e);
        }
    }
}
