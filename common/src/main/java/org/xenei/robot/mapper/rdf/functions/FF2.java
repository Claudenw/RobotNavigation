package org.xenei.robot.mapper.rdf.functions;

import java.util.function.BiFunction;

import org.apache.jena.datatypes.TypeMapper;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.function.FunctionBase2;
import org.apache.jena.sparql.util.Context;
import org.locationtech.jts.geom.Geometry;
import org.xenei.robot.common.utils.RobutContext;
import org.xenei.robot.mapper.rdf.WktDataType;

public class FF2 extends FunctionBase2 {

    private final BiFunction<Geometry, Geometry, NodeValue> func;
    private WktDataType dataType;

    protected FF2(BiFunction<Geometry, Geometry, NodeValue> func) {
        this.func = func;
    }
    

    @Override
    public void build(String uri, ExprList args, Context context) {
       
        dataType = (WktDataType) TypeMapper.getInstance().getTypeByClass(Geometry.class);
        
        if (context.get(RobutContext.symbol) == null) {
            throw new IllegalStateException("Robot context not set in Jena context");
        }
        checkBuild(uri, args) ;
    }

    @Override
    public NodeValue exec(NodeValue v1, NodeValue v2) {
        Geometry gw1 = dataType.parse(v1.getNode().getLiteralLexicalForm());
        Geometry gw2 = dataType.parse(v2.getNode().getLiteralLexicalForm());
        try {
            return func.apply(gw1, gw2);
        } catch (Exception e) {
            throw new GeometryException(e);
        }
    }
}
