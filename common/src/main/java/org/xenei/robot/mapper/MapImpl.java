package org.xenei.robot.mapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.arq.querybuilder.ConstructBuilder;
import org.apache.jena.arq.querybuilder.ExprFactory;
import org.apache.jena.arq.querybuilder.Order;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.geosparql.implementation.vocabulary.Geo;
import org.apache.jena.geosparql.implementation.vocabulary.GeoSPARQL_URI;
import org.apache.jena.geosparql.implementation.vocabulary.SRS_URI;
import org.apache.jena.geosparql.spatial.SpatialIndex;
import org.apache.jena.geosparql.spatial.SpatialIndexException;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.shared.Lock;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateRequest;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.util.iterator.WrappedIterator;
import org.apache.jena.vocabulary.RDF;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.common.FrontsCoordinate;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.mapping.Map;
import org.xenei.robot.common.mapping.MapCoord;
import org.xenei.robot.common.mapping.Obstacle;
import org.xenei.robot.common.planning.Solution;
import org.xenei.robot.common.planning.Segment;
import org.xenei.robot.common.utils.CoordUtils;
import org.xenei.robot.common.utils.RobutContext;
import org.xenei.robot.mapper.rdf.Namespace;

public class MapImpl implements Map {
    private static final Logger LOG = LoggerFactory.getLogger(MapImpl.class);
    private final RobutContext ctxt;
    private final Dataset data;
    private final ExprFactory exprF;
    private final ObstacleHandler obstacleHandler;

    public static PrefixMapping getPrefixes() {
        return PrefixMapping.Factory.create().setNsPrefixes(GeoSPARQL_URI.getPrefixes())
                .setNsPrefixes(PrefixMapping.Standard).setNsPrefix("robut", Namespace.URI);
    }

    public MapImpl(RobutContext ctxt) {
        this.ctxt = ctxt;
        data = DatasetFactory.create();
        data.getDefaultModel().setNsPrefixes(getPrefixes());
        data.addNamedModel(Namespace.BaseModel, defaultModel());
        data.addNamedModel(Namespace.PlanningModel, defaultModel());
        data.addNamedModel(Namespace.KnownModel, defaultModel());
        exprF = new ExprFactory(data.getPrefixMapping());

        try {
            SpatialIndex.buildSpatialIndex(data, SRS_URI.DEFAULT_WKT_CRS84);
        } catch (SpatialIndexException e) {
            throw new RuntimeException(e);
        }
        obstacleHandler = new ObstacleHandler();
    }

    @Override
    public void clear(String namedGraph) {
        try (LockHandler ignored = new LockHandler(Lock.WRITE)) {
            if (namedGraph.equals(Namespace.UnionModel.getURI())) {
                data.getDefaultModel().removeAll();
                data.replaceNamedModel(Namespace.BaseModel, defaultModel());
                data.replaceNamedModel(Namespace.PlanningModel, defaultModel());
            } else {
                data.replaceNamedModel(namedGraph, defaultModel());
            }
        }
    }

    @Override
    public RobutContext getContext() {
        return ctxt;
    }

    public static PrefixMapping getPrefixMapping() {
        PrefixMapping pm = PrefixMapping.Factory.create();
        pm.setNsPrefixes(GeoSPARQL_URI.getPrefixes());
        pm.setNsPrefixes(PrefixMapping.Standard.getNsPrefixMap());
        pm.setNsPrefix("robut", "urn:org.xenei.robot:");
        return pm;
    }

    private static Model defaultModel() {
        return ModelFactory.createDefaultModel().setNsPrefixes(getPrefixMapping());
    }

    public boolean isEmpty() {
        try (LockHandler ignored = new LockHandler(Lock.READ)) {
            return data.getUnionModel().isEmpty();
        }
    }

    private CompletableFuture<?> doUpdate(UpdateBuilder update) {
        return ctxt.submit( () -> {
            try (LockHandler ignored = new LockHandler(Lock.WRITE)) {
                UpdateExecutionFactory.create(update.build(), data).execute();
            }
        });
    }

    private CompletableFuture<?> doUpdate(UpdateRequest request) {
        return ctxt.submit( () -> {
            try (LockHandler ignored = new LockHandler(Lock.WRITE)) {
                UpdateExecutionFactory.create(request, data).execute();
            }
        });
    }

    public boolean ask(AskBuilder ask) {
        try (LockHandler ignored = new LockHandler(Lock.READ);
             QueryExecution exec = QueryExecutionFactory.create(ask.build(), data)) {
            return exec.execAsk();
        }
    }

    public void dump(Resource modelName, Consumer<Model> consumer) {
        try (LockHandler ignored = new LockHandler(Lock.READ)) {
            consumer.accept(data.getNamedModel(modelName));
        }
    }

    /**
     * executes the select query and processes the result with the processor.
     * Processing stops when processor returns false.
     *
     * @param select the SelectBuilder to execute.
     */
    CompletableFuture<ResultSet> exec(SelectBuilder select) {
        return ctxt.submit( () -> {
            try (LockHandler ignored = new LockHandler(Lock.READ);
                 QueryExecution qexec = QueryExecutionFactory.create(select.build(), data)) {
                return qexec.execSelect().materialise();
            }
        });
    }

