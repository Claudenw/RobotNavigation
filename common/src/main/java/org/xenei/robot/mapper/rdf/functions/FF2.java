package org.xenei.robot.mapper.rdf.functions;

import org.apache.jena.datatypes.TypeMapper;
import org.apache.jena.geosparql.implementation.UnitsConversionException;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.function.FunctionBase2;
import org.apache.jena.sparql.util.Context;
import org.locationtech.jts.geom.Geometry;
import org.opengis.geometry.MismatchedDimensionException;
import org.xenei.robot.common.utils.RobutContext;
import org.xenei.robot.mapper.rdf.WktDataType;
import java.util.function.BiFunction;
public class FF2 extends FunctionBase2 {
    
    protected RobutContext ctxt;
    protected WktDataType dataType;
    private final BiFunction<Geometry,Geometry,NodeValue> func;
    
    protected FF2(BiFunction<Geometry,Geometry,NodeValue> func) {
        this.func = func;
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


