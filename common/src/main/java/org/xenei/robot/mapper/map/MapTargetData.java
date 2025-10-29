//package org.xenei.robot.mapper.map;
//
//import org.apache.jena.rdf.model.Resource;
//import org.apache.jena.rdf.model.Statement;
//import org.xenei.robot.common.planning.Segment;
//import org.xenei.robot.mapper.rdf.Namespace;
//
//public class MapTargetData implements org.xenei.robot.common.mapping.MapTargetData {
//    public final static String URI_FORMAT = Namespace.CLASS_URI + MapTargetData.class.getName() + ":%s:%s:%s:%s";
//    private final MapLocationImpl mapLocation;
//    private final Resource data;
//
//    MapTargetData(MapLocationImpl mapLocation, Resource resource) {
//        this.mapLocation = mapLocation;
//        if (resource.getModel() == null) {
//            throw new IllegalStateException(String.format("Resource %s has no model", resource));
//        }
//        data = resource;
//    }
//
//    @Override
//    public double distance() {
//        return data.getProperty(Namespace.distance).getLiteral().getDouble();
//    }
//
//    @Override
//    public boolean indirect() {
//        return data.getProperty(Namespace.isIndirect).getLiteral().getBoolean();
//    }
//
//    @Override
//    public MapLocationImpl getTarget() {
//        Statement statement = data.listProperties(Namespace.point)
//                .filterDrop(s -> s.getLiteral().equals(mapLocation.getUrn())).next();
//        Resource targetUrn = statement.getResource();
//        MapLocationImpl result = mapLocation.parseResource(targetUrn);
//        statement.changeObject(result.getUrn());
//        return result;
//    }
//
//    public Segment asSegment() {
//        return new MapSegment(mapLocation, this);
//    }
//}
