//package org.xenei.robot.mapper.map;
//
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.Collection;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Optional;
//import java.util.Set;
//import java.util.concurrent.CompletableFuture;
//import java.util.function.Consumer;
//import java.util.stream.Collectors;
//
//import org.apache.jena.arq.querybuilder.AskBuilder;
//import org.apache.jena.arq.querybuilder.ConstructBuilder;
//import org.apache.jena.arq.querybuilder.Converters;
//import org.apache.jena.arq.querybuilder.ExprFactory;
//import org.apache.jena.arq.querybuilder.Order;
//import org.apache.jena.arq.querybuilder.SelectBuilder;
//import org.apache.jena.arq.querybuilder.UpdateBuilder;
//import org.apache.jena.arq.querybuilder.WhereBuilder;
//import org.apache.jena.geosparql.implementation.vocabulary.Geo;
//import org.apache.jena.geosparql.implementation.vocabulary.GeoSPARQL_URI;
//import org.apache.jena.geosparql.implementation.vocabulary.SRS_URI;
//import org.apache.jena.geosparql.spatial.SpatialIndex;
//import org.apache.jena.geosparql.spatial.SpatialIndexException;
//import org.apache.jena.query.Dataset;
//import org.apache.jena.query.DatasetFactory;
//import org.apache.jena.query.QueryExecution;
//import org.apache.jena.query.QueryExecutionFactory;
//import org.apache.jena.query.QuerySolution;
//import org.apache.jena.query.ResultSet;
//import org.apache.jena.rdf.model.Literal;
//import org.apache.jena.rdf.model.Model;
//import org.apache.jena.rdf.model.ModelFactory;
//import org.apache.jena.rdf.model.Resource;
//import org.apache.jena.shared.Lock;
//import org.apache.jena.shared.PrefixMapping;
//import org.apache.jena.sparql.core.Var;
//import org.apache.jena.sparql.expr.Expr;
//import org.apache.jena.update.UpdateExecutionFactory;
//import org.apache.jena.update.UpdateRequest;
//import org.apache.jena.vocabulary.RDF;
//import org.apache.sis.util.collection.WeakValueHashMap;
//import org.locationtech.jts.geom.Coordinate;
//import org.locationtech.jts.geom.Geometry;
//import org.locationtech.jts.geom.LineString;
//import org.locationtech.jts.geom.Point;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.xenei.robot.common.mapping.MapCoordinate;
//import org.xenei.robot.common.GeometricObject;
//import org.xenei.robot.common.ObstacleI;
//import org.xenei.robot.common.mapping.Map;
//import org.xenei.robot.common.mapping.MapObstacle;
//import org.xenei.robot.common.mapping.MapPath;
//import org.xenei.robot.common.mapping.MapPosition;
//import org.xenei.robot.common.planning.Solution;
//import org.xenei.robot.common.utils.CoordUtils;
//import org.xenei.robot.common.utils.DoubleUtils;
//import org.xenei.robot.common.utils.RobutContext;
//import org.xenei.robot.mapper.PointCloudSorter;
//import org.xenei.robot.mapper.rdf.Namespace;
//
//public class MapImpl extends Map {
//    private static final Logger LOG = LoggerFactory.getLogger(MapImpl.class);
//    private final Dataset data;
//    final ExprFactory exprF;
//    private final ObstacleHandler obstacleHandler;
//
//    private final static Map.MapObjectFactory FACTORY = new Map.MapObjectFactory() {
//
//        @Override
//        public org.xenei.robot.common.mapping.MapLocation createLocation(MapCoordinate coord) {
//            return new MapLocationImpl(this, coord);
//        }
//
//        @Override
//        public MapObstacle createObstacle(ObstacleI obstacle) {
//            return return new MapObstacle(this, obstacle);
//        }
//
//        @Override
//        public MapPath createPath(org.xenei.robot.common.mapping.MapLocation... coords) {
//            return new MapPath(this, coords);
//        }
//
//        @Override
//        public MapPosition createPosition(MapCoordinate coord, double heading) {
//            return new MapPosition(this, coord, heading);
//        }
//    };
//
//    public static PrefixMapping getPrefixes() {
//        return PrefixMapping.Factory.create().setNsPrefixes(GeoSPARQL_URI.getPrefixes())
//                .setNsPrefixes(PrefixMapping.Standard).setNsPrefix("robut", Namespace.URI);
//    }
//
//    MapLocation recordOnMapCoordinate(Coordinate onMap) {
//        return COORDINATE_MAPLOCATION_MAP.compute(onMap,
//                (k, v) -> v == null ? new MapLocationImpl(MapImpl.this, onMap) : v);
//    }
//
//    public Coordinate scaleCoordinate(Coordinate coordinate) {
//        return this.getContext().scaleInfo.scale(coordinate);
//    }
//
//    @Override
//    public PathImpl asPath(Collection<? extends MapLocationImpl> points) {
//        return new PathImpl(this, points);
//    }
//
//    @Override
//    public MapLocationImpl asMapLocation(MapCoordinate coordinate) {
//        if (coordinate == null) {
//            return null;
//        }
//        if (coordinate instanceof MapLocationImpl) {
//            return (MapLocationImpl) coordinate;
//        }
//        Coordinate onMap = scaleCoordinate(coordinate.getCoordinate());
//        return recordOnMapCoordinate(onMap);
//    }
//
//    public MapLocationImpl asMapCoordinate(Coordinate coordinate) {
//        if (coordinate == null) {
//            return null;
//        }
//        Coordinate onMap = this.getContext().scaleInfo.round(coordinate);
//        return recordOnMapCoordinate(onMap);
//    }
//
//    @Override
//    public org.xenei.robot.mapper.map.MapPosition asMapPosition(MapPosition<?, ?> p) {
//        if (p == null) {
//            return null;
//        }
//        if (p instanceof org.xenei.robot.mapper.map.MapPosition) {
//            return (org.xenei.robot.mapper.map.MapPosition) p;
//        }
//        return new org.xenei.robot.mapper.map.MapPosition(this, p.getCoordinate(), p.getHeading());
//    }
//
//    public MapObstacleImpl asMapObstacle(ObstacleI obstacle) {
//        if (obstacle == null) {
//            return null;
//        }
//        if (obstacle instanceof MapObstacleImpl) {
//            return (MapObstacleImpl) obstacle;
//        }
//        return new MapObstacleImpl(ctxt, obstacle);
//    }
//
//    public MapImpl(RobutContext ctxt) {
//        super(ctxt, FACTORY)
//        this.ctxt = ctxt;
//        data = DatasetFactory.create();
//        data.getDefaultModel().setNsPrefixes(getPrefixes());
//        data.addNamedModel(Namespace.BaseModel, defaultModel());
//        data.addNamedModel(Namespace.PlanningModel, defaultModel());
//        data.addNamedModel(Namespace.KnownModel, defaultModel());
//        exprF = new ExprFactory(data.getPrefixMapping());
//
//        try {
//            SpatialIndex.buildSpatialIndex(data, SRS_URI.DEFAULT_WKT_CRS84);
//        } catch (SpatialIndexException e) {
//            throw new RuntimeException(e);
//        }
//        obstacleHandler = new ObstacleHandler();
//    }
//
//    /**
//     * Alias for {@link #updateSubModel(Resource, boolean)} with forceComplete =
//     * {@code false}.
//     *
//     * @param resource
//     *            the resource to write. Must have a {@link Model} attached.
//     * @return a CompletableFuture containing the resource.
//     */
//    CompletableFuture<Resource> updateSubModel(Resource resource) {
//        return updateSubModel(resource, false);
//    }
//
//    /**
//     * Updates the Resource in the PlanningModel to contain all the data associated
//     * with the resource. The resource must have a model attached.
//     *
//     * @param resource
//     *            the resource to write. Must have a {@link Model} attached.
//     * @param forceComplete
//     *            if {@code true} the CompletableFuture will not return until the
//     *            update is complete. if {@code false} the update will occur in the
//     *            background.
//     * @return a CompletableFuture containing the resource.
//     */
//    CompletableFuture<Resource> updateSubModel(Resource resource, boolean forceComplete) {
//        if (resource.getModel() == null) {
//            throw new IllegalStateException(String.format("Resource %s must have model", resource));
//        }
//        UpdateRequest req = new UpdateRequest()
//                .add(new UpdateBuilder().addDelete(Namespace.PlanningModel, resource, Namespace.p, Namespace.o)
//                        .addGraph(Namespace.PlanningModel, new WhereBuilder()
//                        .addWhere(resource, Namespace.p, Namespace.o)).build())
//                .add(new UpdateBuilder().addInsert(Namespace.PlanningModel, resource.getModel()).build());
//        CompletableFuture<?> future = doUpdate(req);
//        return forceComplete ? future.thenApply(x -> resource) : CompletableFuture.completedFuture(resource);
//    }
//
//    /**
//     * Read the resource and all properties and return it as a Resource with a
//     * {@link Model} containing all the properties attached.
//     *
//     * @param resource
//     *            the Resource to read.
//     * @return a CompletableFuture containing the Resource with the Model.
//     */
//    CompletableFuture<Resource> readSubModel(Resource resource) {
//        return construct(new ConstructBuilder().addConstruct(resource, Namespace.p, Namespace.o)
//                .addGraph(Namespace.UnionModel, new WhereBuilder().addWhere(resource, Namespace.p, Namespace.o)))
//                .thenApply(m -> m.getResource(resource.getURI()));
//    }
//
//    /**
//     * Read the resource and all properties and return it as a Resource with a
//     * {@link Model} containing all the properties attached.
//     *
//     * @param resource
//     *            the Resource to read.
//     * @return a CompletableFuture containing the Resource with the Model.
//     */
//    CompletableFuture<Resource> deleteSubModel(Resource resource) {
//        return doUpdate(new UpdateBuilder().addDelete(Namespace.PlanningModel, resource, Namespace.p, Namespace.o)
//                .addWhere(resource, Namespace.p, Namespace.o)).thenApply(m -> resource);
//    }
//
//
//    @Override
//    public void clear(String namedGraph) {
//        try (LockHandler ignored = new LockHandler(Lock.WRITE)) {
//            if (namedGraph.equals(Namespace.UnionModel.getURI())) {
//                data.getDefaultModel().removeAll();
//                data.replaceNamedModel(Namespace.BaseModel, defaultModel());
//                data.replaceNamedModel(Namespace.PlanningModel, defaultModel());
//            } else {
//                data.replaceNamedModel(namedGraph, defaultModel());
//            }
//        }
//    }
//
//    @Override
//    public RobutContext getContext() {
//        return ctxt;
//    }
//
//    public static PrefixMapping getPrefixMapping() {
//        PrefixMapping pm = PrefixMapping.Factory.create();
//        pm.setNsPrefixes(GeoSPARQL_URI.getPrefixes());
//        pm.setNsPrefixes(PrefixMapping.Standard.getNsPrefixMap());
//        pm.setNsPrefix("robut", "urn:org.xenei.robot:");
//        return pm;
//    }
//
//    private static Model defaultModel() {
//        return ModelFactory.createDefaultModel().setNsPrefixes(getPrefixMapping());
//    }
//
//    public boolean isEmpty() {
//        try (LockHandler ignored = new LockHandler(Lock.READ)) {
//            return data.getUnionModel().isEmpty();
//        }
//    }
//
//    private CompletableFuture<?> doUpdate(UpdateBuilder update) {
//        return ctxt.submit(() -> {
//            try (LockHandler ignored = new LockHandler(Lock.WRITE)) {
//                UpdateExecutionFactory.create(update.build(), data).execute();
//            }
//        });
//    }
//
//    CompletableFuture<?> doUpdate(UpdateRequest request) {
//        return ctxt.submit(() -> {
//            try (LockHandler ignored = new LockHandler(Lock.WRITE)) {
//                UpdateExecutionFactory.create(request, data).execute();
//            }
//        });
//    }
//
//    public boolean ask(AskBuilder ask) {
//        try (LockHandler ignored = new LockHandler(Lock.READ);
//                QueryExecution exec = QueryExecutionFactory.create(ask.build(), data)) {
//            return exec.execAsk();
//        }
//    }
//
//    public void dump(Resource modelName, Consumer<Model> consumer) {
//        try (LockHandler ignored = new LockHandler(Lock.READ)) {
//            consumer.accept(data.getNamedModel(modelName));
//        }
//    }
//
//    /**
//     * executes the select query and processes the result with the processor.
//     * Processing stops when processor returns false.
//     *
//     * @param select
//     *            the SelectBuilder to execute.
//     */
//    public CompletableFuture<ResultSet> exec(SelectBuilder select) {
//        return ctxt.submit(() -> {
//            try (LockHandler ignored = new LockHandler(Lock.READ);
//                    QueryExecution qexec = QueryExecutionFactory.create(select.build(), data)) {
//                return qexec.execSelect().materialise();
//            }
//        });
//    }
//
//    CompletableFuture<Model> construct(ConstructBuilder select) {
//        return ctxt.submit(() -> {
//            try (LockHandler ignore = new LockHandler(Lock.READ);
//                    QueryExecution qexec = QueryExecutionFactory.create(select.build(), data)) {
//                return qexec.execConstruct();
//            }
//        });
//    }
//
//    // @Override
//    // public CompletableFuture<Optional<Step>> addCoord(Coordinate coord, Double
//    // distance, boolean visited, Boolean isIndirect) {
//    // MapCoordinate mapCoord = new MapCoordinate(coord);
//    // UpdateRequest req = new UpdateRequest();
//    // Resource graphCoord = null;
//    // WhereBuilder where = new WhereBuilder().addWhere(Namespace.s, RDF.type,
//    // Namespace.Coord)
//    // .addWhere(Namespace.s, Geo.AS_WKT_PROP,
//    // ctxt.graphGeomFactory.asWKT(mapCoord.getCoordinate()));
//    // if (exists(mapCoord, Namespace.Coord)) {
//    // UpdateBuilder newDat = new UpdateBuilder().addWhere(where);
//    // if (distance != null) {
//    // newDat.addInsert(Namespace.PlanningModel, Namespace.s, Namespace.distance,
//    // distance);
//    // }
//    // if (visited) {
//    // newDat.addInsert(Namespace.PlanningModel, Namespace.s, Namespace.visited,
//    // visited);
//    // }
//    // if (isIndirect != null && isIndirect) {
//    // newDat.addInsert(Namespace.PlanningModel, Namespace.s, Namespace.isIndirect,
//    // isIndirect);
//    // }
//    // // clear and set existing value
//    // req.add(new UpdateBuilder().addDelete(Namespace.PlanningModel, Namespace.s,
//    // Namespace.distance, Namespace.o)
//    // .addWhere(Namespace.s, Namespace.distance,
//    // Namespace.o).addWhere(where).build())
//    // .add(newDat.build());
//    // } else {
//    // // no existing record
//    // graphCoord = ctxt.graphGeomFactory.asRDF(mapCoord, Namespace.Coord);
//    // if (distance != null) {
//    // graphCoord.addLiteral(Namespace.distance, distance);
//    // }
//    // if (visited) {
//    // graphCoord.addLiteral(Namespace.visited, visited);
//    // }
//    // if (isIndirect != null && isIndirect) {
//    // graphCoord.addLiteral(Namespace.isIndirect, isIndirect);
//    // }
//    // req.add(new UpdateBuilder().addInsert(Namespace.PlanningModel,
//    // graphCoord.getModel()).build());
//    // }
//    //
//    // return doUpdate(req).thenApply( x -> {
//    // LOG.debug("Added {} for {}", mapCoord, coord);
//    // if (distance == null || distance <= 0) {
//    // return Optional.empty();
//    // }
//    // return
//    // Optional.of(StepImpl.builder().setCoordinate(mapCoord).setDistance(distance)
//    // .setCost(isIndirect != null && isIndirect ? distance * 2 :
//    // distance).build(ctxt));
//    // });
//    // }
//
////    @Override
////    public CompletableFuture<MapLocation> addCoord(final FrontsCoordinate coord, final FrontsCoordinate target,
////            final boolean visited) {
////        final MapLocation mapCoord = asMapCoordinate(coord);
////        CompletableFuture<?> future = visited ? mapCoord.setVisited() : CompletableFuture.completedFuture(null);
////        if (target != null) {
////            future.thenApply(x -> mapCoord.addTarget(target));
////        }
////        return future.thenApply(x -> mapCoord);
////    }
//
//    private CompletableFuture<MapObstacleImpl> addObstacle(Geometry geometry) {
//        return ctxt.submit(() -> obstacleHandler.addObstacle(new MapObstacleImpl(ctxt, geometry)));
//    }
//
//    @Override
//    public boolean isObstacle(MapCoordinate point) {
//        return obstacleHandler.isObstacle(point);
//    }
//
//    @Override
//    public CompletableFuture<Set<MapObstacleImpl>> getObstacles() {
//        return obstacleHandler.getObstacles();
//    }
//
////    /**
////     * Gets the Step for the coordinates.
////     *
////     * @param location
////     *            The location to get the Step for
////     * @return the Step for the location.
////     */
////    public CompletableFuture<Optional<Segment>> getStep(double costToLocation, FrontsCoordinate location) {
////        MapLocation coordinate = asMapLocation(location);
////
////        Var geom = Var.alloc("geom");
////        Var dist = Var.alloc("dist");
////        Var indirect = Var.alloc("indirect");
////        Var indirectFlg = Var.alloc("indirectFlg");
////        Var cost = Var.alloc("cost");
////
////        SelectBuilder sb = new SelectBuilder().addVar(cost).addVar(dist).addVar(geom) //
////                .from(Namespace.PlanningModel.getURI()) //
////                .addWhere(Namespace.s, RDF.type, Namespace.Coord) //
////                .addWhere(Namespace.s, Namespace.distance, dist) //
////                .addWhere(Namespace.s, Namespace.x, coordinate.getX()) //
////                .addWhere(Namespace.s, Namespace.y, coordinate.getY()) //
////                .addWhere(Namespace.s, Geo.AS_WKT_PROP, geom) //
////                .addOptional(Namespace.s, Namespace.isIndirect, indirect) //
////                .addBind(exprF.cond(exprF.bound(indirect), exprF.asExpr(indirect), exprF.asExpr(false)), indirectFlg)
////                .addBind(SPARQL.costCalc(costToLocation, dist, indirectFlg), cost);
////
////        SegmentImpl.Builder builder = SegmentImpl.builder();
////
////        // Predicate<QuerySolution> processor = soln -> {
////        // Geometry geometry =
////        // ctxt.graphGeomFactory.fromWkt(soln.getLiteral(geom.getName()));
////        // builder.setCoordinate(coordinate).setCost(soln.getLiteral(cost.getName()).getDouble())
////        // .setDistance(soln.getLiteral(dist.getName()).getDouble()).setGeometry(geometry);
////        // return false;
////        // };
////
////        return exec(sb).thenApply(resultSet -> {
////            if (resultSet.hasNext()) {
////                QuerySolution soln = resultSet.next();
////                Geometry geometry = ctxt.graphGeomFactory.fromWkt(soln.getLiteral(geom.getName()));
////                builder.setCoordinate(coordinate).setCost(soln.getLiteral(cost.getName()).getDouble())
////                        .setDistance(soln.getLiteral(dist.getName()).getDouble()).setGeometry(geometry);
////            }
////            return builder.isValid(ctxt) ? Optional.of(builder.build(ctxt)) : Optional.empty();
////        });
////        // return new ChainedFuture<>(exec(sb, processor), () ->builder.isValid(ctxt) ?
////        // Optional.of(builder.build(ctxt)) : Optional.empty());
////    }
//
//    /**
//     * Add the plan record to the map
//     *
//     * @param model
//     *            the model name to add the path to.
//     * @param coords
//     *            the coordinates of the path.
//     * @return An array of coordinates on the path.
//     */
//    @Override
//    public CompletableFuture<PathImpl> addPath(Resource model, MapCoordinate... coords) {
//        return new PathImpl(this, Arrays.stream(coords).map(this::asMapLocation).collect(Collectors.toList())).update(model);
//    }
//
//    @Override
//    public CompletableFuture<?> cutPath(Resource model, MapCoordinate a, MapCoordinate b) {
//        Var ra = Var.alloc("a");
//        Var rb = Var.alloc("b");
//        MapLocationImpl mapA = asMapLocation(a);
//        MapLocationImpl mapB = asMapLocation(b);
//
//        UpdateBuilder ub = new UpdateBuilder().addDelete(model, Namespace.s, Namespace.p, Namespace.o)
//                .addWhere(Namespace.s, Namespace.p, Namespace.o).addWhere(Namespace.s, Namespace.point, ra)
//                .addWhere(Namespace.s, Namespace.point, rb).addWhere(ra, Namespace.x, mapA.getX())
//                .addWhere(ra, Namespace.y, mapA.getY()).addWhere(rb, Namespace.x, mapB.getX())
//                .addWhere(rb, Namespace.y, mapB.getY());
//        return doUpdate(ub);
//    }
//
//    @Override
//    public boolean hasPath(MapCoordinate a, MapCoordinate b) {
//        return PathImpl.hasPath(asMapLocation(a), asMapLocation(b));
//    }
//
//    @Override
//    public boolean isClearPath(MapCoordinate from, MapCoordinate target) {
//        return asMapLocation(from).hasClearPath(asMapLocation(target));
//    }
//
////    /**
////     * Calculate the best next position based on the map and current coordinates.
////     *
////     * @param currentCoords
////     *            the current coordinates
////     * @return Optional containing either the PlanRecord for the next position, or
////     *         empty if none found.
////     */
////    @Override
////    public Optional<Segment> getBestSegment(FrontsCoordinate currentCoords, FrontsCoordinate targetCoords) {
////        MapLocation location = asMapLocation(currentCoords);
////        MapLocation mapTarget = asMapLocation(targetCoords);
////        List<? extends Map.TargetData> candidates = location.getSegmentCandidates(mapTarget).join();
////        if (candidates.isEmpty()) {
////            return Optional.empty();
////        }
////        Map.TargetData solution = candidates.get(0);
////        double cost = solution.asSegment().cost() + asMapLocation((solution.getTarget())).addTarget(mapTarget).join().asSegment().cost();
////        double newCost;
////        for (int i = 1 ; i < candidates.size() ; i++) {
////            Map.TargetData candidate = candidates.get(i);
////            newCost = candidate.asSegment().cost() + asMapLocation((candidate.getTarget())).addTarget(mapTarget).join().asSegment().cost();
////            if (newCost < cost) {
////                solution = candidate;
////                cost = newCost;
////            }
////        }
////        return Optional.of(solution.asSegment());
////        return segments.isEmpty() ? Optional.empty() : Optional.of(segments.get(0));
////        if (data.isEmpty()) {
////            LOG.debug("No map points");
////            return Optional.empty();
////        }
////
////        System.out.println(MapReports.dumpModel(this));
////        StepQuery stepQuery = new StepQuery(currentCoords);
////        ExtendedIterator<SegmentImpl.Builder> iter = stepQuery.execute().join();
////        if (iter.hasNext()) {
////            if (LOG.isDebugEnabled()) {
////                LOG.debug("Query\n{}", MapReports.dumpQuery(MapImpl.this, stepQuery.query));
////                LOG.debug("Distance\n{}", MapReports.dumpDistance(MapImpl.this, currentCoords));
////                LOG.debug("Obstacles\n{}", MapReports.dumpObstacleDistance(MapImpl.this));
////                MapImpl.this.getObstacles().thenAccept(obs -> obs.forEach(s -> LOG.debug(s.toString()))).join();
////                LOG.debug("Model\n{}", MapReports.dumpModel(MapImpl.this));
////                LOG.debug("No Selected map points");
////            }
////            return Optional.of(iter.next().build(ctxt));
////        }
////        return Optional.empty();
////    }
//
//    @Override
//    public void setVisited(MapCoordinate coord) {
//        asMapLocation(coord).setVisited();
//    }
//
//    @Override
//    public MapLocationImpl recalculate(MapCoordinate target) {
//        LOG.debug("recalculate: {}", target);
//        Var distance = Var.alloc("distance");
//        Var wkt = Var.alloc("wkt");
//        MapLocationImpl result = asMapLocation(target);
//        Literal targ = ctxt.graphGeomFactory.asWKT(result.getCoordinate());
//        // remove all the distance and adjustments and then calculate the distance to
//        // the new target
//
//        UpdateRequest req = new UpdateRequest().add(new UpdateBuilder() //
//                .addDelete(Namespace.PlanningModel, Namespace.s, Namespace.p, Namespace.o) //
//                .addGraph(Namespace.PlanningModel, new WhereBuilder() //
//                        .addWhere(Namespace.s, Namespace.p, Namespace.o) //
//                        .addFilter(exprF.in(Namespace.p, exprF.asList(Namespace.isIndirect, Namespace.distance))) //
//                ).build()) //
//                .add(new UpdateBuilder() //
//                        .addInsert(Namespace.PlanningModel, Namespace.s, Namespace.distance, distance) //
//                        .addGraph(Namespace.UnionModel, new WhereBuilder() //
//                                .addWhere(Namespace.s, RDF.type, Namespace.o) //
//                                .addFilter(exprF.in(Namespace.o, exprF.asList(Namespace.Coord, Namespace.Path))) //
//                                .addWhere(Namespace.s, Geo.AS_WKT_PROP, wkt) //
//                                .addBind(ctxt.graphGeomFactory.calcDistance(exprF, targ, wkt), distance))
//                        .build());
//
//        getMapLocations().thenAccept(c -> c.stream().filter(ml -> !isClearPath(ml, target))
//                .forEach(ml -> req.add(new UpdateBuilder() //
//                        .addInsert(Namespace.PlanningModel, Namespace.s, Namespace.isIndirect, true) //
//                        .addGraph(Namespace.UnionModel, new WhereBuilder() //
//                                .addWhere(Namespace.s, Geo.AS_WKT_PROP, ml.getWkt())) //
//                        .build())));
//
//        doUpdate(req);
//        return result;
//    }
//
//    @Override
//    public CompletableFuture<Collection<MapLocationImpl>> getMapLocations() {
//        Var x = Var.alloc("x");
//        Var y = Var.alloc("y");
//        Var wkt = Var.alloc("wkt");
//        Var indirect = Var.alloc("indirect");
//
//        SelectBuilder sb = new SelectBuilder().addVar(x).addVar(y).addVar(indirect).addVar(wkt) //
//                .from(Namespace.PlanningModel.getURI()) //
//                .addWhere(Namespace.s, RDF.type, Namespace.Coord) //
//                .addOptional(Namespace.s, Namespace.isIndirect, indirect) //
//                .addWhere(Namespace.s, Geo.AS_WKT_PROP, wkt) //
//                .addWhere(Namespace.s, Namespace.x, x) //
//                .addWhere(Namespace.s, Namespace.y, y);
//
//        List<MapLocationImpl> result = new ArrayList<>();
//
//        Consumer<QuerySolution> processor = soln -> result.add(this.asMapCoordinate(
//                new Coordinate(soln.getLiteral(x.getName()).getDouble(), soln.getLiteral(y.getName()).getDouble())));
//
//        return exec(sb).thenApply(resultSet -> {
//            resultSet.forEachRemaining(processor);
//            return result;
//        });
//
//    }
//
////    @Override
////    public CompletableFuture<List<Segment>> getSegments(FrontsCoordinate currentPosition) {
////        return asMapLocation(currentPosition).getSegments();
////    }
//
//    @Override
//    public CompletableFuture<PathImpl> recordSolution(Solution solution) {
//        solution.simplify(this::isClearPath);
//        return new PathImpl(this, solution.stream().map(this::asMapLocation).toList()).update(Namespace.BaseModel);
//    }
//
//    Model getModel() {
//        return data.getUnionModel();
//    }
//
//    @Override
//    public CompletableFuture<Void> updateIsIndirect(MapCoordinate finalTarget, Set<MapObstacleI> newObstacles) {
//        Var isIndirect = Var.alloc("isIndirect");
//        Var wkt = Var.alloc("wkt");
//        Var x = Var.alloc("x");
//        Var y = Var.alloc("y");
//
//        // find all the coordinates that are not indirect.
//        SelectBuilder sb = new SelectBuilder().addVar(x).addVar(y).setDistinct(true) //
//                .addGraph(Namespace.UnionModel, new WhereBuilder() //
//                        .addWhere(Namespace.s, RDF.type, Namespace.Coord) //
//                        .addWhere(Namespace.s, Namespace.x, x) //
//                        .addWhere(Namespace.s, Namespace.y, y) //
//                        .addOptional(Namespace.s, Namespace.isIndirect, isIndirect) //
//                        .addFilter(exprF.not(exprF.bound(isIndirect))));
//
//        return exec(sb).thenApply(resultSet -> {
//            List<MapLocationImpl> updateCoords = new ArrayList<>();
//            // find all the coordinates that are now blocked by one of the new obstacles.
//            resultSet.forEachRemaining(soln -> {
//                MapLocationImpl c = this.asMapCoordinate(new Coordinate(soln.getLiteral(x.getName()).getDouble(),
//                        soln.getLiteral(y.getName()).getDouble()));
//
//                Geometry path = ctxt.geometryUtils.asPath(ctxt.chassisInfo.radius, c.getCoordinate(),
//                        finalTarget.getCoordinate());
//                for (MapObstacleI obst : newObstacles) {
//                    if (path.distance(obst.getGeometry()) == 0) {
//                        updateCoords.add(c);
//                        break;
//                    }
//                }
//            });
//            return updateCoords;
//        }).thenAccept(updateCoords -> {
//            // update the coordinates to be indirect.
//            if (updateCoords.isEmpty()) {
//                CompletableFuture.completedFuture(null);
//            }
//            UpdateBuilder ub = new UpdateBuilder()
//                    .addInsert(Namespace.PlanningModel, Namespace.s, Namespace.isIndirect, Boolean.TRUE) //
//                    .addGraph(Namespace.UnionModel, new WhereBuilder() //
//                            .addWhere(Namespace.s, RDF.type, Namespace.Coord) //
//                            .addWhere(Namespace.s, Geo.AS_WKT_NODE, wkt)
//                            .addFilter(exprF.in(wkt, updateCoords.stream().map(MapLocationImpl::getWkt).toArray())));
//            doUpdate(ub);
//        });
//    }
//
//    @Override
//    public CompletableFuture<MapObstacleImpl> createObstacle(Coordinate location) {
//        return addObstacle(ctxt.geometryUtils.asPoint(new Coordinate(location.getX(), location.getY())));
//    }
//
//    @Override
//    public CompletableFuture<MapObstacleImpl> createObstacle(MapPosition<?, ?> startPosition, MapCoordinate relativeStart,
//                                                             MapCoordinate relativeEnd) {
//        return createObstacle(startPosition.nextPosition(relativeStart).getCoordinate(),
//                startPosition.nextPosition(relativeEnd).getCoordinate());
//    }
//
//    @Override
//    public CompletableFuture<MapObstacleImpl> createObstacle(Coordinate first, Coordinate last) {
//        List<LineString> lst = new ArrayList<>();
//        Position p = new Position(first, CoordUtils.calcHeading(first, last));
//        Position p2;
//        do {
//            p2 = p.nextPosition(ctxt.scaleInfo.getResolution());
//            lst.add(ctxt.geometryUtils.asLine(p, p2));
//            p = p2;
//        } while (p.distance(last) > ctxt.scaleInfo.getResolution());
//        lst.add(ctxt.geometryUtils.asLine(p.getCoordinate(), last));
//        return addObstacle(ctxt.geometryFactory.createMultiLineString(lst.toArray(new LineString[0])));
//    }
//
//    @Override
//    public CompletableFuture<MapObstacleImpl> createObstacle(MapPosition<?, ?> startPosition,
//                                                             MapCoordinate relativeCoordinate) {
//        return addObstacle(ctxt.geometryUtils.asPoint(asMapPosition(startPosition).nextPosition(relativeCoordinate)));
//    }
//
//    private class LockHandler implements AutoCloseable {
//        Lock lock;
//
//        private LockHandler(boolean flag) {
//            lock = data.getLock();
//            lock.enterCriticalSection(flag);
//        }
//
//        @Override
//        public void close() {
//            lock.leaveCriticalSection();
//        }
//    }
//
//    @Override
//    public CompletableFuture<Optional<MapCoordinate>> look(MapPosition<?, ?> from, double heading, int maxRange) {
//        org.xenei.robot.mapper.map.MapPosition mapPosition = asMapPosition(from);
//        Coordinate target = mapPosition.plus(CoordUtils.fromAngle(heading, maxRange));
//
//        Literal pathWkt = ctxt.graphGeomFactory.asWKTString(mapPosition.getCoordinate(), target);
//        Var wkt = Var.alloc("wkt");
//        Var dist = Var.alloc("dist");
//        Literal fromWkt = ctxt.graphGeomFactory.asWKT(mapPosition.getCoordinate());
//        SelectBuilder look = new SelectBuilder().addVar(dist).from(Namespace.UnionModel.getURI()) //
//                .addWhere(Namespace.s, RDF.type, Namespace.Obst) //
//                .addWhere(Namespace.s, Geo.AS_WKT_PROP, wkt) //
//                .addBind(ctxt.graphGeomFactory.calcIntersectionDistance(exprF, fromWkt, pathWkt, wkt), dist) //
//                .addFilter(ctxt.graphGeomFactory.intersects(exprF, pathWkt, wkt)) //
//                .addFilter(exprF.lt(dist, maxRange)) //
//                .addOrderBy(dist, Order.ASCENDING) //
//                .setLimit(1);
//
//        return exec(look).thenApply(resultSet -> {
//            double range = resultSet.hasNext() ? resultSet.next().getLiteral(dist.getName()).getDouble() : -1.0;
//            MapLocationImpl location = null;
//            if (range > -1) {
//                location = asMapCoordinate(CoordUtils.fromAngle(heading - mapPosition.getHeading(), range));
//            }
//            if (LOG.isDebugEnabled()) {
//                LOG.debug("Looking {} ({}) from {} returned {}", heading, Math.toDegrees(heading), mapPosition,
//                        location);
//            }
//            return Optional.ofNullable(location);
//        });
//    }
//
//    private static class SPARQL {
//
//        /**
//         * Calculate the cost from a position to the target
//         *
//         * @param posDistToTarget
//         *            distance from position to target
//         * @param indirect
//         *            true if there is an obstacle in the way.
//         * @return the expression to calculate the distance.
//         */
//        static Expr indirectCalc(Object posDistToTarget, Object indirect) {
//            ExprFactory exprF = new ExprFactory(getPrefixes());
//            return exprF.add(exprF.cond(exprF.bound(indirect), exprF.asExpr(posDistToTarget), exprF.asExpr(0)),
//                    posDistToTarget);
//        }
//
//        /**
//         * Calculates the cost to the target via the position.
//         *
//         * @param distToPos
//         *            the distance to the position.
//         * @param posDistToTarget
//         *            the distance from the position to the target
//         * @param indirect
//         *            true if there is an obstacle in the way.
//         * @return the expression to calcualte the cost.
//         */
//        static Expr costCalc(Object distToPos, Var posDistToTarget, Var indirect) {
//            ExprFactory exprF = new ExprFactory(getPrefixes());
//            return exprF.add(distToPos, SPARQL.indirectCalc(posDistToTarget, indirect));
//        }
//    }
//
////    public class StepQuery {
////        Literal wkt;
////
////        static Var cost = Var.alloc("cost");
////
////        // distance current to other
////        static Var dist = Var.alloc("dist");
////
////        static Var other = Var.alloc("other");
////        // wkt of other
////        static Var otherWkt = Var.alloc("otherWkt");
////        static Var indirectFlg = Var.alloc("indirectFlg");
////        // distance from other to target
////        static Var otherDist = Var.alloc("otherDist");
////
////        static Var indirect = Var.alloc("indirect");
////        static Var visited = Var.alloc("visited");
////        static Var other2 = Var.alloc("other2");
////        static Var other2Wkt = Var.alloc("other2Wkt");
////
////        final SelectBuilder query;
////        final AskBuilder checkVisited;
////        final Function<ResultSet, ExtendedIterator<SegmentImpl.Builder>> processor;
////
////        StepQuery(FrontsCoordinate currentCoords) {
////            MapLocation mapCoords = asMapCoordinate(currentCoords);
////            wkt = ctxt.graphGeomFactory.asWKT(mapCoords.getCoordinate());
////
////            query = new SelectBuilder().addVar(indirectFlg).addVar(cost).addVar(otherWkt).addVar(other).addVar(dist) //
////                    .from(Namespace.UnionModel.getURI()) //
////                    .addWhere(Namespace.s, RDF.type, Namespace.Coord) //
////                    .addWhere(Namespace.s, Geo.AS_WKT_PROP, wkt).addWhere(other, RDF.type, Namespace.Coord) //
////                    .addOptional(other, Namespace.isIndirect, indirect) //
////                    .addWhere(other, Geo.AS_WKT_PROP, otherWkt) //
////                    .addWhere(other, Namespace.distance, otherDist) //
////                    .addOptional(other, Namespace.visited, visited) //
////                    .addFilter(exprF.and(exprF.ne(other, Namespace.s), exprF.not(exprF.bound(visited)))) //
////                    .addBind(ctxt.graphGeomFactory.calcDistance(exprF, otherWkt, wkt), dist) //
////                    .addBind(SPARQL.costCalc(dist, otherDist, indirect), cost) //
////                    .addBind(exprF.cond(exprF.bound(indirect), exprF.asExpr(indirect), exprF.asExpr(false)),
////                            indirectFlg)
////                    .addOrderBy(indirectFlg, Order.ASCENDING).addOrderBy(cost, Order.ASCENDING);
////
////            // skip coords that are within the tolerance range of visited coords
////            // returns true if the position has been visited.
////            checkVisited = new AskBuilder() //
////                    .addWhere(other2, Namespace.visited, "?ignore") //
////                    .addWhere(other2, RDF.type, Namespace.Coord)//
////                    .addWhere(other2, Geo.AS_WKT_PROP, other2Wkt) //
////                    .addFilter(exprF.le(ctxt.graphGeomFactory.calcDistance(exprF, otherWkt, other2Wkt),
////                            ctxt.chassisInfo.radius)) //
////            ;
////
////            Predicate<QuerySolution> filter = soln -> {
////                Geometry geom = ctxt.graphGeomFactory.fromWkt(soln.getLiteral(otherWkt.getName()));
////                for (Coordinate candidate : geom.getCoordinates()) {
////                    Literal candidateWkt = ctxt.graphGeomFactory.asWKT(candidate);
////                    checkVisited.setVar(otherWkt, candidateWkt);
////                    // if not visited and has a clear path
////                    return (!ask(checkVisited) && isClearPath(currentCoords, asMapCoordinate(candidate)));
////                }
////                return false;
////            };
////
////            processor = resultSet -> WrappedIterator.create(resultSet).mapWith(soln -> {
////                Geometry geom = ctxt.graphGeomFactory.fromWkt(soln.getLiteral(otherWkt.getName()));
////                for (Coordinate candidate : geom.getCoordinates()) {
////                    Literal candidateWkt = ctxt.graphGeomFactory.asWKT(candidate);
////                    checkVisited.setVar(otherWkt, candidateWkt);
////                    // if not visited and has a clear path
////                    if (!ask(checkVisited) && isClearPath(currentCoords, asMapCoordinate(candidate))) {
////                        return SegmentImpl.builder().setCoordinate(candidate)
////                                .setCost(soln.getLiteral(cost.getName()).getDouble())
////                                .setDistance(soln.getLiteral(dist.getName()).getDouble()).setGeometry(geom);
////                    }
////                }
////                return null;
////            }).filterDrop(Objects::isNull).filterKeep(builder -> builder.isValid(ctxt));
////        }
////
////        public CompletableFuture<ExtendedIterator<SegmentImpl.Builder>> execute() {
////            return exec(query).thenApply(processor);
////        }
////    }
//
//    // package private for testing.
//    // class ObstacleImpl implements Obstacle {
//    // private final Literal wkt;
//    // private final Geometry geom;
//    // private final UUID uuid;
//    // private Resource rdf;
//    //
//    // ObstacleImpl(Geometry geom) {
//    // this(UUID.randomUUID(), geom);
//    // }
//    //
//    // ObstacleImpl(UUID uuid, Geometry geom) {
//    // this.uuid = uuid;
//    // this.geom = geom;
//    // this.wkt = ctxt.graphGeomFactory.asWKT(geom);
//    // }
//    //
//    // ObstacleImpl(Resource rdf, Literal wkt) {
//    // this.rdf = rdf;
//    // this.uuid = parseUUID(rdf);
//    // this.geom = ctxt.graphGeomFactory.fromWkt(wkt);
//    // this.wkt = wkt;
//    // }
//    //
//    // ObstacleImpl(MapPosition startPosition, FrontsCoordinate relativeStart,
//    // FrontsCoordinate relativeEnd) {
//    // Loc start = startPosition.nextPosition(relativeStart);
//    // Loc end = startPosition.nextPosition(relativeEnd);
//    // double d = start.distance(end);
//    // int parts = (int) (d / (ctxt.scaleInfo.getResolution() / 2));
//    // double xIncr = (end.getX() - start.getX()) / (parts + 1);
//    // double yIncr = (end.getY() - start.getY()) / (parts + 1);
//    // Coordinate[] part = new Coordinate[parts + 1];
//    // part[0] = start.getCoordinate();
//    // for (int i = 1; i < parts; i++) {
//    // part[i] = new Coordinate(part[i - 1].x + xIncr, part[i - 1].y + yIncr);
//    // }
//    // part[parts] = end.getCoordinate();
//    // geom = ctxt.geometryUtils.asLine(part);
//    // wkt = ctxt.graphGeomFactory.asWKT(geom);
//    // uuid = UUID.randomUUID();
//    // }
//    //
//    // ObstacleImpl(MapPosition startPosition, FrontsCoordinate relativeLocation) {
//    // MapPosition absoluteObstacle = startPosition.nextPosition(relativeLocation);
//    // if (Double.isNaN(absoluteObstacle.getHeading())) {
//    // LOG.error("Heading is NAN");
//    // }
//    // absoluteObstacle = new MapPosition(absoluteObstacle.getMap(),
//    // ctxt.scaleInfo.round(absoluteObstacle.getCoordinate()),
//    // absoluteObstacle.getHeading());
//    // geom = ctxt.geometryUtils.asPoint(absoluteObstacle);
//    // wkt = ctxt.graphGeomFactory.asWKT(geom);
//    // uuid = UUID.randomUUID();
//    // }
//    //
//    // private static UUID parseUUID(Resource rdf) {
//    // return UUID.fromString(rdf.getURI().substring("urn:uuid:".length()));
//    // }
//    //
//    // @Override
//    // public Literal wkt() {
//    // return wkt;
//    // }
//    //
//    // @Override
//    // public Geometry geometry() {
//    // return geom;
//    // }
//    //
//    // @Override
//    // public UUID uuid() {
//    // return uuid;
//    // }
//    //
//    // @Override
//    // public Resource rdf() {
//    // Resource result = rdf;
//    // if (result == null) {
//    // result = rdf = ResourceFactory.createResource("urn:uuid:" +
//    // uuid().toString());
//    // }
//    // return result;
//    // }
//    //
//    // @Override
//    // public int hashCode() {
//    // return Obstacle.hashCode(this);
//    // }
//    //
//    // @Override
//    // public boolean equals(Object obj) {
//    // return Obstacle.equalsImpl(this, obj);
//    // }
//    //
//    // @Override
//    // public String toString() {
//    // return wkt.getLexicalForm();
//    // }
//    // }
//
//    /**
//     * Create a cloud of obstacle points in a single geometry.
//     */
//    private class ObstacleHandler {
//        private RobutContext ctxt = MapImpl.this.ctxt;
//
//        private Geometry makeCloud(MapObstacleI obstacle, Collection<? extends MapObstacleI> others) {
//            Set<Point> points = new HashSet<>(obstacle.getPoints());
//            others.stream().map(GeometricObject::getPoints).forEach(points::addAll);
//            return new PointCloudSorter(ctxt.scaleInfo.getResolution() * DoubleUtils.SQRT2).process(points);
//        }
//
//        private MapObstacleImpl mergeIntersectOrTouch(MapObstacleImpl obstacle, Set<MapObstacleImpl> solns) {
//            UpdateRequest req = new UpdateRequest();
//            Geometry result = makeCloud(obstacle, solns);
//
//            for (MapObstacleImpl obst : solns) {
//                req.add(new UpdateBuilder().addDelete(Namespace.PlanningModel, obst.rdf(), Namespace.p, Namespace.o)
//                        .addGraph(Namespace.UnionModel,
//                                new WhereBuilder().addWhere(obst.rdf(), Namespace.p, Namespace.o))
//                        .build());
//            }
//            Model merged = ModelFactory.createDefaultModel();
//            MapObstacleImpl obst = new MapObstacleImpl(ctxt, result);
//            obst.in(merged);
//            req.add(new UpdateBuilder().addInsert(Namespace.PlanningModel, merged).build());
//            doUpdate(req);
//
//            return obst;
//        }
//
//        /**
//         * Creates adds an obstacle to the map.
//         *
//         * @param mapObstacle
//         *            the obstacle to add.
//         * @return
//         */
//        MapObstacleImpl addObstacle(MapObstacleImpl mapObstacle) {
//            // find all Obstacles that this obstacle will intersect or touch
//            // if there are any, merge them together.
//            // if not just write this on to the graph.
//            Var wkt = Var.alloc("wkt");
//
//            SelectBuilder selectBuilder = new SelectBuilder().setDistinct(true).addVar(Namespace.s).addVar(wkt)
//                    .from(Namespace.UnionModel.getURI()) //
//                    .addWhere(Namespace.s, Geo.AS_WKT_NODE, wkt) //
//                    .addWhere(Namespace.s, RDF.type, Namespace.Obst) //
//                    .addFilter(ctxt.graphGeomFactory.isNearby(exprF, mapObstacle.wkt(), wkt,
//                            ctxt.scaleInfo.getResolution()));
//
//            Set<MapObstacleImpl> solns = new HashSet<>();
//            exec(selectBuilder).thenAccept(resultSet -> resultSet.forEachRemaining(soln -> {
//                solns.add(
//                        new MapObstacleImpl(ctxt, soln.getResource(Namespace.s.getName()), soln.getLiteral(wkt.getName())));
//            })).join();
//            if (solns.isEmpty()) {
//                LOG.debug("Adding obstacle: {}", mapObstacle);
//                Model merged = ModelFactory.createDefaultModel();
//                mapObstacle.in(merged);
//                doUpdate(new UpdateRequest()
//                        .add(new UpdateBuilder().addInsert(Namespace.PlanningModel, merged).build()));;
//            } else {
//                mapObstacle = mergeIntersectOrTouch(mapObstacle, solns);
//            }
//
//            // delete any Coords that are within buffer of any work geometries.
//            Var obstRes = Var.alloc("obst");
//            Var otherWkt = Var.alloc("otherWkt");
//            Var varX = Var.alloc("x");
//            Var varY = Var.alloc("y");
//            SelectBuilder sb = new SelectBuilder().addWhere(Namespace.s, RDF.type, Namespace.Coord)
//                    .addWhere(Namespace.s, Namespace.x, varX).addWhere(Namespace.s, Namespace.y, varY)
//                    .addWhere(Namespace.s, Geo.AS_WKT_NODE, otherWkt)
//                    .addFilter(exprF.lt(ctxt.graphGeomFactory.calcDistance(exprF, mapObstacle.wkt(), otherWkt),
//                            ctxt.chassisInfo.radius));
//            Set<MapLocationImpl> deletedCoords = new HashSet<>();
//            exec(sb).thenAccept(resultSet -> resultSet.forEachRemaining(soln -> {
//                MapLocationImpl mapLocation = asMapCoordinate(new Coordinate(soln.getLiteral(varX.getName()).getDouble(),
//                        soln.getLiteral(varY.getName()).getDouble()));
//                deletedCoords.add(mapLocation);
//            }));
//
//            // delete the target records
//            COORDINATE_MAPLOCATION_MAP.values().forEach(mapLocation -> mapLocation.removeTargets(deletedCoords));
//            UpdateRequest req = new UpdateRequest();
//
//            Object toTarget = Converters.makeNodeOrPath("(" + Namespace.target.asNode() + "/*)", getPrefixMapping());
//
//            deletedCoords
//                    .forEach(
//                            mapLocation -> req
//                                    .add(new UpdateBuilder()
//                                            .addDelete(Namespace.PlanningModel, mapLocation.getUrn().asNode(), toTarget,
//                                                    Namespace.o)
//                                            .addWhere(mapLocation.getUrn(), toTarget, Namespace.o).build())
//                                    .add(new UpdateBuilder()
//                                            .addDelete(Namespace.PlanningModel, mapLocation.getUrn().asNode(),
//                                                    Namespace.p, Namespace.o)
//                                            .addWhere(mapLocation.getUrn(), Namespace.p, Namespace.o).build()));
//
//            doUpdate(req);
//
//            return mapObstacle;
//        }
//
//        boolean isObstacle(MapCoordinate point) {
//            Literal pointWKT = ctxt.graphGeomFactory.asWKT(point.getCoordinate());
//            Var wkt = Var.alloc("wkt");
//            AskBuilder ask = new AskBuilder().addGraph(Namespace.UnionModel, new WhereBuilder()
//                    .addWhere(Namespace.s, RDF.type, Namespace.Obst) //
//                    .addWhere(Namespace.s, Geo.AS_WKT_PROP, wkt) //
//                    .addFilter(ctxt.graphGeomFactory.isNearby(exprF, pointWKT, wkt, ctxt.scaleInfo.getResolution())));
//            return ask(ask);
//        }
//
//        CompletableFuture<Set<MapObstacleImpl>> getObstacles() {
//            Var wkt = Var.alloc("wkt");
//            SelectBuilder sb = new SelectBuilder().addVar(Namespace.s).addVar(wkt) //
//                    .addGraph(Namespace.UnionModel, new WhereBuilder().addWhere(Namespace.s, RDF.type, Namespace.Obst) //
//                            .addWhere(Namespace.s, Geo.AS_WKT_PROP, wkt));
//
//            return exec(sb).thenApply(resultSet -> {
//                Set<MapObstacleImpl> result = new HashSet<>();
//                resultSet.forEachRemaining(soln -> {
//                    result.add(new MapObstacleImpl(ctxt, soln.getResource(Namespace.s.getName()),
//                            soln.getLiteral(wkt.getName())));
//                });
//                return result;
//            });
//        }
//    }
//}
