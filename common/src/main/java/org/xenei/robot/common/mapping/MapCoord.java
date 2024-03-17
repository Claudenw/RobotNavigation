package org.xenei.robot.common.mapping;

import org.locationtech.jts.geom.Geometry;
import org.xenei.robot.common.Location;

public class MapCoord {
    public final Location location;
    public final boolean isIndirect; 
    public final Geometry geometry;
    
    public MapCoord(double x, double y, boolean isIndirect, Geometry geometry) {
        location = Location.from(x,y);
        this.isIndirect = isIndirect;
        this.geometry = geometry;
    }

}
