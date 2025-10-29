//package org.xenei.robot.mapper.map;
//
//import org.locationtech.jts.geom.Coordinate;
//import org.xenei.robot.common.LocationI;
//import org.xenei.robot.common.utils.CoordUtils;
//
//public class MapPosition extends MapLocationImpl implements org.xenei.robot.common.mapping.MapPosition<MapLocationImpl, MapPosition> {
//    double heading;
//
//    MapPosition(MapImpl map, Coordinate coordinate, double heading) {
//        super(map, coordinate);
//        this.heading = heading;
//    }
//
//    @Override
//    public MapPosition buildPosition(LocationI<?> coordinate, double heading) {
//        return new MapPosition(map, coordinate.getCoordinate(), heading);
//    }
//
//    @Override
//    public MapLocationImpl buildLocation(Coordinate coordinate) {
//        return new MapLocationImpl(map, coordinate);
//    }
//
//    @Override
//    public double getHeading() {
//        return heading;
//    }
//
//    @Override
//    public String toString() {
//        return String.format("MapPosition[ %s heading:%.4f (%.4f) ]", CoordUtils.toString(this.getCoordinate(), 4),
//                getHeading(), Math.toDegrees(getHeading()));
//    }
//}
