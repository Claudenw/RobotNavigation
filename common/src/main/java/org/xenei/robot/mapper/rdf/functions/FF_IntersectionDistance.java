package org.xenei.robot.mapper.rdf.functions;

import org.apache.jena.datatypes.TypeMapper;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.function.FunctionBase3;
import org.apache.jena.sparql.util.Context;
import org.locationtech.jts.geom.Geometry;
import org.xenei.robot.common.utils.RobutContext;
import org.xenei.robot.mapper.rdf.WktDataType;

public class FF_IntersectionDistance extends FunctionBase3 {

    private WktDataType dataType;
    
    protected FF_IntersectionDistance() {
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
    public NodeValue exec(NodeValue v1, NodeValue v2, NodeValue v3) {
        Geometry gw2 = dataType.parse(v2.getNode().getLiteralLexicalForm());
        Geometry gw3 = dataType.parse(v3.getNode().getLiteralLexicalForm());
        try {
            Geometry intersection = gw2.intersection(gw3);
            if (intersection.isEmpty()) {
                return NodeValue.makeDouble(Double.POSITIVE_INFINITY);
            }
            Geometry gw1 = dataType.parse(v1.getNode().getLiteralLexicalForm());
            return NodeValue.makeDouble(gw1.distance(intersection));
        } catch (Exception e) {
            throw new GeometryException(e);
        }
    }
}
