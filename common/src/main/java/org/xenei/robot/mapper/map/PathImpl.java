//package org.xenei.robot.mapper.map;
//
//import org.apache.jena.arq.querybuilder.AskBuilder;
//import org.apache.jena.arq.querybuilder.UpdateBuilder;
//import org.apache.jena.arq.querybuilder.WhereBuilder;
//import org.apache.jena.geosparql.implementation.vocabulary.Geo;
//import org.apache.jena.graph.Node;
//import org.apache.jena.graph.Triple;
//import org.apache.jena.rdf.model.Literal;
//import org.apache.jena.rdf.model.Resource;
//import org.apache.jena.rdf.model.ResourceFactory;
//import org.apache.jena.sparql.path.Path;
//import org.apache.jena.sparql.path.PathFactory;
//import org.apache.jena.vocabulary.RDF;
//import org.locationtech.jts.geom.Coordinate;
//import org.locationtech.jts.geom.Geometry;
//import org.locationtech.jts.geom.LineString;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.xenei.robot.common.mapping.MapPath;
//import org.xenei.robot.common.mapping.MapPathI;
//import org.xenei.robot.common.planning.Segment;
//import org.xenei.robot.mapper.SegmentImpl;
//import org.xenei.robot.mapper.rdf.Namespace;
//
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.List;
//import java.util.concurrent.CompletableFuture;
//
//public class PathImpl extends MapPath {
//    private static final Logger LOG = LoggerFactory.getLogger(PathImpl.class);
//    private final MapImpl map;
//    private final Literal wkt;
//    private final LineString geometry;
//    private final List<MapLocationImpl> pointsList;
//
//    private static final Path PATH_QUERY_PREDICATE = PathFactory.pathOneOrMore1(PathFactory.pathLink(Namespace.path.asNode()));
//
//    PathImpl(MapImpl map, MapLocationImpl start, MapLocationImpl end) {
//        this(map, Arrays.asList(start, end));
//    }
//
//    PathImpl(MapImpl map, final List<MapLocationImpl> coords) {
//        super(coords);
//        this.map = map;
//        pointsList = new ArrayList<>(coords);
//
////        List<CompletableFuture<?>> futures = new ArrayList<>();
////        UpdateBuilder updateBuilder = new UpdateBuilder();
////        MapLocation lastLoc = path.get(0);
////        for (int i = 1; i < path.size(); i++) {
////            MapLocation nextLoc = path.get(i);
////            futures.add(lastLoc.addPath(nextLoc));
////            lastLoc = nextLoc;
////        }
//        Coordinate[] points = pointsList.stream().map(MapLocationImpl::getCoordinate).toArray(Coordinate[]::new);
//        geometry = map.getContext().geometryFactory.createLineString(points);
//        wkt = map.getContext().graphGeomFactory.asWKT(geometry);
//        //CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[0])).join();
//        LOG.debug("Path <{} {}>", points[0], points[points.length - 1]);
//    }
//
//    CompletableFuture<PathImpl> update(final Resource model) {
//        Node tn = ResourceFactory.createResource().asNode();
//        List<Triple> triples = new ArrayList<>();
//        triples.add(Triple.create(tn, RDF.type.asNode(), Namespace.Path.asNode()));
//        triples.add(Triple.create(tn, Geo.AS_WKT_PROP.asNode(), wkt.asNode()));
//        return map.doUpdate(new UpdateBuilder().addInsert(model, triples).buildRequest()).thenApply(n -> this);
//    }
//
//    public static boolean hasPath(MapLocationImpl a, MapLocationImpl b) {
//        AskBuilder ask = new AskBuilder().addGraph(Namespace.UnionModel, new WhereBuilder().addWhere(a.getUrn(), PATH_QUERY_PREDICATE, b.getUrn()));
//        return a.getMap().ask(ask);
//    }
//
//    public Literal getWkt() {
//        return wkt;
//    }
//
//    @Override
//    public Geometry getGeometry() {
//        return geometry;
//    }
//
//    @Override
//    public Segment getSegment(MapLocationImpl startPosition) {
//        for (int i = 0; i < pointsList.size() - 1; i++) {
//            if (pointsList.get(i).equals(startPosition)) {
//                MapLocationImpl point = pointsList.get(i + 1);
//                double distance = startPosition.distance(point);
//                return SegmentImpl.builder().setCoordinate(point)
//                        .setDistance(distance)
//                        .setCost(distance)
//                        .build(map.getContext());
//
//            }
//        }
//        return null;
//    }
//}
