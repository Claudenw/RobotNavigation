package org.xenei.robot.common.mapping;

import org.locationtech.jts.geom.Geometry;
import org.xenei.robot.common.Location;

public class MapCoord {
    public final Location location;
    public final boolean isDirect; 
    public final Geometry geometry;
    
    public MapCoord(double x, double y, boolean isDirect, Geometry geometry) {
        location = Location.from(x,y);
        this.isDirect = isDirect;
        this.geometry = geometry;
    }

}
