package org.xenei.robot.common.utils;

import java.util.Collections;
import java.util.Map;

import org.apache.commons.collections4.map.LRUMap;
import org.apache.jena.riot.RIOT;
import org.apache.jena.sparql.util.Symbol;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.xenei.robot.common.ScaleInfo;
import org.xenei.robot.mapper.GraphGeomFactory;
import org.xenei.robot.mapper.rdf.Namespace;

public class RobutContext {
    
    public static final Symbol symbol = Symbol.create(RobutContext.class.getName());
    
    public final ScaleInfo scaleInfo;
    public final GeometryFactory geometryFactory;
    public final GeometryUtils geometryUtils;
    public final GraphGeomFactory graphGeomFactory;
    public final Map<String,Geometry> cache = Collections.synchronizedMap(new LRUMap<String,Geometry>(500));
    
    public RobutContext(ScaleInfo scaleInfo) {
        RIOT.getContext().put(symbol, this);
        this.scaleInfo = scaleInfo;
        this.geometryFactory = new GeometryFactory(scaleInfo.getPrecisionModel());
        this.geometryUtils = new GeometryUtils(this);
        this.graphGeomFactory = new GraphGeomFactory(geometryUtils);
        Namespace.init(this);
    }
}