    Model construct(ConstructBuilder select) {
        try (LockHandler ignore = new LockHandler(Lock.READ);
                QueryExecution qexec = QueryExecutionFactory.create(select.build(), data)) {
            return qexec.execConstruct();
        }
    }

//    @Override
//    public CompletableFuture<Optional<Step>> addCoord(Coordinate coord, Double distance, boolean visited, Boolean isIndirect) {
//        MapCoordinate mapCoord = new MapCoordinate(coord);
//        UpdateRequest req = new UpdateRequest();
//        Resource graphCoord = null;
//        WhereBuilder where = new WhereBuilder().addWhere(Namespace.s, RDF.type, Namespace.Coord)
//                .addWhere(Namespace.s, Geo.AS_WKT_PROP, ctxt.graphGeomFactory.asWKT(mapCoord.getCoordinate()));
//        if (exists(mapCoord, Namespace.Coord)) {
//            UpdateBuilder newDat = new UpdateBuilder().addWhere(where);
//            if (distance != null) {
//                newDat.addInsert(Namespace.PlanningModel, Namespace.s, Namespace.distance, distance);
//            }
//            if (visited) {
//                newDat.addInsert(Namespace.PlanningModel, Namespace.s, Namespace.visited, visited);
//            }
//            if (isIndirect != null && isIndirect) {
//                newDat.addInsert(Namespace.PlanningModel, Namespace.s, Namespace.isIndirect, isIndirect);
//            }
//            // clear and set existing value
//            req.add(new UpdateBuilder().addDelete(Namespace.PlanningModel, Namespace.s, Namespace.distance, Namespace.o)
//                    .addWhere(Namespace.s, Namespace.distance, Namespace.o).addWhere(where).build())
//                    .add(newDat.build());
//        } else {
//            // no existing record
//            graphCoord = ctxt.graphGeomFactory.asRDF(mapCoord, Namespace.Coord);
//            if (distance != null) {
//                graphCoord.addLiteral(Namespace.distance, distance);
//            }
//            if (visited) {
//                graphCoord.addLiteral(Namespace.visited, visited);
//            }
//            if (isIndirect != null && isIndirect) {
//                graphCoord.addLiteral(Namespace.isIndirect, isIndirect);
//            }
//            req.add(new UpdateBuilder().addInsert(Namespace.PlanningModel, graphCoord.getModel()).build());
//        }
//
//        return doUpdate(req).thenApply( x -> {
//            LOG.debug("Added {} for {}", mapCoord, coord);
//            if (distance == null || distance <= 0) {
//                return Optional.empty();
//            }
//            return Optional.of(StepImpl.builder().setCoordinate(mapCoord).setDistance(distance)
//                    .setCost(isIndirect != null && isIndirect ? distance * 2 : distance).build(ctxt));
//        });
//    }

    @Override
    public CompletableFuture<Optional<Segment>> addCoord(final FrontsCoordinate coord, final FrontsCoordinate target, final boolean visited) {
        final MapCoordinate mapCoord = asMapCoordinate(coord);
        final MapCoordinate targetCoord = asMapCoordinate(target);
        final Double distance = targetCoord == null ? null : mapCoord.distance(targetCoord);
        final Boolean indirect = targetCoord == null ? null : !this.isClearPath(mapCoord, targetCoord);
        UpdateRequest req = new UpdateRequest();
        AskBuilder whereClause = askExists(mapCoord, Namespace.Coord);
        if (!ask(whereClause)) {
            Resource graphCoord = ctxt.graphGeomFactory.asRDF(mapCoord, Namespace.Coord);
            if (target != null) {
                graphCoord.addLiteral(Namespace.distance, distance);
                graphCoord.addLiteral(Namespace.isIndirect, indirect);
            }
            if (visited) {
                graphCoord.addLiteral(Namespace.visited, true);
            }
            req.add(new UpdateBuilder().addInsert(Namespace.PlanningModel, graphCoord.getModel()).build());
        } else {
            UpdateBuilder update = new UpdateBuilder();
            if (target != null) {
                update.addInsert(Namespace.PlanningModel, Namespace.s, Namespace.distance, distance);
                update.addInsert(Namespace.PlanningModel, Namespace.s, Namespace.isIndirect, indirect);
            }
            if (visited) {
                update.addInsert(Namespace.PlanningModel, Namespace.s, Namespace.visited, true);
            }
            if (!update.isEmpty()) {
                update.addWhere(whereClause);
                req.add(update.build());
            }
        }
        Function<Object,Optional<Segment>> conversion = r -> Optional.empty();
        if (target != null) {
            final double cost = indirect != null && indirect ? distance * 2 : distance;
            conversion = x -> {
                LOG.debug("Added {} for {}", mapCoord, coord);
                return target == null ?
                        Optional.empty() :
                        Optional.of(StepImpl.builder().setCoordinate(targetCoord).setDistance(distance)
                                .setCost(cost).build(ctxt));
            };
        }
        return doUpdate(req).thenApply(conversion);
    }

    @Override
    public Set<Obstacle> addObstacle(Obstacle obst) {
        return obstacleHandler.addObstacle(obst);
    }


    @Override
    public boolean isObstacle(FrontsCoordinate point) {
        return obstacleHandler.isObstacle(point);
    }

    @Override
    public CompletableFuture<Set<Obstacle>> getObstacles() {
        return obstacleHandler.getObstacles();
    }

