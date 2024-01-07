package org.xenei.robot.common.mapping;

import java.util.UUID;

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Resource;
import org.locationtech.jts.geom.Geometry;

public interface Obstacle {
    Literal wkt();
    
    Geometry geom();
    
    UUID uuid();
    
    Resource rdf();
}
