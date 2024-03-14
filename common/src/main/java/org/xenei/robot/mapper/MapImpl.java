package org.xenei.robot.mapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.math3.util.Precision;
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
import org.apache.jena.vocabulary.RDF;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.common.FrontsCoordinate;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.UnmodifiableCoordinate;
import org.xenei.robot.common.mapping.Map;
import org.xenei.robot.common.mapping.MapCoord;
import org.xenei.robot.common.mapping.Obstacle;
import org.xenei.robot.common.planning.Solution;
import org.xenei.robot.common.planning.Step;
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
        try (LockHandler lh = new LockHandler(Lock.WRITE)) {
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

    @Override
    public Coordinate adopt(Coordinate c) {
        double x = ctxt.scaleInfo.scale(c.getX());
        double y = ctxt.scaleInfo.scale(c.getY());
        return (Precision.equals(x, c.getX(), 0) && Precision.equals(y, c.getY(), 0)) ? c : new Coordinate(x, y);
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
        try (LockHandler lh = new LockHandler(Lock.READ)) {
            return data.getUnionModel().isEmpty();
        }
    }

    private void doUpdate(UpdateBuilder update) {
        try (LockHandler lh = new LockHandler(Lock.WRITE)) {
            UpdateExecutionFactory.create(update.build(), data).execute();
        }
    }

    private void doUpdate(UpdateRequest request) {
        try (LockHandler lh = new LockHandler(Lock.WRITE)) {
            UpdateExecutionFactory.create(request, data).execute();
        }
    }

    public boolean ask(AskBuilder ask) {
        try (LockHandler lh = new LockHandler(Lock.READ);
                QueryExecution exec = QueryExecutionFactory.create(ask.build(), data)) {
            return exec.execAsk();
        }
    }

    public void dump(Resource modelName, Consumer<Model> consumer) {
        try (LockHandler lh = new LockHandler(Lock.READ)) {
            consumer.accept(data.getNamedModel(modelName));
        }
    }

    /**
     * executes the select query and processes the result with the processor.
     * Processing stops when processor returns false.
     * 
     * @param select the SelectBuilder to execute.
     * @param processor the processor to run to handle the results.
     */
    void exec(SelectBuilder select, Predicate<QuerySolution> processor) {
        try (LockHandler lh = new LockHandler(Lock.READ);
                QueryExecution qexec = QueryExecutionFactory.create(select.build(), data)) {
            Iterator<QuerySolution> results = qexec.execSelect();
            while (results.hasNext() && processor.test(results.next())) {
                // all work is done in the processor above
            }
        }
    }

    Model construct(ConstructBuilder select) {
        try (LockHandler lh = new LockHandler(Lock.READ);
                QueryExecution qexec = QueryExecutionFactory.create(select.build(), data)) {
            return qexec.execConstruct();
        }
    }

    @Override
    public Optional<Step> addCoord(Coordinate coord, double distance, boolean visited, boolean isIndirect) {
        MapCoordinate mapCoord = new MapCoordinate(coord);
        UpdateRequest req = new UpdateRequest();
        if (exists(mapCoord, Namespace.Coord)) {
            WhereBuilder where = new WhereBuilder().addWhere(Namespace.s, RDF.type, Namespace.Coord)
                    .addWhere(Namespace.s, Geo.AS_WKT_PROP, ctxt.graphGeomFactory.asWKT(mapCoord.getCoordinate()));
            UpdateBuilder newDat = new UpdateBuilder()
                    .addInsert(Namespace.PlanningModel, Namespace.s, Namespace.distance, distance).addWhere(where);
            if (visited) {
                newDat.addInsert(Namespace.PlanningModel, Namespace.s, Namespace.visited, visited);
            }
            if (isIndirect) {
                newDat.addInsert(Namespace.PlanningModel, Namespace.s, Namespace.isIndirect, isIndirect);
            }
            // clear and set existing value
            req.add(new UpdateBuilder().addDelete(Namespace.PlanningModel, Namespace.s, Namespace.distance, Namespace.o)
                    .addWhere(Namespace.s, Namespace.distance, Namespace.o).addWhere(where).build())
                    .add(newDat.build());
        } else {
            // no existing record
            Resource qA = ctxt.graphGeomFactory.asRDF(mapCoord, Namespace.Coord);
            qA.addLiteral(Namespace.distance, distance);
            if (visited) {
                qA.addLiteral(Namespace.visited, visited);
            }
            if (isIndirect) {
                qA.addLiteral(Namespace.isIndirect, isIndirect);
            }
            req.add(new UpdateBuilder().addInsert(Namespace.PlanningModel, qA.getModel()).build());
        }

        doUpdate(req);
        LOG.debug("Added {} for {}", mapCoord, coord);
        return Optional.ofNullable(distance <= 0 ? null
                : StepImpl.builder().setCoordinate(mapCoord).setDistance(distance)
                        .setCost(isIndirect ? distance * 2 : distance).build(ctxt));
    }

    @SuppressWarnings("unchecked")
    @Override
    public Set<Obstacle> addObstacle(Obstacle obst) {
        return (Set<Obstacle>) obstacleHandler.addObstacle(obst);
    }

    @Override
    public boolean isObstacle(Coordinate point) {
        return obstacleHandler.isObstacle(point);
    }

    @Override
    public Set<Obstacle> getObstacles() {
        return obstacleHandler.getObstacles();
    }

    /**
     * Gets the Step for the coordinates.
     * 
     * @param location The location to get the Step for
     * @return the Step for the location.
     */
    public Optional<Step> getStep(double distance, FrontsCoordinate location) {
        MapCoordinate coordinate = new MapCoordinate(location.getCoordinate());

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
                .addBind(SPARQL.costCalc(distance, dist, indirectFlg), cost);

        StepImpl.Builder builder = StepImpl.builder();

        Predicate<QuerySolution> processor = soln -> {
            Geometry geometry = ctxt.graphGeomFactory.fromWkt(soln.getLiteral(geom.getName()));
            builder.setCoordinate(coordinate).setCost(soln.getLiteral(cost.getName()).getDouble())
                    .setDistance(soln.getLiteral(dist.getName()).getDouble()).setGeometry(geometry);
            return false;
        };

        exec(sb, processor);

        return builder.isValid(ctxt) ? Optional.of(builder.build(ctxt)) : Optional.empty();
    }

    /**
     * Add the plan record to the map
     * 
     * @param record the plan record to add
     * @return true if the record updated the map, false otherwise.
     */
    @Override
    public Coordinate[] addPath(Coordinate... coords) {
        return addPath(Namespace.PlanningModel, Arrays.stream(coords).map(MapCoordinate::new));
    }

    /**
     * Add the plan record to the map
     * 
     * @param records the coordinates of the path.
     * @return true if the record updated the map, false otherwise.
     */
    @Override
    public Coordinate[] addPath(Resource model, Coordinate... coords) {
        return addPath(model, Arrays.stream(coords).map(MapCoordinate::new));
    }

    /**
     * Add a stream of coordinates as a path.
     * 
     * @param record the plan record to add
     * @return true if the record updated the map, false otherwise.
     */
    private Coordinate[] addPath(Resource model, Stream<MapCoordinate> coords) {
        List<MapCoordinate> lst = coords.toList();
        Coordinate[] points = new Coordinate[lst.size()];
        int[] idx = { 0 };
        lst.stream().forEach(c -> points[idx[0]++] = c.getCoordinate());
        Literal path = ctxt.graphGeomFactory.asWKTString(points);
        Resource tn = ResourceFactory.createResource();
        List<Triple> triples = new ArrayList<>();
        triples.add(Triple.create(tn.asNode(), RDF.type.asNode(), Namespace.Path.asNode()));
        triples.add(Triple.create(tn.asNode(), Geo.AS_WKT_PROP.asNode(), path.asNode()));
        doUpdate(new UpdateBuilder().addInsert(model, triples));
        LOG.debug("Path <{} {}>", points[0], points[points.length - 1]);
        return points;
    }

    @Override
    public void cutPath(Coordinate a, Coordinate b) {
        cutPath(Namespace.PlanningModel, a, b);
    }

    private boolean exists(MapCoordinate coordinate, Resource type) {
        AskBuilder ask = new AskBuilder().from(Namespace.UnionModel.getURI()) //
                .addWhere(Namespace.s, RDF.type, type) //
                .addWhere(Namespace.s, Namespace.x, coordinate.getX()) //
                .addWhere(Namespace.s, Namespace.y, coordinate.getY());
        return ask(ask);
    }

    public void cutPath(Resource model, Coordinate a, Coordinate b) {
        Var ra = Var.alloc("a");
        Var rb = Var.alloc("b");
        MapCoordinate mapA = new MapCoordinate(a);
        MapCoordinate mapB = new MapCoordinate(b);

        UpdateBuilder ub = new UpdateBuilder().addDelete(model, Namespace.s, Namespace.p, Namespace.o)
                .addWhere(Namespace.s, Namespace.p, Namespace.o).addWhere(Namespace.s, Namespace.point, ra)
                .addWhere(Namespace.s, Namespace.point, rb).addWhere(ra, Namespace.x, mapA.getX())
                .addWhere(ra, Namespace.y, mapA.getY()).addWhere(rb, Namespace.x, mapB.getX())
                .addWhere(rb, Namespace.y, mapB.getY());
        doUpdate(ub);
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
    public boolean isClearPath(Coordinate from, Coordinate target) {
        LOG.debug("checking clearView from {} to {} ", from, target);
        Literal pathWkt = ctxt.graphGeomFactory.asWKTPath(ctxt.chassisInfo.radius, from, target);
        Var wkt = Var.alloc("wkt");
        AskBuilder ask = new AskBuilder().from(Namespace.UnionModel.getURI()) //
                .addWhere(Namespace.s, RDF.type, Namespace.Obst) //
                .addWhere(Namespace.s, Geo.AS_WKT_PROP, wkt)
                .addFilter(exprF.eq(ctxt.graphGeomFactory.calcDistance(exprF, pathWkt, wkt), 0));
        return !ask(ask);
    }

    /**
     * Updates the property of the coordinates record in the model to have the
     * specified value.
     * 
     * 
     * @param model The model to update.
     * @param coordinate the node to update
     * @param property the property to update
     * @param value the value to set the property to.
     * @return true if the node was update, false if the node did not exist.
     */
    boolean updateCoordinate(Resource model, Coordinate coordinate, Property property, Object value) {
        LOG.debug("updatating {} {} {} to {}", model.getLocalName(), CoordUtils.toString(coordinate, 1),
                property.getLocalName(), value);
        MapCoordinate mapCoord = new MapCoordinate(coordinate);

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
            doUpdate(req);
            return true;
        }
        return false;
    }

    /**
     * Calculate the best next position based on the map and current coordinates.
     * 
     * @param currentCoords the current coordinates
     * @return Optional containing either either the PlanRecord for the next
     * position, or empty if none found.
     */
    @Override
    public Optional<Step> getBestStep(Coordinate currentCoords) {
        if (data.isEmpty()) {
            LOG.debug("No map points");
            return Optional.empty();
        }

        StepImpl.Builder[] builder = { null };

        StepQuery stepQuery = new StepQuery(currentCoords, (b) -> {
            LOG.debug("getBest() -> {}", b);
            builder[0] = b;
            return false;
        });

        stepQuery.execute();
        if (builder[0] == null) {
            return Optional.empty();
        }

        if (!builder[0].isValid(ctxt)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Query\n" + MapReports.dumpQuery(MapImpl.this, stepQuery.query));
                LOG.debug("Distance\n" + MapReports.dumpDistance(MapImpl.this, currentCoords));
                LOG.debug("Obstacles\n" + MapReports.dumpObstacleDistance(MapImpl.this));
                MapImpl.this.getObstacles().forEach(s -> LOG.debug(s.toString()));
                LOG.debug("Model\n" + MapReports.dumpModel(MapImpl.this));
                LOG.debug("No Selected map points");
            }
            return Optional.empty();
        }
        return Optional.of(builder[0].build(ctxt));
    }

    @Override
    public void setVisited(Coordinate finalTarget, Coordinate coord) {
        if (!updateCoordinate(Namespace.PlanningModel, coord, Namespace.visited, Boolean.TRUE)) {
            MapCoordinate mapCoord = new MapCoordinate(coord);
            UpdateRequest req = new UpdateRequest();
            Resource qA = ctxt.graphGeomFactory.asRDF(mapCoord, Namespace.Coord);
            req.add(new UpdateBuilder().addInsert(Namespace.PlanningModel, qA.getModel()) //
                    .addInsert(Namespace.PlanningModel, qA.asResource(), Namespace.visited, true) //
                    .addInsert(Namespace.PlanningModel, qA.asResource(), Namespace.distance,
                            mapCoord.distance(finalTarget)) //
                    .build());
            doUpdate(req);
        }
    }

    @Override
    public Coordinate recalculate(Coordinate target) {
        LOG.debug("recalculate: {}", target);
        Var distance = Var.alloc("distance");
        Var wkt = Var.alloc("wkt");
        MapCoordinate result = new MapCoordinate(target);
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

        getCoords().stream().filter(mc -> !isClearPath(mc.location.getCoordinate(), target))
                .forEach(mc -> req.add(new UpdateBuilder() //
                        .addInsert(Namespace.PlanningModel, Namespace.s, Namespace.isIndirect, true) //
                        .addGraph(Namespace.UnionModel, new WhereBuilder() //
                                .addWhere(Namespace.s, Geo.AS_WKT_PROP, ctxt.graphGeomFactory.asWKT(mc.geometry))) //
                        .build()));

        doUpdate(req);

        return result.getCoordinate();
    }

    @Override
    public Collection<MapCoord> getCoords() {
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

        Predicate<QuerySolution> processor = soln -> {
            Geometry geom = ctxt.graphGeomFactory.fromWkt(soln.getLiteral(wkt.getName()));
            Literal litIndirect = soln.getLiteral(indirect.getName());
            result.add(new MapCoord( //
                    soln.getLiteral(x.getName()).getDouble(), //
                    soln.getLiteral(y.getName()).getDouble(), //
                    litIndirect == null ? false : litIndirect.getBoolean(), geom));
            return true;
        };

        exec(sb, processor);
        return result;
    }

    @Override
    public Collection<Step> getSteps(Coordinate currentPosition) {
        List<Step> result = new ArrayList<>();

        new StepQuery(currentPosition, (b) -> {
            result.add(b.build(ctxt));
            return true;
        }).execute();

        return result;
    }

    @Override
    public void recordSolution(Solution solution) {
        solution.simplify((x, y) -> this.isClearPath(x, y));
        addPath(Namespace.BaseModel, solution.stream().map(c -> new MapCoordinate(c)));
    }

    @Override
    public boolean areEquivalent(Coordinate a, Coordinate b) {
        return new MapCoordinate(a).equals2D(new MapCoordinate(b), ctxt.scaleInfo.getResolution());
    }

    Model getModel() {
        return data.getUnionModel();
    }

    @Override
    public void updateIsIndirect(Coordinate finalTarget, Set<Obstacle> newObstacles) {
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

        List<Coordinate> candidates = new ArrayList<>();

        Predicate<QuerySolution> processor = soln -> {
            candidates.add(
                    new Coordinate(soln.getLiteral(x.getName()).getDouble(), soln.getLiteral(y.getName()).getDouble()));
            return true;
        };

        this.exec(sb, processor);

        List<Literal> updateCoords = new ArrayList<>();

        for (Coordinate c : candidates) {
            Geometry path = ctxt.geometryUtils.asPath(ctxt.chassisInfo.radius, c, finalTarget);
            for (Obstacle obst : newObstacles) {
                if (path.distance(obst.geom()) == 0) {
                    updateCoords.add(ctxt.graphGeomFactory.asWKT(c));
                    break;
                }
            }
        }

        if (!updateCoords.isEmpty()) {
            UpdateBuilder ub = new UpdateBuilder()
                    .addInsert(Namespace.PlanningModel, Namespace.s, Namespace.isIndirect, Boolean.TRUE) //
                    .addGraph(Namespace.UnionModel, new WhereBuilder() //
                            .addWhere(Namespace.s, RDF.type, Namespace.Coord) //
                            .addWhere(Namespace.s, Geo.AS_WKT_NODE, wkt)
                            .addFilter(exprF.in(wkt, updateCoords.toArray())));
            doUpdate(ub);
        }
    }

    @Override
    public Obstacle createObstacle(Position startPosition, Location relativeLocation) {
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
    public Optional<Location> look(Location from, double heading, int maxRange) {

        Coordinate target = from.plus(CoordUtils.fromAngle(heading, maxRange));

        Literal pathWkt = ctxt.graphGeomFactory.asWKTPath(ctxt.chassisInfo.radius, from.getCoordinate(), target);
        Var wkt = Var.alloc("wkt");
        Var dist = Var.alloc("dist");
        Literal fromWkt = ctxt.graphGeomFactory.asWKT(from.getCoordinate());
        SelectBuilder look = new SelectBuilder().addVar(dist).from(Namespace.UnionModel.getURI()) //
                .addWhere(Namespace.s, RDF.type, Namespace.Obst) //
                .addWhere(Namespace.s, Geo.AS_WKT_PROP, wkt)
                .addBind(ctxt.graphGeomFactory.calcDistance(exprF, fromWkt, wkt), dist)
                .addFilter(exprF.eq(ctxt.graphGeomFactory.calcDistance(exprF, pathWkt, wkt), 0))
                .addFilter(exprF.lt(dist, maxRange)).addOrderBy(dist, Order.ASCENDING).setLimit(1);

        double range[] = { -1 };

        Predicate<QuerySolution> processor = soln -> {
            range[0] = soln.getLiteral(dist.getName()).getDouble();
            return false;
        };

        exec(look, processor);

        if (range[0] > -1) {
            return Optional.of(Location.from(from.plus(CoordUtils.fromAngle(heading, range[0]))));
        }
        return Optional.empty();
    }

    private class MapCoordinate implements FrontsCoordinate {

        UnmodifiableCoordinate coord;

        MapCoordinate(Coordinate coordinate) {
            coord = UnmodifiableCoordinate.make(adopt(coordinate));
        }

        @Override
        public UnmodifiableCoordinate getCoordinate() {
            return coord;
        }

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
        final Predicate<QuerySolution> processor;

        StepQuery(Coordinate currentCoords, Predicate<StepImpl.Builder> builderPred) {
            MapCoordinate mapCoords = new MapCoordinate(currentCoords);
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

            processor = soln -> {
                Geometry geom = ctxt.graphGeomFactory.fromWkt(soln.getLiteral(otherWkt.getName()));
                for (Coordinate candidate : geom.getCoordinates()) {
                    Literal candidateWkt = ctxt.graphGeomFactory.asWKT(candidate);
                    checkVisited.setVar(otherWkt, candidateWkt);
                    // if not visited and has a clear path
                    if (!ask(checkVisited) && isClearPath(currentCoords, candidate)) {
                        StepImpl.Builder builder = StepImpl.builder().setCoordinate(candidate)
                                .setCost(soln.getLiteral(cost.getName()).getDouble())
                                .setDistance(soln.getLiteral(dist.getName()).getDouble()).setGeometry(geom);
                        return builderPred.test(builder);
                    }
                }
                return true;
            };
        }

        public void execute() {
            exec(query, processor);
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

        ObstacleImpl(Coordinate start, Coordinate end) {
            double d = start.distance(end);
            int parts = (int) (d / ctxt.scaleInfo.getHalfResolution());
            double xIncr = (end.x - start.x) / (parts + 1);
            double yIncr = (end.y - start.y) / (parts + 1);
            Coordinate[] part = new Coordinate[parts + 1];
            part[0] = start;
            for (int i = 1; i < parts; i++) {
                part[i] = new Coordinate(part[i - 1].x + xIncr, part[i - 1].y + yIncr);
            }
            part[parts] = end;
            geom = ctxt.geometryUtils.asLine(part);
            wkt = ctxt.graphGeomFactory.asWKT(geom);
            uuid = UUID.randomUUID();
        }

        ObstacleImpl(Position startPostition, Location relativeLocation) {
            Position absoluteObstacle = startPostition.nextPosition(relativeLocation);
            absoluteObstacle = Position.from(ctxt.scaleInfo.precise(absoluteObstacle.getCoordinate()),
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

    private class ObstacleHandler {
        private Geometry makeCloud(Obstacle obstacle, Collection<? extends Obstacle> others) {
            Set<Coordinate> cSet = new HashSet<>();
            Consumer<Obstacle> co = o -> Arrays.stream(o.geom().getCoordinates()).forEach(cSet::add);
            co.accept(obstacle);
            others.forEach(co);

            if (cSet.size() > 2) {
                PointCloudSorter pcs = new PointCloudSorter(MapImpl.this.getContext(), cSet);
                return pcs.walk();
            }

            return ctxt.geometryFactory.createLineString(cSet.toArray(new Coordinate[cSet.size()]));
        }

        private Set<Obstacle> mergeIntersectOrTouch(UpdateRequest req, Obstacle obstacle) {
            Var otherWkt = Var.alloc("otherWkt");
            SelectBuilder sb = new SelectBuilder().setDistinct(true).addVar(Namespace.s).addVar(otherWkt) //
                    .from(Namespace.UnionModel.getURI()) //
                    .addWhere(Namespace.s, Geo.AS_WKT_NODE, otherWkt) //
                    .addWhere(Namespace.s, RDF.type, Namespace.Obst).addFilter(ctxt.graphGeomFactory.isNearby(exprF,
                            obstacle.wkt(), otherWkt, ctxt.scaleInfo.getResolution()));

            Set<ObstacleImpl> solns = new HashSet<>();

            Predicate<QuerySolution> processor = soln -> {
                solns.add(
                        new ObstacleImpl(soln.getResource(Namespace.s.getName()), soln.getLiteral(otherWkt.getName())));
                return true;
            };

            exec(sb, processor);

            Set<Obstacle> solution = new HashSet<>();

            if (solns.isEmpty()) {
                Obstacle obstImpl = obstacle;
                Resource r = obstImpl.in(ModelFactory.createDefaultModel());
                req.add(new UpdateBuilder().addInsert(Namespace.PlanningModel, r.getModel()).build());
                solution.add(obstImpl);
            } else {
                solns.remove(obstacle);
                if (!solns.isEmpty()) {
                    Geometry result = makeCloud(obstacle, solns);
                    for (Obstacle obst : solns) {
                        req.add(new UpdateBuilder()
                                .addDelete(Namespace.PlanningModel, obst.rdf(), Namespace.p, Namespace.o)
                                .addGraph(Namespace.UnionModel,
                                        new WhereBuilder().addWhere(obst.rdf(), Namespace.p, Namespace.o))
                                .build());
                    }

                    Model merged = ModelFactory.createDefaultModel();
                    ObstacleImpl obst = new ObstacleImpl(result);
                    obst.in(merged);
                    solution.add(obst);
                    req.add(new UpdateBuilder().addInsert(Namespace.PlanningModel, merged).build());
                }
            }
            return solution;
        }

        Set<? extends Obstacle> addObstacle(Obstacle obst) {
            // find all Obstacles that this obstacle will intersect or touch
            // if there are any, merge them together.
            // if not just write this on to the graph.
            Var wkt = Var.alloc("wkt");
            AskBuilder askBuilder = new AskBuilder().from(Namespace.UnionModel.getURI()) //
                    .addWhere(Namespace.s, Geo.AS_WKT_NODE, wkt) //
                    .addWhere(Namespace.s, RDF.type, Namespace.Obst) //
                    .addFilter(ctxt.graphGeomFactory.isNearby(exprF, obst.wkt(), wkt, ctxt.scaleInfo.getResolution()));

            UpdateRequest req = new UpdateRequest();
            Set<Obstacle> work;
            if (ask(askBuilder)) {
                work = mergeIntersectOrTouch(req, obst);
            } else {
                Model merged = ModelFactory.createDefaultModel();
                obst.in(merged);
                req.add(new UpdateBuilder().addInsert(Namespace.PlanningModel, merged).build());
                work = Set.of(obst);
            }
            // delete any Coords that are within buffer of any of the work geometries.
            Var obstRes = Var.alloc("obst");
            Var otherWkt = Var.alloc("otherWkt");
            req.add(new UpdateBuilder().addDelete(Namespace.PlanningModel, Namespace.s, Namespace.p, Namespace.o)
                    .addGraph(Namespace.UnionModel, new WhereBuilder() //
                            .addWhere(Namespace.s, Namespace.p, Namespace.o) //
                            .addWhere(Namespace.s, RDF.type, Namespace.Coord) //
                            .addWhere(Namespace.s, Geo.AS_WKT_NODE, wkt) //
                            .addWhere(obstRes, Geo.AS_WKT_NODE, otherWkt)
                            .addFilter(exprF.lt(ctxt.graphGeomFactory.calcDistance(exprF, wkt, otherWkt),
                                    ctxt.chassisInfo.radius))
                            .addFilter(exprF.in(exprF.asExpr(obstRes),
                                    exprF.asList(
                                            work.stream().map(Obstacle::rdf).collect(Collectors.toList()).toArray()))))
                    .build());
            doUpdate(req);
            return work;
        }

        boolean isObstacle(Coordinate point) {
            Literal pointWKT = ctxt.graphGeomFactory.asWKT(point);
            Var wkt = Var.alloc("wkt");
            AskBuilder ask = new AskBuilder().addGraph(Namespace.UnionModel,
                    new WhereBuilder().addWhere(Namespace.s, RDF.type, Namespace.Obst) //
                            .addWhere(Namespace.s, Geo.AS_WKT_PROP, wkt) //
                            .addFilter(ctxt.graphGeomFactory.intersects(exprF, pointWKT, wkt)));
            return ask(ask);
        }

        Set<Obstacle> getObstacles() {
            Var wkt = Var.alloc("wkt");

            SelectBuilder sb = new SelectBuilder().addVar(Namespace.s).addVar(wkt) //
                    .addGraph(Namespace.UnionModel, new WhereBuilder().addWhere(Namespace.s, RDF.type, Namespace.Obst) //
                            .addWhere(Namespace.s, Geo.AS_WKT_PROP, wkt));

            Set<Obstacle> result = new HashSet<>();

            Predicate<QuerySolution> processor = soln -> {
                result.add(new ObstacleImpl(soln.getResource(Namespace.s.getName()), soln.getLiteral(wkt.getName())));
                return true;
            };

            exec(sb, processor);

            return result;
        }
    }

}