    /**
     * Gets the Step for the coordinates.
     *
     * @param location The location to get the Step for
     * @return the Step for the location.
     */
    public CompletableFuture<Optional<Segment>> getStep(double costToLocation, FrontsCoordinate location) {
        MapCoordinate coordinate = asMapCoordinate(location);

        Var geom = Var.alloc("geom");
        Var dist = Var.alloc("dist");
        Var indirect = Var.alloc("indirect");
        Var indirectFlg = Var.alloc("indirectFlg");
        Var cost = Var.alloc("cost");

        SelectBuilder sb = new SelectBuilder().addVar(cost).addVar(dist).addVar(geom) //
                .from(Namespace.PlanningModel.getURI()) //
                .addWhere(Namespace.s, RDF.type, Namespace.Coord) //
                .addWhere(Namespace.s, Namespace.distance, dist) //
                .addWhere(Namespace.s, Namespace.x, coordinate.getX()) //
                .addWhere(Namespace.s, Namespace.y, coordinate.getY()) //
                .addWhere(Namespace.s, Geo.AS_WKT_PROP, geom) //
                .addOptional(Namespace.s, Namespace.isIndirect, indirect) //
                .addBind(exprF.cond(exprF.bound(indirect), exprF.asExpr(indirect), exprF.asExpr(false)),
                        indirectFlg)
                .addBind(SPARQL.costCalc(costToLocation, dist, indirectFlg), cost);

        StepImpl.Builder builder = StepImpl.builder();

//        Predicate<QuerySolution> processor = soln -> {
//            Geometry geometry = ctxt.graphGeomFactory.fromWkt(soln.getLiteral(geom.getName()));
//            builder.setCoordinate(coordinate).setCost(soln.getLiteral(cost.getName()).getDouble())
//                    .setDistance(soln.getLiteral(dist.getName()).getDouble()).setGeometry(geometry);
//            return false;
//        };

        return exec(sb).thenApply( resultSet -> {
            if (resultSet.hasNext()) {
                QuerySolution soln = resultSet.next();
                Geometry geometry = ctxt.graphGeomFactory.fromWkt(soln.getLiteral(geom.getName()));
                builder.setCoordinate(coordinate).setCost(soln.getLiteral(cost.getName()).getDouble())
                        .setDistance(soln.getLiteral(dist.getName()).getDouble()).setGeometry(geometry);
            }
            return builder.isValid(ctxt) ? Optional.of(builder.build(ctxt)) : Optional.empty() ;
        });
        //return new ChainedFuture<>(exec(sb, processor), () ->builder.isValid(ctxt) ? Optional.of(builder.build(ctxt)) : Optional.empty());
    }

    /**
     * Add a path to the planning model of the map.
     *
     * @param coords the coordinates of the path.
     * @return An array of coordinates on the path.
     */
    @Override
    public void addPath(FrontsCoordinate... coords) {
        addPath(Namespace.PlanningModel, coords);
    }

    /**
     * Add the plan record to the map
     *
     * @param model  the model name to add the path to.
     * @param coords the coordinates of the path.
     * @return An array of coordinates on the path.
     */
    @Override
    public void addPath(Resource model, FrontsCoordinate... coords) {
        addPath(model, Arrays.stream(coords).map(this::asMapCoordinate));
    }

    /**
     * Add a stream of coordinates as a path.
     * @param model the model name to add the path to.
     * @param coords the coordinates of the path.
     * @return An array of coordinates on the path.
     */
    private void addPath(Resource model, Stream<MapCoordinate> coords) {
        Coordinate[] points = coords.map(MapCoordinate::getCoordinate).toArray(Coordinate[]::new);
        Literal path = ctxt.graphGeomFactory.asWKTString(points);
        Resource tn = ResourceFactory.createResource();
        List<Triple> triples = new ArrayList<>();
        triples.add(Triple.create(tn.asNode(), RDF.type.asNode(), Namespace.Path.asNode()));
        triples.add(Triple.create(tn.asNode(), Geo.AS_WKT_PROP.asNode(), path.asNode()));
        LOG.debug("Path <{} {}>", points[0], points[points.length - 1]);
        doUpdate(new UpdateBuilder().addInsert(model, triples));
    }

    @Override
    public CompletableFuture<?> cutPath(FrontsCoordinate a, FrontsCoordinate b) {
        return cutPath(Namespace.PlanningModel, a, b);
    }

    private AskBuilder askExists(MapCoordinate coordinate, Resource type) {
        return new AskBuilder().from(Namespace.UnionModel.getURI()) //
                .addWhere(Namespace.s, RDF.type, type) //
                .addWhere(Namespace.s, Namespace.x, coordinate.getX()) //
                .addWhere(Namespace.s, Namespace.y, coordinate.getY());
    }

    private boolean exists(MapCoordinate coordinate, Resource type) {
        return ask(askExists(coordinate, type));
    }

    public CompletableFuture<?> cutPath(Resource model, FrontsCoordinate a, FrontsCoordinate b) {
        Var ra = Var.alloc("a");
        Var rb = Var.alloc("b");
        MapCoordinate mapA = asMapCoordinate(a);
        MapCoordinate mapB = asMapCoordinate(b);

        UpdateBuilder ub = new UpdateBuilder().addDelete(model, Namespace.s, Namespace.p, Namespace.o)
                .addWhere(Namespace.s, Namespace.p, Namespace.o).addWhere(Namespace.s, Namespace.point, ra)
                .addWhere(Namespace.s, Namespace.point, rb).addWhere(ra, Namespace.x, mapA.getX())
                .addWhere(ra, Namespace.y, mapA.getY()).addWhere(rb, Namespace.x, mapB.getX())
                .addWhere(rb, Namespace.y, mapB.getY());
        return doUpdate(ub);
    }

    public boolean hasPath(Location a, Location b) {
        Var wkt = Var.alloc("wkt");
        Literal mapA = ctxt.graphGeomFactory.asWKT(a.getCoordinate());
        Literal mapB = ctxt.graphGeomFactory.asWKT(b.getCoordinate());

        WhereBuilder wb = new WhereBuilder().addWhere(Namespace.s, RDF.type, Namespace.Path) //
                .addWhere(Namespace.s, Geo.AS_WKT_NODE, wkt)
                .addFilter(ctxt.graphGeomFactory.isNearby(exprF, wkt, mapA, ctxt.scaleInfo.getResolution()))
                .addFilter(ctxt.graphGeomFactory.isNearby(exprF, wkt, mapB, ctxt.scaleInfo.getResolution()));

        AskBuilder ask = new AskBuilder().addGraph(Namespace.UnionModel, wb);
        return ask(ask);
    }

