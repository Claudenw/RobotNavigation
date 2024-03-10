package org.xenei.robot.common.mapping;

import org.apache.jena.rdf.model.Resource;
import org.locationtech.jts.geom.Geometry;
import org.xenei.robot.common.Location;

public class MapCoord {
    public final Resource resource;
    public final Location location;
    public final boolean isDirect; 
    public final Geometry geometry;
    
    public MapCoord(Resource resource, double x, double y, boolean isDirect, Geometry geometry) {
        this.resource = resource;
        location = Location.from(x,y);
        this.isDirect = isDirect;
        this.geometry = geometry;
    }

}
