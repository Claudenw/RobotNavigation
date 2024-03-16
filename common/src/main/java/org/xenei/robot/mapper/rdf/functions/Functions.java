package org.xenei.robot.mapper.rdf.functions;

import java.util.HashMap;
import java.util.Map;

import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.function.Function;
import org.apache.jena.sparql.function.FunctionFactory;
import org.apache.jena.sparql.function.FunctionRegistry;
import org.xenei.robot.mapper.rdf.Namespace;

public class Functions implements FunctionFactory {

    public Map<String,Function> map = new HashMap<String,Function>();
    
    public Functions() {
        add( Namespace.overlapsF, new FF2((x, y) -> NodeValue.makeBoolean(x.overlaps(y))));
        add( Namespace.intersectsF , new FF2((x, y) -> NodeValue.makeBoolean(x.intersects(y))));
        add( Namespace.touchesF, new FF2((x, y) -> NodeValue.makeBoolean(x.touches(y))));
        add( Namespace.distanceF, new FF2((x, y) -> NodeValue.makeDouble(x.distance(y))));
        add( Namespace.intersectDistF, new FF_IntersectionDistance());
        add( Namespace.nearbyF, new FF_Nearby() );
    }
    
    public void add(Resource url, Function func) {
        map.put(url.getURI(), func);
    }
    
    public void add(String url, Function func) {
        map.put(url, func);
    }

    @Override
    public Function create(String uri) {
        return map.get(uri);
    }
    
    public void register() {
        FunctionRegistry registry = FunctionRegistry.get();
        map.keySet().forEach(e -> registry.put(e, this));
    }
    
    public void remove() {
        FunctionRegistry registry = FunctionRegistry.get();
        map.keySet().forEach(e -> registry.remove(e));
    }
}