    @Override
    public boolean isClearPath(FrontsCoordinate from, FrontsCoordinate target) {
        return !ask(getClearPathCalculation(Namespace.s, from.getCoordinate(), target.getCoordinate()));
    }

    AskBuilder getClearPathCalculation(Var subject, Coordinate from, Coordinate target) {
        LOG.debug("checking clearView from {} to {} ", from, target);
        Literal pathWkt = ctxt.graphGeomFactory.asWKTPath(ctxt.chassisInfo.radius, from, target);
        Var wkt = Var.alloc("wkt");

        return new AskBuilder().from(Namespace.UnionModel.getURI()) //
                .addWhere(subject, RDF.type, Namespace.Obst) //
                .addWhere(subject, Geo.AS_WKT_PROP, wkt)
                .addFilter(exprF.eq(ctxt.graphGeomFactory.calcDistance(exprF, pathWkt, wkt), 0));
    }

    /**
     * Updates the property of the coordinates record in the model to have the
     * specified value.
     *
     * @param model The model to update.
     * @param coord the node to update
     * @param property the property to update
     * @param value the value to set the property to.
     * @return An optional future if the node was updated, an empty optional otherwise.
     */
    CompletableFuture<?> updateCoordinate(Resource model, FrontsCoordinate coord, Property property, Object value) {
        MapCoordinate mapCoord = asMapCoordinate(coord);
        LOG.debug("updating {} {} {} to {}", model.getLocalName(), CoordUtils.toString(mapCoord, 1),
                property.getLocalName(), value);

        if (exists(mapCoord, Namespace.Coord)) {
            UpdateRequest req = new UpdateRequest();
            req.add(new UpdateBuilder().addDelete(model, Namespace.s, property, Namespace.o)
                    .addGraph(Namespace.UnionModel,
                            new WhereBuilder().addWhere(Namespace.s, property, Namespace.o)
                                    .addWhere(Namespace.s, RDF.type, Namespace.Coord) //
                                    .addWhere(Namespace.s, Namespace.x, mapCoord.getX())
                                    .addWhere(Namespace.s, Namespace.y, mapCoord.getY()))
                    .build())
                    .add(new UpdateBuilder().addInsert(model, Namespace.s, property, value)
                            .addGraph(Namespace.UnionModel,
                                    new WhereBuilder().addWhere(Namespace.s, RDF.type, Namespace.Coord)
                                            .addWhere(Namespace.s, Namespace.x, mapCoord.getX())
                                            .addWhere(Namespace.s, Namespace.y, mapCoord.getY()))
                            .build());
            return doUpdate(req);
        }
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Calculate the best next position based on the map and current coordinates.
     *
     * @param currentCoords the current coordinates
     * @return Optional containing either either the PlanRecord for the next
     * position, or empty if none found.
     */
    @Override
    public Optional<Segment> getBestSegment(FrontsCoordinate currentCoords) {
        if (data.isEmpty()) {
            LOG.debug("No map points");
            return Optional.empty();
        }

        StepQuery stepQuery = new StepQuery(currentCoords);
        ExtendedIterator<StepImpl.Builder> iter = stepQuery.execute().join();
        if (iter.hasNext()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Query\n{}", MapReports.dumpQuery(MapImpl.this, stepQuery.query));
                LOG.debug("Distance\n{}", MapReports.dumpDistance(MapImpl.this, currentCoords));
                LOG.debug("Obstacles\n{}", MapReports.dumpObstacleDistance(MapImpl.this));
                MapImpl.this.getObstacles().thenAccept(obs -> obs.forEach(s -> LOG.debug(s.toString()))).join();
                LOG.debug("Model\n{}", MapReports.dumpModel(MapImpl.this));
                LOG.debug("No Selected map points");
            }
            return Optional.of(iter.next().build(ctxt));
        }
        return Optional.empty();
    }

    @Override
    public CompletableFuture<?> setVisited(FrontsCoordinate finalTarget, FrontsCoordinate coord) {
        MapCoordinate mapCoord = asMapCoordinate(coord);
        CompletableFuture<?> future = updateCoordinate(Namespace.PlanningModel, mapCoord, Namespace.visited, Boolean.TRUE);

        UpdateRequest req = new UpdateRequest();
        Resource qA = ctxt.graphGeomFactory.asRDF(mapCoord, Namespace.Coord);
        req.add(new UpdateBuilder().addInsert(Namespace.PlanningModel, qA.getModel()) //
                .addInsert(Namespace.PlanningModel, qA.asResource(), Namespace.visited, true) //
                .addInsert(Namespace.PlanningModel, qA.asResource(), Namespace.distance,
                        mapCoord.distance(finalTarget)) //
                .build());
        return future.thenApply(r -> doUpdate(req));
    }

