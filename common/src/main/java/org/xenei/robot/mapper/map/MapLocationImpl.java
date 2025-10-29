//package org.xenei.robot.mapper.map;
//
//import org.apache.jena.arq.querybuilder.AskBuilder;
//import org.apache.jena.arq.querybuilder.ExprFactory;
//import org.apache.jena.arq.querybuilder.SelectBuilder;
//import org.apache.jena.arq.querybuilder.UpdateBuilder;
//import org.apache.jena.arq.querybuilder.WhereBuilder;
//import org.apache.jena.geosparql.implementation.vocabulary.Geo;
//import org.apache.jena.rdf.model.Literal;
//import org.apache.jena.rdf.model.ModelFactory;
//import org.apache.jena.rdf.model.Resource;
//import org.apache.jena.rdf.model.ResourceFactory;
//import org.apache.jena.sparql.core.Var;
//import org.apache.jena.sparql.path.Path;
//import org.apache.jena.sparql.path.PathFactory;
//import org.apache.jena.update.UpdateRequest;
//import org.apache.jena.vocabulary.RDF;
//import org.locationtech.jts.geom.Coordinate;
//import org.locationtech.jts.geom.Geometry;
//import org.locationtech.jts.geom.OctagonalEnvelope;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.xenei.robot.common.UnmodifiableCoordinate;
//import org.xenei.robot.common.mapping.MapCoordinate;
//import org.xenei.robot.common.mapping.MapLocation;
//import org.xenei.robot.common.mapping.MapTargetData;
//import org.xenei.robot.common.planning.Segment;
//import org.xenei.robot.common.utils.AngleUtils;
//import org.xenei.robot.common.utils.CoordUtils;
//import org.xenei.robot.common.utils.RobutContext;
//import org.xenei.robot.mapper.rdf.Namespace;
//
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.Collection;
//import java.util.Collections;
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Set;
//import java.util.concurrent.CompletableFuture;
//
///**
// * The map coordinates for this implementation.
// */
//public class MapLocationImpl extends MapLocation implements GraphObject {
//    private static final Path PATH_QUERY_PREDICATE = PathFactory.pathOneOrMore1(PathFactory.pathLink(Namespace.path.asNode()));
//
//    private static final Logger LOG = LoggerFactory.getLogger(MapLocationImpl.class);
//    private static final String urnFmt = Namespace.CLASS_URI + MapLocationImpl.class.getName() + ":%s:%s";
//
//    private final MapTargetData selfTargetData = new MapTargetData() {
//        @Override
//        public MapLocationImpl getTarget() {
//            return MapLocationImpl.this;
//        }
//
//        @Override
//        public double distance() {
//            return 0;
//        }
//
//        @Override
//        public boolean indirect() {
//            return false;
//        }
//
//        @Override
//        public Segment asSegment() {
//            return new MapSegment(MapLocationImpl.this, this);
//        }
//    };
//
//    private final Resource urn;
//
//    @Override
//    public MapImpl getMap() {
//        return (MapImpl) super.getMap();
//    }
//
//    MapLocationImpl(MapImpl map, MapCoordinate coordinate) {
//        super(map, coordinate.getCoordinate());
//        double multiplier = Math.pow(10, map.getContext().scaleInfo.decimalPlaces());
//        long lX = (long) (getCoordinate().x * multiplier);
//        long lY = (long) (getCoordinate().y * multiplier);
//        Resource r = ResourceFactory.createResource(String.format(urnFmt, lX, lY));
//        if (map.ask(askExists(r))) {
//            urn = map.readSubModel(r).join();
//        } else {
//            urn = ModelFactory.createDefaultModel().createResource(r.getURI(), Namespace.Coord);
//        }
//        boolean dirty = false;
//        if (!urn.hasProperty(RDF.type)) {
//            urn.addProperty(RDF.type, Namespace.Coord);
//            dirty = true;
//        }
//        if (!urn.hasProperty(Namespace.x) || !urn.hasProperty(Namespace.y)) {
//            urn.addLiteral(Namespace.x, getCoordinate().x);
//            urn.addLiteral(Namespace.y, getCoordinate().y);
//            dirty = true;
//        }
//        if (!urn.hasProperty(Geo.AS_WKT_PROP)) {
//            Geometry geometry = map.getContext().geometryUtils.asPoint(getCoordinate());
//            urn.addLiteral(Geo.AS_WKT_PROP, map.getContext().graphGeomFactory.asWKT(geometry));
//            dirty = true;
//        }
//        if (!urn.hasProperty(Namespace.visited)) {
//            urn.addLiteral(Namespace.visited, false);
//            dirty = true;
//        }
//        if (dirty) {
//            map.updateSubModel(urn, false);
//        }
//    }
//
//     MapLocationImpl parseResource(Resource resource) {
//        String[] parts = resource.getURI().split(":");
//        double multiplier = Math.pow(10, getMap().getContext().scaleInfo.decimalPlaces());
//        double lX = Long.parseLong(parts[parts.length-2]) * multiplier;
//        double lY = Long.parseLong(parts[parts.length-1]) * multiplier;
//        return getMap().recordOnMapCoordinate(new Coordinate(lX, lY));
//    }
//
//    public Resource getUrn() {
//        return urn;
//    }
//
//    CompletableFuture<?> addPath(MapLocationImpl other) {
//        Resource otherUrn = other.getUrn();
//        other.urn.addProperty(Namespace.path, urn);
//        urn.addProperty(Namespace.path, otherUrn);
//        return getMap().doUpdate(new UpdateRequest().add(new UpdateBuilder()
//                .addInsert(Namespace.PlanningModel, otherUrn, Namespace.path, urn)
//                .addInsert(Namespace.PlanningModel, urn, Namespace.path, otherUrn)
//                        .build()
//                )
//        );
//    }
//
//    CompletableFuture<?> removeTarget(MapLocationImpl target) {
//        if (target.coordinate.equals2D(this.coordinate)) {
//            return CompletableFuture.completedFuture(null);
//        }
//        return getMap().deleteSubModel(createTargetUri(target.coordinate));
//    }
//
//    public void removeTargets(Collection<MapLocationImpl> deadTargets) {
//        deadTargets.forEach(this::removeTarget);
//    }
//
//    public CompletableFuture<MapTargetData> addTarget(MapLocationI<?> target) {
//        MapLocationImpl mapTarget = getMap().asMapLocation(target);
//        if (mapTarget.equals(this)) {
//            return CompletableFuture.completedFuture(selfTargetData);
//        }
//        return getTargetData(mapTarget);
//    }
//
//    private OctagonalEnvelope createBoundingBox(MapLocationImpl target) {
//        double theta = this.angleBetween(target);
//        double range = this.distance(target);
//
//        OctagonalEnvelope envelope = new OctagonalEnvelope(this.getCoordinate());
//        envelope.expandToInclude(this.plus(CoordUtils.fromAngle(theta + AngleUtils.RADIANS_45, range)));
//        envelope.expandToInclude(this.plus(CoordUtils.fromAngle(theta, 2 * range)));
//        envelope.expandToInclude(this.plus(CoordUtils.fromAngle(theta - AngleUtils.RADIANS_45, range)));
//        return envelope;
//    }
//
//    public CompletableFuture<List<org.xenei.robot.mapper.map.MapTargetData>> getSegmentCandidates(MapLocationImpl target) {
//        List<CompletableFuture<? extends MapTargetData>> futures = new ArrayList<>();
//        RobutContext ctxt = map.getContext();
//        OctagonalEnvelope envelope = this.createBoundingBox(target);
//        Literal boundingBox = ctxt.graphGeomFactory.asWKT(envelope.toGeometry(ctxt.geometryFactory));
//        ExprFactory exprF = new ExprFactory();
//        Var wkt = Var.alloc("wkt");
//        Var xVar = Var.alloc("x");
//        Var yVar = Var.alloc("y");
//
//        return map.exec(new SelectBuilder()
//                        .addVar(Namespace.s)
//                        .addGraph(Namespace.UnionModel, new WhereBuilder()
//                                .addWhere(Namespace.s, Geo.AS_WKT_PROP, wkt)
//                                .addWhere(Namespace.s, RDF.type, Namespace.Coord)
//                                .addWhere(Namespace.s, Namespace.x, xVar)
//                                .addWhere(Namespace.s, Namespace.y, yVar)
//                                .addFilter(ctxt.graphGeomFactory.isCoveredBy(map.exprF, wkt, boundingBox))
//                        ))
//                .thenApply(resultSet -> {
//                    resultSet.forEachRemaining(soln -> {
//                        futures.add(addTarget(map.asMapCoordinate(new Coordinate(soln.getLiteral("x").getDouble()
//                                , soln.getLiteral("y").getDouble()))));
//                    });
//                    return futures;
//                }).thenApply(comptLst -> {
//                            List<org.xenei.robot.mapper.map.MapTargetData> result = new ArrayList<>();
//                            comptLst.forEach(tdFuture -> {
//                                MapTargetData td = tdFuture.join();
//                                if (td.distance() > 0 && !td.indirect()) {
//                                    result.add(((org.xenei.robot.mapper.map.MapTargetData) td));
//                                }
//                            });
//                            return result;
//                        }
//                );
//    }
//
//    public CompletableFuture<List<MapLocationImpl>> getCandidateLocations(MapLocationImpl target) {
//        List<MapLocationImpl> futures = new ArrayList<>();
//        RobutContext ctxt = map.getContext();
//        OctagonalEnvelope envelope = this.createBoundingBox(target);
//        Literal boundingBox = ctxt.graphGeomFactory.asWKT(envelope.toGeometry(ctxt.geometryFactory));
//        ExprFactory exprF = new ExprFactory();
//        Var wkt = Var.alloc("wkt");
//        Var xVar = Var.alloc("x");
//        Var yVar = Var.alloc("y");
//
//        return map.exec(new SelectBuilder()
//                        .addVar(Namespace.s)
//                        .addGraph(Namespace.UnionModel, new WhereBuilder()
//                                .addWhere(Namespace.s, Geo.AS_WKT_PROP, wkt)
//                                .addWhere(Namespace.s, RDF.type, Namespace.Coord)
//                                .addWhere(Namespace.s, Namespace.x, xVar)
//                                .addWhere(Namespace.s, Namespace.y, yVar)
//                                .addFilter(ctxt.graphGeomFactory.isCoveredBy(map.exprF, wkt, boundingBox))
//                        ))
//                .thenApply(resultSet -> {
//                    resultSet.forEachRemaining(soln -> {
//                        futures.add(map.recordOnMapCoordinate(new Coordinate(soln.getLiteral("x").getDouble()
//                                , soln.getLiteral("y").getDouble())));
//                    });
//                    return futures;
//                });
//    }
//
//
//    // private Map.TargetData createTargetData(MapLocation target,
//    // Map.TargetData<Coordinate> value) {
//    // if (value != null) {
//    // return value;
//    // }
//    //
//    // SelectBuilder sb = new SelectBuilder().addVar(distanceV).addVar(indirectV)
//    // .addWhere(urn, Namespace.target, targetV).addWhere(targetV, Namespace.point,
//    // target.wkt)
//    // .addOptional(targetV, Namespace.distance, distanceV)
//    // .addOptional(targetV, Namespace.isIndirect, indirectV);
//    //
//    // return map.exec(sb).thenApply(rs -> {
//    // Double distance = null;
//    // Boolean indirect = null;
//    // if (rs.hasNext()) {
//    // QuerySolution solution = rs.next();
//    // Literal literal = solution.getLiteral(distanceV.getVarName());
//    // distance = (literal == null) ? null : literal.getDouble();
//    // literal = solution.getLiteral(indirectV.getVarName());
//    // indirect = (literal == null) ? null : literal.getBoolean();
//    // }
//    // if (distance == null) {
//    // distance = distance(target.getCoordinate());
//    // }
//    // if (indirect == null) {
//    // indirect = this.hasClearPath(target);
//    // }
//    // return new Map.TargetData<>(target, distance, indirect);
//    // }).join();
//    // }
//
//    @Override
//    public boolean isIndirect(MapLocationImpl target) {
//        return addTarget(target).join().indirect();
//    }
//
//    public Literal getWkt() {
//        return urn.getProperty(Geo.AS_WKT_PROP).getLiteral();
//    }
//
//    boolean isVisited() {
//        return urn.getProperty(Namespace.visited).getLiteral().getBoolean();
//    }
//
//    // CompletableFuture<MapLocation> updateValue(Property property, Object value) {
//    // return map
//    // .doUpdate(new UpdateRequest()
//    // .add(new UpdateBuilder().addDelete(Namespace.PlanningModel, urn, property,
//    // Namespace.o)
//    // .addGraph(Namespace.PlanningModel,
//    // new SelectBuilder().addWhere(urn, property, Namespace.o))
//    // .build())
//    // .add(new UpdateBuilder().addInsert(Namespace.PlanningModel, urn, property,
//    // value).build()))
//    // .thenApply(n -> this);
//    // }
//
//    // CompletableFuture<MapLocation>
//    // updateOrCreate(Supplier<CompletableFuture<MapLocation>> updateFunction) {
//    // return exists ? updateFunction.get() : update();
//    // }
//
//
//    public CompletableFuture<MapLocationImpl> setVisited() {
//        CompletableFuture<?> future = CompletableFuture.completedFuture(null);
//        if (!isVisited()) {
//            urn.removeAll(Namespace.visited);
//            urn.addLiteral(Namespace.visited, true);
//            future = map.updateSubModel(urn, false);
//        }
//        return future.thenApply(x -> this);
//    }
//
//    private AskBuilder askExists(Resource urn) {
//        return new AskBuilder().from(Namespace.UnionModel.getURI()).addWhere(urn, RDF.type, Namespace.Coord);
//    }
//
//    @Override
//    public String toString() {
//        return "MapLocation [" + CoordUtils.toString(getCoordinate(), 1) + "]";
//    }
//
//    // CompletableFuture<MapLocation> update() {
//    // UpdateRequest req = new UpdateRequest();
//    // req.add(new UpdateBuilder().addDelete(Namespace.PlanningModel, urn,
//    // Namespace.target, targetV)
//    // .addGraph(Namespace.PlanningModel, new SelectBuilder().addWhere(urn,
//    // Namespace.target, targetV))
//    // .build());
//    // org.apache.jena.rdf.model.Model model = ModelFactory.createDefaultModel();
//    // model.add(urn.getModel());
//    // for (Map.TargetData targetData : targets.values()) {
//    // Resource targ = model.createResource();
//    // model.add(urn, Namespace.target, targ);
//    // targ.addLiteral(Namespace.point, targetData.target().wkt);
//    // targ.addLiteral(Namespace.distance, targetData.distance());
//    // targ.addLiteral(Namespace.isIndirect, targetData.indirect());
//    // }
//    // req.add(new UpdateBuilder().addInsert(Namespace.PlanningModel,
//    // model).build());
//    // return map.doUpdate(req).thenApply(rs -> this);
//    // }
//
//    private boolean calcIntersects(MapLocationImpl target) {
//        RobutContext ctxt = map.getContext();
//        // create a path between the two points.
//        Literal pathWkt = ctxt.graphGeomFactory.asWKTPath(ctxt.chassisInfo.radius, this.getCoordinate(),
//                target.getCoordinate());
//        Var wkt = Var.alloc("wkt");
//        OctagonalEnvelope envelope = this.createBoundingBox(target);
//        Literal boundingBox = ctxt.graphGeomFactory.asWKT(envelope.toGeometry(ctxt.geometryFactory));
//
//        // see if the path intersects any obstacles.
//        boolean intersects = map.ask(new AskBuilder().from(Namespace.UnionModel.getURI()) //
//                .addWhere(Namespace.s, RDF.type, Namespace.Obst) //
//                .addWhere(Namespace.s, Geo.AS_WKT_PROP, wkt)
//                .addFilter(ctxt.graphGeomFactory.intersects(map.exprF, wkt, boundingBox))
//                .addFilter(ctxt.graphGeomFactory.intersects(map.exprF, pathWkt, wkt)));
//
//        LOG.debug("calcIntersects from {} to {}: {}", this, target, intersects);
//        return intersects;
//    }
//
//    public boolean hasClearPath(MapLocationImpl target) {
//        return !getTargetData(target).join().indirect();
//    }
//
//    @Override
//    public boolean hasPath(MapLocationImpl mapLocation) {
//        if (isIndirect(mapLocation)) {
//            return true;
//        }
//        AskBuilder ask = new AskBuilder().addGraph(Namespace.UnionModel,
//                new WhereBuilder().addWhere(getUrn(), PATH_QUERY_PREDICATE, mapLocation.getUrn()));
//        return getMap().ask(ask);
//    }
//
//    private Resource createTargetUri(Coordinate target) {
//        Coordinate[] coords = new Coordinate[] {getCoordinate(), target};
//        Arrays.sort(coords, CoordUtils.XYCompr);
//        double multiplier = Math.pow(10, map.getContext().scaleInfo.decimalPlaces());
//        long lX = (long) (coords[0].x * multiplier);
//        long lY = (long) (coords[0].y * multiplier);
//        long tX = (long) (coords[1].x * multiplier);
//        long tY = (long) (coords[1].y * multiplier);
//        return ResourceFactory.createResource(String.format(org.xenei.robot.mapper.map.MapTargetData.URI_FORMAT, lX, lY, tX, tY));
//    }
//
//    public CompletableFuture<MapTargetData> getTargetData(MapLocationImpl target) {
//        return map.readSubModel(target.getUrn())
//                .thenApply(data -> {
//                    if (data.getModel().isEmpty()) {
//                        data.addProperty(RDF.type, Namespace.TargetData);
//                        data.addLiteral(Namespace.point, target.getUrn());
//                        data.addLiteral(Namespace.point, MapLocationImpl.this.getUrn());
//                        data.addLiteral(Namespace.distance, MapLocationImpl.this.distance(target));
//                        data.addLiteral(Namespace.isIndirect, calcIntersects(target));
//                        return map.updateSubModel(data);
//                    } else {
//                        return CompletableFuture.completedFuture(data);
//                    }
//                }).thenCompose(future ->
//                        future.thenApply(resource -> new org.xenei.robot.mapper.map.MapTargetData(this, resource)));
//    }
//
//    public PathImpl getPath(MapLocationImpl target) {
//        if (!isIndirect(target)) {
//            return (PathImpl) getMap().asPath(Arrays.asList(this, target));
//        }
//        RobutContext ctxt = map.getContext();
//        Var sWkt = Var.alloc("swkt");
//        Var oWkt = Var.alloc("swkt");
//        OctagonalEnvelope envelope = createBoundingBox(target);
//        Literal boundingBox = ctxt.graphGeomFactory.asWKT(envelope.toGeometry(ctxt.geometryFactory));
//        HashMap<MapLocationImpl, Set<MapLocationImpl>> segmentPairs = new HashMap<>();
//        /*
//        Select segments from within the bounding box.
//         */
//        map.exec(new SelectBuilder()
//                        .addVar(Namespace.s).addVar(Namespace.o)
//                        .addWhere(Namespace.s, Namespace.path, Namespace.o)
//                        .addFilter(map.exprF.lt(Namespace.s, Namespace.o))
//                        .addWhere(Namespace.s, Geo.AS_WKT_PROP, sWkt)
//                        .addFilter(ctxt.graphGeomFactory.intersects(map.exprF, sWkt, boundingBox))
//                        .addWhere(Namespace.o, Geo.AS_WKT_PROP, oWkt)
//                        .addFilter(ctxt.graphGeomFactory.intersects(map.exprF, oWkt, boundingBox)))
//                .thenAccept(rs -> {
//                    rs.forEachRemaining(qs -> {
//                        segmentPairs.computeIfAbsent(parseResource(qs.getResource(Namespace.s.getName())), k ->
//                                new HashSet<MapLocationImpl>()).add(parseResource(qs.getResource(Namespace.o.getName())));
//
//                    });
//                }).join();
//
//        /*
//         * set all distance to infinity
//         * set distance for start = 0;
//         * add start to visited list
//         * for every start.targetData.isDirect and targetData.target not in visited list, add targetData.target to queue with distance from start
//         * find the shortest distance in the queue
//         *
//         * record distance and parent.
//         * stop when shortest distance MapLocation = target
//         */
//        Set<MapLocationImpl> seen = new HashSet<>();
//        HashMap<MapLocationImpl, PartialPath> partialPathMap = new HashMap<>();
//        segmentPairs.keySet().stream().map(k -> k.equals(this) ? new PartialPath(k, 0, null) :
//                        new PartialPath(k, Double.POSITIVE_INFINITY, null))
//                .forEach(partialPath -> partialPathMap.put(partialPath.source, partialPath));
//
//        List<PartialPath> nodeList = new ArrayList<>(partialPathMap.values());
//
//        // dijkstra's algorithm
//        while (!segmentPairs.isEmpty()) {
//            nodeList.sort((a, b) -> Double.compare(a.cost, b.cost));
//            PartialPath pathSegment = nodeList.stream().filter(p -> seen.contains(p.source)).findFirst().get();
//            seen.add(pathSegment.source);
//            Set<MapLocationImpl> adjacentNodes = segmentPairs.get(pathSegment.source);
//            for (MapLocationImpl adjacent : adjacentNodes) {
//                double cost = pathSegment.cost + pathSegment.source.getTargetData(adjacent).join().distance();
//                PartialPath adjacentPath = partialPathMap.get(adjacent);
//                if (adjacentPath.cost > cost) {
//                    adjacentPath.cost = cost;
//                    adjacentPath.parent = pathSegment.source;
//                    if (adjacentPath.source.equals(target)) {
//                        List<MapLocationImpl> segments = new ArrayList<>();
//                        segments.add(adjacentPath.source);
//                        while (adjacentPath.parent != null) {
//                            adjacentPath = partialPathMap.get(adjacentPath.parent);
//                            segments.add(adjacentPath.source);
//                        }
//                        Collections.reverse(segments);
//                        return new PathImpl(this.getMap(), segments);
//                    }
//                }
//            }
//            segmentPairs.remove(pathSegment.source);
//        }
//        throw new IllegalStateException("No path found");
//    }
//
//    private static class PartialPath implements Comparable<PartialPath> {
//        private MapLocationImpl source;
//        private double cost;
//        private MapLocationImpl parent;
//
//        PartialPath(MapLocationImpl source, double cost, MapLocationImpl parent) {
//            this.source = source;
//            this.cost = cost;
//            this.parent = parent;
//        }
//
//        @Override
//        public int compareTo(PartialPath o) {
//            return Double.compare(cost, o.cost);
//        }
//    }
//}
