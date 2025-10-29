//package org.xenei.robot.mapper.map;
//
//import org.locationtech.jts.geom.Geometry;
//import org.xenei.robot.common.UnmodifiableCoordinate;
//import org.xenei.robot.common.mapping.MapTargetData;
//import org.xenei.robot.common.planning.Segment;
//
//public class MapSegment implements Segment {
//    final UnmodifiableCoordinate coordinate;
//    final Geometry geometry;
//    final double distance;
//    final boolean indirect;
//
//    public MapSegment(MapLocationImpl location, MapTargetData targetData) {
//        coordinate = targetData.getTarget().getCoordinate();
//        geometry = location.getMap().getContext().geometryUtils.asLine(location.getCoordinate(), coordinate);
//        distance = targetData.distance();
//        indirect = targetData.indirect();
//    }
//
//    @Override
//    public int compareTo(Segment o) {
//        return Segment.COMPARATOR.compare(this, o);
//    }
//
//    @Override
//    public double cost() {
//        double result = distance;
//        if (indirect) {
//            result *= 2;
//        }
//        return result;
//    }
//
//    @Override
//    public double distance() {
//        return distance;
//    }
//
//    @Override
//    public UnmodifiableCoordinate getCoordinate() {
//        return coordinate;
//    }
//
//    @Override
//    public Geometry getGeometry() {
//        return geometry;
//    }
//
//    @Override
//    public String toString() {
//        return "Segment[" + geometry.toString() + "]";
//    }
//}