    @Override
    public MapCoordinate recalculate(FrontsCoordinate target) {
        LOG.debug("recalculate: {}", target);
        Var distance = Var.alloc("distance");
        Var wkt = Var.alloc("wkt");
        MapCoordinate result = asMapCoordinate(target);
        Literal targ = ctxt.graphGeomFactory.asWKT(result.getCoordinate());
        // remove all the distance and adjustments and then calculate the distance to
        // the new target

        UpdateRequest req = new UpdateRequest().add(new UpdateBuilder() //
                .addDelete(Namespace.PlanningModel, Namespace.s, Namespace.p, Namespace.o) //
                .addGraph(Namespace.PlanningModel, new WhereBuilder() //
                        .addWhere(Namespace.s, Namespace.p, Namespace.o) //
                        .addFilter(exprF.in(Namespace.p, exprF.asList(Namespace.isIndirect, Namespace.distance))) //
                ).build()) //
                .add(new UpdateBuilder() //
                        .addInsert(Namespace.PlanningModel, Namespace.s, Namespace.distance, distance) //
                        .addGraph(Namespace.UnionModel, new WhereBuilder() //
                                .addWhere(Namespace.s, RDF.type, Namespace.o) //
                                .addFilter(exprF.in(Namespace.o, exprF.asList(Namespace.Coord, Namespace.Path))) //
                                .addWhere(Namespace.s, Geo.AS_WKT_PROP, wkt) //
                                .addBind(ctxt.graphGeomFactory.calcDistance(exprF, targ, wkt), distance))
                        .build());

        getCoords().thenAccept( c -> c.stream().filter(mc -> !isClearPath(mc.location, target))
                .forEach(mc -> req.add(new UpdateBuilder() //
                        .addInsert(Namespace.PlanningModel, Namespace.s, Namespace.isIndirect, true) //
                        .addGraph(Namespace.UnionModel, new WhereBuilder() //
                                .addWhere(Namespace.s, Geo.AS_WKT_PROP, ctxt.graphGeomFactory.asWKT(mc.geometry))) //
                        .build())));

        doUpdate(req);
        return result;
    }

    @Override
    public CompletableFuture<Collection<MapCoord>>  getCoords() {
        Var x = Var.alloc("x");
        Var y = Var.alloc("y");
        Var wkt = Var.alloc("wkt");
        Var indirect = Var.alloc("indirect");

        SelectBuilder sb = new SelectBuilder().addVar(x).addVar(y).addVar(indirect).addVar(wkt) //
                .from(Namespace.PlanningModel.getURI()) //
                .addWhere(Namespace.s, RDF.type, Namespace.Coord) //
                .addOptional(Namespace.s, Namespace.isIndirect, indirect) //
                .addWhere(Namespace.s, Geo.AS_WKT_PROP, wkt) //
                .addWhere(Namespace.s, Namespace.x, x) //
                .addWhere(Namespace.s, Namespace.y, y);

        List<MapCoord> result = new ArrayList<>();

        Consumer<QuerySolution> processor = soln -> {
            Geometry geom = ctxt.graphGeomFactory.fromWkt(soln.getLiteral(wkt.getName()));
            Literal litIndirect = soln.getLiteral(indirect.getName());
            result.add(new MapCoord( //
                    soln.getLiteral(x.getName()).getDouble(), //
                    soln.getLiteral(y.getName()).getDouble(), //
                    litIndirect != null && litIndirect.getBoolean(), geom));
        };

        return exec(sb).thenApply(resultSet -> {
            resultSet.forEachRemaining(processor);
            return result;
        });

    }

    @Override
    public Collection<Segment> getSegments(FrontsCoordinate currentPosition) {
        return new StepQuery(currentPosition).execute().join().mapWith(builder -> builder.build(ctxt))
                .toList();
    }

    @Override
    public void recordSolution(Solution solution) {
        solution.simplify(this::isClearPath);
        addPath(Namespace.BaseModel, solution.stream().map(this::asMapCoordinate));
    }

    Model getModel() {
        return data.getUnionModel();
    }

    @Override
    public CompletableFuture<Void> updateIsIndirect(FrontsCoordinate finalTarget, Set<Obstacle> newObstacles) {
        Var isIndirect = Var.alloc("isIndirect");
        Var wkt = Var.alloc("wkt");
        Var x = Var.alloc("x");
        Var y = Var.alloc("y");

        SelectBuilder sb = new SelectBuilder().addVar(x).addVar(y).setDistinct(true) //
                .addGraph(Namespace.UnionModel, new WhereBuilder() //
                        .addWhere(Namespace.s, RDF.type, Namespace.Coord) //
                        .addWhere(Namespace.s, Namespace.x, x) //
                        .addWhere(Namespace.s, Namespace.y, y) //
                        .addOptional(Namespace.s, Namespace.isIndirect, isIndirect) //
                        .addFilter(exprF.not(exprF.bound(isIndirect))));

        return exec(sb).thenApply(resultSet -> {
            List<Literal> updateCoords = new ArrayList<>();

            resultSet.forEachRemaining(soln -> {
                Coordinate c = new Coordinate(soln.getLiteral(x.getName()).getDouble(), soln.getLiteral(y.getName()).getDouble());
                Geometry path = ctxt.geometryUtils.asPath(ctxt.chassisInfo.radius, c, finalTarget.getCoordinate());
                for (Obstacle obst : newObstacles) {
                    if (path.distance(obst.geom()) == 0) {
                        updateCoords.add(ctxt.graphGeomFactory.asWKT(c));
                        break;
                    }
                }
            });
            return updateCoords;
        }).thenAccept( updateCoords -> {
            if (!updateCoords.isEmpty()) {
                UpdateBuilder ub = new UpdateBuilder()
                        .addInsert(Namespace.PlanningModel, Namespace.s, Namespace.isIndirect, Boolean.TRUE) //
                        .addGraph(Namespace.UnionModel, new WhereBuilder() //
                                .addWhere(Namespace.s, RDF.type, Namespace.Coord) //
                                .addWhere(Namespace.s, Geo.AS_WKT_NODE, wkt)
                                .addFilter(exprF.in(wkt, updateCoords.toArray())));
                doUpdate(ub);
            }
        });
    }

    @Override
    public Obstacle createObstacle(Position startPosition, FrontsCoordinate relativeStart, FrontsCoordinate relativeEnd) {
        return new ObstacleImpl(startPosition, relativeStart, relativeEnd);
    }

