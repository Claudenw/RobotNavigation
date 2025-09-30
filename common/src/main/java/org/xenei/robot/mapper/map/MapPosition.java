package org.xenei.robot.mapper.map;

import org.locationtech.jts.geom.Coordinate;
import org.xenei.robot.common.mapping.Map;
import org.xenei.robot.common.utils.CoordUtils;

public class MapPosition extends MapLocation implements Map.Pos<MapLocation, MapPosition> {
    double heading;

    MapPosition(MapImpl map, Coordinate coordinate, double heading) {
        super(map, coordinate);
        this.heading = heading;
    }

    @Override
    public MapPosition buildPosition(Coordinate coordinate, double heading) {
        return new MapPosition(map, coordinate, heading);
    }

    @Override
    public MapLocation buildLocation(Coordinate coordinate) {
        return new MapLocation(map, coordinate);
    }

    @Override
    public double getHeading() {
        return heading;
    }

    @Override
    public String toString() {
        return String.format("MapPosition[ %s heading:%.4f (%.4f) ]", CoordUtils.toString(this.getCoordinate(), 4),
                getHeading(), Math.toDegrees(getHeading()));
    }
}
