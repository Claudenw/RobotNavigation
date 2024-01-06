package org.xenei.robot.mapper.rdf.functions;

import java.util.function.BiFunction;

import org.apache.jena.datatypes.TypeMapper;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.function.FunctionBase2;
import org.apache.jena.sparql.function.FunctionBase3;
import org.apache.jena.sparql.util.Context;
import org.locationtech.jts.geom.Geometry;
import org.xenei.robot.common.utils.RobutContext;
import org.xenei.robot.mapper.rdf.WktDataType;

public class FF_Nearby extends FunctionBase3 {
    protected RobutContext ctxt;
    protected WktDataType dataType;
    
    protected FF_Nearby() {
    }
    
    @Override
    public void build(String uri, ExprList args, Context context) {
        ctxt = context.get(RobutContext.symbol);
        dataType = (WktDataType) TypeMapper.getInstance().getTypeByClass(Geometry.class);
        
        if (ctxt == null) {
            throw new IllegalStateException("Robot context not set in Jena context");
        }
        checkBuild(uri, args) ;
    }
    
    @Override
    public NodeValue exec(NodeValue v1, NodeValue v2, NodeValue v3) {
        Geometry gw1 = dataType.parse(v1.getNode().getLiteralLexicalForm());
        Geometry gw2 = dataType.parse(v2.getNode().getLiteralLexicalForm());
        try {
            return NodeValue.makeBoolean(gw1.isWithinDistance(gw2, v3.getDouble()));
        } catch (Exception e) {
            throw new GeometryException(e);
        }
    }
}