    @Override
    public Obstacle createObstacle(Position startPosition, FrontsCoordinate relativeLocation) {
        return new ObstacleImpl(startPosition, relativeLocation);
    }

    private class LockHandler implements AutoCloseable {
        Lock lock;

        private LockHandler(boolean flag) {
            lock = data.getLock();
            lock.enterCriticalSection(flag);
        }

        @Override
        public void close() {
            lock.leaveCriticalSection();
        }
    }

    @Override
    public CompletableFuture<Optional<FrontsCoordinate>> look(Position from, double heading, int maxRange) {

        Coordinate target = from.plus(CoordUtils.fromAngle(heading, maxRange));

        Literal pathWkt = ctxt.graphGeomFactory.asWKTString(from.getCoordinate(), target);
        Var wkt = Var.alloc("wkt");
        Var dist = Var.alloc("dist");
        Literal fromWkt = ctxt.graphGeomFactory.asWKT(from.getCoordinate());
        SelectBuilder look = new SelectBuilder().addVar(dist).from(Namespace.UnionModel.getURI()) //
                .addWhere(Namespace.s, RDF.type, Namespace.Obst) //
                .addWhere(Namespace.s, Geo.AS_WKT_PROP, wkt) //
                .addBind(ctxt.graphGeomFactory.calcIntersectionDistance(exprF, fromWkt, pathWkt, wkt), dist) //
                .addFilter(ctxt.graphGeomFactory.intersects(exprF, pathWkt, wkt)) //
                .addFilter(exprF.lt(dist, maxRange)) //
                .addOrderBy(dist, Order.ASCENDING) //
                .setLimit(1);

        return exec(look).thenApply(resultSet -> {
            double range = resultSet.hasNext() ?
                resultSet.next().getLiteral(dist.getName()).getDouble() :
                    -1.0;
            Location location = null;
            if (range > -1) {
                location = Location.from(CoordUtils.fromAngle(heading - from.getHeading(), range));
            }
                if ( LOG.isDebugEnabled()) {
                    LOG.debug("Looking {} ({}) from {} returned {}", heading, Math.toDegrees(heading), from, location);
                }
            return Optional.ofNullable(location);
        });
    }

    private class SPARQL {

        /**
         * Calculate the cost from a position to the target
         *
         * @param posDistToTarget distance from position to target
         * @param indirect true if there is an obstacle in the way.
         * @return the expression to calculate the distance.
         */
        static Expr indirectCalc(Object posDistToTarget, Object indirect) {
            ExprFactory exprF = new ExprFactory(getPrefixes());
            return exprF.add(exprF.cond(exprF.bound(indirect), exprF.asExpr(posDistToTarget), exprF.asExpr(0)),
                    posDistToTarget);
        }

        /**
         * Calculates the cost to the target via the position.
         *
         * @param distToPos the distance to the position.
         * @param posDistToTarget the distance from the position to the target
         * @param indirect true if there is an obstacle in the way.
         * @return the expression to calcualte the cost.
         */
        static Expr costCalc(Object distToPos, Var posDistToTarget, Var indirect) {
            ExprFactory exprF = new ExprFactory(getPrefixes());
            return exprF.add(distToPos, SPARQL.indirectCalc(posDistToTarget, indirect));
        }
    }

    public class StepQuery {
        Literal wkt;

        Var cost = Var.alloc("cost");

        // distance current to other
        Var dist = Var.alloc("dist");

        Var other = Var.alloc("other");
        // wkt of other
        Var otherWkt = Var.alloc("otherWkt");
        Var indirectFlg = Var.alloc("indirectFlg");
        // distance from other to target
        Var otherDist = Var.alloc("otherDist");

        Var indirect = Var.alloc("indirect");
        Var visited = Var.alloc("visited");
        Var other2 = Var.alloc("other2");
        Var other2Wkt = Var.alloc("other2Wkt");

        final SelectBuilder query;
        final AskBuilder checkVisited;
        final Function<ResultSet,ExtendedIterator<StepImpl.Builder>> processor;

        StepQuery(FrontsCoordinate currentCoords) {
            MapCoordinate mapCoords = asMapCoordinate(currentCoords);
            wkt = ctxt.graphGeomFactory.asWKT(mapCoords.getCoordinate());

            query = new SelectBuilder().addVar(indirectFlg).addVar(cost).addVar(otherWkt).addVar(other).addVar(dist) //
                    .from(Namespace.UnionModel.getURI()) //
                    .addWhere(Namespace.s, RDF.type, Namespace.Coord) //
                    .addWhere(Namespace.s, Geo.AS_WKT_PROP, wkt).addWhere(other, RDF.type, Namespace.Coord) //
                    .addOptional(other, Namespace.isIndirect, indirect) //
                    .addWhere(other, Geo.AS_WKT_PROP, otherWkt) //
                    .addWhere(other, Namespace.distance, otherDist) //
                    .addOptional(other, Namespace.visited, visited) //
                    .addFilter(exprF.and(exprF.ne(other, Namespace.s), exprF.not(exprF.bound(visited)))) //
                    .addBind(ctxt.graphGeomFactory.calcDistance(exprF, otherWkt, wkt), dist) //
                    .addBind(SPARQL.costCalc(dist, otherDist, indirect), cost) //
                    .addBind(exprF.cond(exprF.bound(indirect), exprF.asExpr(indirect), exprF.asExpr(false)),
                            indirectFlg)
                    .addOrderBy(indirectFlg, Order.ASCENDING).addOrderBy(cost, Order.ASCENDING)
            ;

            // skip coords that are within the tolerance range of visited coords
            // returns true if the position has been visited.
            checkVisited = new AskBuilder() //
                    .addWhere(other2, Namespace.visited, "?ignore") //
                    .addWhere(other2, RDF.type, Namespace.Coord)//
                    .addWhere(other2, Geo.AS_WKT_PROP, other2Wkt) //
                    .addFilter(exprF.le(ctxt.graphGeomFactory.calcDistance(exprF, otherWkt, other2Wkt),
                            ctxt.chassisInfo.radius)) //
            ;

            Predicate<QuerySolution> filter = soln -> {
                Geometry geom = ctxt.graphGeomFactory.fromWkt(soln.getLiteral(otherWkt.getName()));
                for (Coordinate candidate : geom.getCoordinates()) {
                    Literal candidateWkt = ctxt.graphGeomFactory.asWKT(candidate);
                    checkVisited.setVar(otherWkt, candidateWkt);
                    // if not visited and has a clear path
                    return (!ask(checkVisited) && isClearPath(currentCoords, Location.from(candidate)));
                }
                return false;
            };


            processor = resultSet ->
                    WrappedIterator.create(resultSet)
                            .mapWith(soln -> {
                                Geometry geom = ctxt.graphGeomFactory.fromWkt(soln.getLiteral(otherWkt.getName()));
                                for (Coordinate candidate : geom.getCoordinates()) {
                                    Literal candidateWkt = ctxt.graphGeomFactory.asWKT(candidate);
                                    checkVisited.setVar(otherWkt, candidateWkt);
                                    // if not visited and has a clear path
                                    if (!ask(checkVisited) && isClearPath(currentCoords, Location.from(candidate))) {
                                        return StepImpl.builder().setCoordinate(candidate)
                                                .setCost(soln.getLiteral(cost.getName()).getDouble())
                                                .setDistance(soln.getLiteral(dist.getName()).getDouble()).setGeometry(geom);
                                    }
                                }
                                return null;
                            })
                            .filterDrop(Objects::isNull)
                            .filterKeep(builder -> builder.isValid(ctxt));
        }

        public CompletableFuture<ExtendedIterator<StepImpl.Builder>> execute() {
            return exec(query).thenApply(processor);
        }
    }

    // package private for testing.
    class ObstacleImpl implements Obstacle {
        private final Literal wkt;
        private final Geometry geom;
        private final UUID uuid;
        private Resource rdf;

        ObstacleImpl(Geometry geom) {
            this(UUID.randomUUID(), geom);
        }

        ObstacleImpl(UUID uuid, Geometry geom) {
            this.uuid = uuid;
            this.geom = geom;
            this.wkt = ctxt.graphGeomFactory.asWKT(geom);
        }

        ObstacleImpl(Resource rdf, Literal wkt) {
            this.rdf = rdf;
            this.uuid = parseUUID(rdf);
            this.geom = ctxt.graphGeomFactory.fromWkt(wkt);
            this.wkt = wkt;
        }

        ObstacleImpl(Position startPosition, FrontsCoordinate relativeStart, FrontsCoordinate relativeEnd) {
            Location start = startPosition.nextPosition(relativeStart);
            Location end = startPosition.nextPosition(relativeEnd);
            double d = start.distance(end);
            int parts = (int) (d / (ctxt.scaleInfo.getResolution() / 2));
            double xIncr = (end.getX() - start.getX()) / (parts + 1);
            double yIncr = (end.getY() - start.getY()) / (parts + 1);
            Coordinate[] part = new Coordinate[parts + 1];
            part[0] = start.getCoordinate();
            for (int i = 1; i < parts; i++) {
                part[i] = new Coordinate(part[i - 1].x + xIncr, part[i - 1].y + yIncr);
            }
            part[parts] = end.getCoordinate();
            geom = ctxt.geometryUtils.asLine(part);
            wkt = ctxt.graphGeomFactory.asWKT(geom);
            uuid = UUID.randomUUID();
        }

        ObstacleImpl(Position startPosition, FrontsCoordinate relativeLocation) {
            Position absoluteObstacle = startPosition.nextPosition(relativeLocation);
            if (Double.isNaN(absoluteObstacle.getHeading())) {
                System.err.println("NAN");
            }
            absoluteObstacle = Position.from(ctxt.scaleInfo.round(absoluteObstacle.getCoordinate()),
                    absoluteObstacle.getHeading());
            geom = ctxt.geometryUtils.asPoint(absoluteObstacle);
            wkt = ctxt.graphGeomFactory.asWKT(geom);
            uuid = UUID.randomUUID();
        }

        private static UUID parseUUID(Resource rdf) {
            return UUID.fromString(rdf.getURI().substring("urn:uuid:".length()));
        }

        @Override
        public Literal wkt() {
            return wkt;
        }

        @Override
        public Geometry geom() {
            return geom;
        }

        @Override
        public UUID uuid() {
            return uuid;
        }

        @Override
        public Resource rdf() {
            Resource result = rdf;
            if (result == null) {
                result = rdf = ResourceFactory.createResource("urn:uuid:" + uuid().toString());
            }
            return result;
        }

        @Override
        public int hashCode() {
            return Obstacle.hashCode(this);
        }

        @Override
        public boolean equals(Object obj) {
            return Obstacle.equalsImpl(this, obj);
        }

        @Override
        public String toString() {
            return wkt.getLexicalForm();
        }
    }

    /**
     * Create a cloud of obsacle points in a single geometry.
     */
    private class ObstacleHandler {
        private Collection<Geometry> makeCloud(Obstacle obstacle, Collection<? extends Obstacle> others) {
            Set<Coordinate> cSet = new HashSet<>();
            Consumer<Obstacle> co = o -> cSet.addAll(Arrays.asList(o.geom().getCoordinates()));
            co.accept(obstacle);
            others.forEach(co);

            if (cSet.size() > 2) {
                PointCloudSorter pcs = new PointCloudSorter(MapImpl.this.getContext(), cSet);
                return pcs.walk();
            }

            return List.of(ctxt.geometryFactory.createLineString(cSet.toArray(new Coordinate[0])));
        }

        private Set<Obstacle> mergeIntersectOrTouch(Obstacle obstacle, Set<ObstacleImpl> solns) {
            Set<Obstacle> solution = new HashSet<>();
            solns.remove(obstacle);
            if (solns.isEmpty()) {
                solution.add(obstacle);
            } else if (solns.size() == 1) {
                Obstacle obs = solns.iterator().next();
                if (obstacle.geom().coveredBy(obs.geom())) {
                    return Collections.emptySet();
                }
            } else {
                UpdateRequest req = new UpdateRequest();
                Collection<Geometry> result = makeCloud(obstacle, solns);
                for (Obstacle obst : solns) {
                    req.add(new UpdateBuilder()
                            .addDelete(Namespace.PlanningModel, obst.rdf(), Namespace.p, Namespace.o)
                            .addGraph(Namespace.UnionModel,
                                    new WhereBuilder().addWhere(obst.rdf(), Namespace.p, Namespace.o))
                            .build());
                }
                Model merged = ModelFactory.createDefaultModel();
                for (Geometry g : result) {
                    ObstacleImpl obst = new ObstacleImpl(g);
                    obst.in(merged);
                    solution.add(obst);
                }
                req.add(new UpdateBuilder().addInsert(Namespace.PlanningModel, merged).build());
                doUpdate(req);
            }
            return solution;
        }

        Set<Obstacle> addObstacle(Obstacle obst) {
            // find all Obstacles that this obstacle will intersect or touch
            // if there are any, merge them together.
            // if not just write this on to the graph.
            Var wkt = Var.alloc("wkt");

            SelectBuilder selectBuilder = new SelectBuilder()
                    .setDistinct(true).addVar(Namespace.s)
                    .addVar(wkt)
                    .from(Namespace.UnionModel.getURI()) //
                    .addWhere(Namespace.s, Geo.AS_WKT_NODE, wkt) //
                    .addWhere(Namespace.s, RDF.type, Namespace.Obst) //
                    .addFilter(ctxt.graphGeomFactory.isNearby(exprF, obst.wkt(), wkt, ctxt.scaleInfo.getResolution()));

            Set<Obstacle> work;
            Set<ObstacleImpl> solns = new HashSet<>();
            exec(selectBuilder).thenAccept(
                    resultSet -> resultSet.forEachRemaining(soln -> {
                        solns.add(
                                new ObstacleImpl(soln.getResource(Namespace.s.getName()), soln.getLiteral("wkt")));
                    })
            ).join();
            if (solns.isEmpty()) {
                LOG.debug("Adding obstacle: {}", obst);
                Model merged = ModelFactory.createDefaultModel();
                obst.in(merged);
                doUpdate(new UpdateRequest().add(new UpdateBuilder().addInsert(Namespace.PlanningModel, merged).build()));
                work = Set.of(obst);
            } else {
               work = mergeIntersectOrTouch(obst, solns);
            }
            if (!work.isEmpty()) {
                // delete any Coords that are within buffer of any of the work geometries.
                Var obstRes = Var.alloc("obst");
                Var otherWkt = Var.alloc("otherWkt");
                doUpdate(new UpdateRequest().add(new UpdateBuilder().addDelete(Namespace.PlanningModel, Namespace.s, Namespace.p, Namespace.o)
                        .addGraph(Namespace.UnionModel, new WhereBuilder() //
                                .addWhere(Namespace.s, Namespace.p, Namespace.o) //
                                .addWhere(Namespace.s, RDF.type, Namespace.Coord) //
                                .addWhere(Namespace.s, Geo.AS_WKT_NODE, wkt) //
                                .addWhere(obstRes, Geo.AS_WKT_NODE, otherWkt)
                                .addFilter(exprF.lt(ctxt.graphGeomFactory.calcDistance(exprF, wkt, otherWkt),
                                        ctxt.chassisInfo.radius))
                                .addFilter(exprF.in(exprF.asExpr(obstRes),
                                        exprF.asList(
                                                work.stream().map(Obstacle::rdf).toList().toArray()))))
                        .build()));
            }
            return work;
        }

        boolean isObstacle(FrontsCoordinate point) {
            Literal pointWKT = ctxt.graphGeomFactory.asWKT(point.getCoordinate());
            Var wkt = Var.alloc("wkt");
            AskBuilder ask = new AskBuilder().addGraph(Namespace.UnionModel,
                    new WhereBuilder().addWhere(Namespace.s, RDF.type, Namespace.Obst) //
                            .addWhere(Namespace.s, Geo.AS_WKT_PROP, wkt) //
                            .addFilter(ctxt.graphGeomFactory.isNearby(exprF, pointWKT, wkt, ctxt.scaleInfo.getResolution())));
            return ask(ask);
        }

        CompletableFuture<Set<Obstacle>> getObstacles() {
            Var wkt = Var.alloc("wkt");
            SelectBuilder sb = new SelectBuilder().addVar(Namespace.s).addVar(wkt) //
                    .addGraph(Namespace.UnionModel, new WhereBuilder().addWhere(Namespace.s, RDF.type, Namespace.Obst) //
                            .addWhere(Namespace.s, Geo.AS_WKT_PROP, wkt));

            return exec(sb).thenApply(resultSet -> {
                Set<Obstacle> result = new HashSet<>();
                resultSet.forEachRemaining(soln -> {
                    result.add(new ObstacleImpl(soln.getResource(Namespace.s.getName()), soln.getLiteral(wkt.getName())));
                });
                return result;
            });
        }
    }

}
