package org.xenei.robot.mapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
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
import org.locationtech.jts.algorithm.hull.ConcaveHull;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.operation.linemerge.LineMerger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.common.FrontsCoordinate;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.UnmodifiableCoordinate;
import org.xenei.robot.common.mapping.Map;
import org.xenei.robot.common.mapping.Obstacle;
import org.xenei.robot.common.planning.Solution;
import org.xenei.robot.common.planning.Step;
import org.xenei.robot.common.utils.RobutContext;
import org.xenei.robot.common.utils.CoordUtils;
import org.xenei.robot.mapper.rdf.Namespace;

public class MapImpl implements Map {
    private static final Logger LOG = LoggerFactory.getLogger(MapImpl.class);
    private final RobutContext ctxt;
    private final Dataset data;
    private final ExprFactory exprF;
    private final ObstacleHandler obstacleHandler;
    private final double buffer = 0.25; // FIXME

    public static PrefixMapping getPrefixes() {
        return PrefixMapping.Factory.create()
                .setNsPrefixes(PrefixMapping.Standard).setNsPrefixes(Namespace.getPrefixes());
    }

    public MapImpl(RobutContext ctxt) {
        this.ctxt = ctxt;
        data = DatasetFactory.create();
        data.getDefaultModel().setNsPrefixes(getPrefixes());
        // data.getDefaultModel().add(GraphModFactory.asRDF(new Coordinate(0,0), null,
        // null).getModel());
        data.addNamedModel(Namespace.BaseModel, defaultModel());
        data.addNamedModel(Namespace.PlanningModel, defaultModel());
        exprF = new ExprFactory(data.getPrefixMapping());
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

    private static Model defaultModel() {
        return ModelFactory.createDefaultModel().setNsPrefixes(getPrefixes());
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
    public Optional<Step> addCoord(Coordinate target, double distance, boolean visited, boolean isIndirect) {
        MapCoordinate mapCoord = new MapCoordinate(target);
        UpdateRequest req = new UpdateRequest();
        if (exists(mapCoord, Namespace.Coord)) {
            WhereBuilder where = new WhereBuilder().addWhere(Namespace.s, RDF.type, Namespace.Coord)
                    .addWhere(Namespace.s, Namespace.wkt, ctxt.graphGeomFactory.asWKT(mapCoord.getCoordinate()));
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
        LOG.debug("Added {} for {}", mapCoord, target);
        return Optional.ofNullable(distance<=0 ? null :
         StepImpl.builder().setCoordinate(mapCoord).setDistance(distance)
                .setCost(isIndirect ? distance * 2 : distance).build(ctxt));
    }

    private Geometry cell(Coordinate point) {
        return ctxt.geometryUtils.asPolygon(point, ctxt.scaleInfo.getResolution() / 2, 4);

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
    public Optional<Step> getStep(FrontsCoordinate location) {
        MapCoordinate coordinate = new MapCoordinate(location.getCoordinate());

        Var geom = Var.alloc("geom");
        Var dist = Var.alloc("dist");
        Var indirect = Var.alloc("indirect");
        Var cost = Var.alloc("cost");

        SelectBuilder sb = new SelectBuilder().addVar(cost).addVar(dist).addVar(geom) //
                .from(Namespace.PlanningModel.getURI()) //
                .addWhere(Namespace.s, RDF.type, Namespace.Coord) //
                .addWhere(Namespace.s, Namespace.distance, dist) //
                .addWhere(Namespace.s, Namespace.x, coordinate.getX()) //
                .addWhere(Namespace.s, Namespace.y, coordinate.getY()) //
                .addWhere(Namespace.s, Namespace.wkt, geom) //
                .addOptional(Namespace.s, Namespace.isIndirect, indirect) //
                .addBind(SPARQL.indirectCalc(dist, indirect), cost);

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
        UpdateRequest req = new UpdateRequest();
        WhereBuilder wb = new WhereBuilder();
        List<Triple> triples = new ArrayList<>();
        triples.add(Triple.create(tn.asNode(), RDF.type.asNode(), Namespace.Path.asNode()));
        triples.add(Triple.create(tn.asNode(), Namespace.wkt.asNode(), path.asNode()));
        int i = 0;
        for (MapCoordinate c : lst) {
            Optional<UpdateBuilder> updt = addBuilder(Namespace.PlanningModel, c, Namespace.Coord,
                    cell(c.getCoordinate()));
            if (updt.isPresent()) {
                req.add(updt.get().build());
            }
            Var v = Var.alloc("c" + i++);
            triples.add(Triple.create(tn.asNode(), Namespace.point.asNode(), v));
            wb.addWhere(v, Namespace.x, c.getX()).addWhere(v, Namespace.y, c.getY());
        }
        req.add(new UpdateBuilder().addInsert(model, triples).addGraph(Namespace.PlanningModel, wb).build());
        doUpdate(req);
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

    /**
     * Creates a builder for the coordinate if it does not exist.
     * 
     * @param model the name of the model to build the RDF data in/
     * @param coordinate the coordinate that the data exists at.
     * @param type Type of object
     * @param geometry the Geometry of the object
     * @return an optional containing the UpdateBuilder or and empty optional.
     */
    private Optional<UpdateBuilder> addBuilder(Resource model, MapCoordinate coordinate, Resource type,
            Geometry geometry) {
        return (!exists(coordinate, type))
                ? Optional.of(new UpdateBuilder().addInsert(model,
                        ctxt.graphGeomFactory.asRDF(coordinate.getCoordinate(), type, geometry).getModel()))
                : Optional.empty();
    }

//    private boolean add(Resource model, MapCoordinate coordinate, Resource type, Geometry geometry) {
//        Optional<UpdateBuilder> builder = addBuilder(model, coordinate, type, geometry);
//        if (builder.isPresent()) {
//            doUpdate(builder.get());
//            return true;
//        }
//        return false;
//    }

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
        Var ra = Var.alloc("a");
        Var rb = Var.alloc("b");
        MapCoordinate mapA = new MapCoordinate(a);
        MapCoordinate mapB = new MapCoordinate(b);

        WhereBuilder wb = new WhereBuilder().addWhere(Namespace.s, RDF.type, Namespace.Path)
                .addWhere(Namespace.s, Namespace.point, ra).addWhere(Namespace.s, Namespace.point, rb)
                .addWhere(ra, Namespace.x, mapA.getX()).addWhere(ra, Namespace.y, mapA.getY())
                .addWhere(rb, Namespace.x, mapB.getX()).addWhere(rb, Namespace.y, mapB.getY());
        AskBuilder ask = new AskBuilder().addGraph(Namespace.UnionModel, wb);
        return ask(ask);
    }

    @Override
    public boolean isClearPath(Coordinate from, Coordinate target, double buffer) {
        LOG.debug("checking clearView from {} to {} ", from, target);
        Literal pathWkt = ctxt.graphGeomFactory.asWKTPath(buffer, from, target);
        Var wkt = Var.alloc("wkt");
        AskBuilder ask = new AskBuilder().from(Namespace.UnionModel.getURI()) //
                .addWhere(Namespace.s, RDF.type, Namespace.Obst) //
                .addWhere(Namespace.s, Namespace.wkt, wkt)
                .addFilter(exprF.eq(ctxt.graphGeomFactory.calcDistance(exprF, pathWkt, wkt), 0));
        return !ask(ask);
    }

    /**
     * Updates the property of the coordinates record in the model to have the
     * specified value.
     * 
     * 
     * @param model The model to update.
     * @param type The type of the node to update.
     * @param coordinate the node to update
     * @param property the property to update
     * @param value the value to set the property to.
     */
    void updateCoordinate(Resource model, Resource type, Coordinate coordinate, Property property, Object value) {
        LOG.debug("updatating {} {} {} to {}", model.getLocalName(), CoordUtils.toString(coordinate, 1),
                property.getLocalName(), value);
        MapCoordinate mapCoord = new MapCoordinate(coordinate);
        if (exists(mapCoord, type)) {
            doUpdate(new UpdateRequest().add(new UpdateBuilder().addDelete(model, Namespace.s, property, Namespace.o)
                    .addGraph(Namespace.UnionModel, new WhereBuilder().addWhere(Namespace.s, property, Namespace.o)
                            .addWhere(Namespace.s, RDF.type, type).addWhere(Namespace.s, Namespace.x, mapCoord.getX())
                            .addWhere(Namespace.s, Namespace.y, mapCoord.getY()))
                    .build())
                    .add(new UpdateBuilder().addInsert(model, Namespace.s, property, value)
                            .addGraph(Namespace.UnionModel,
                                    new WhereBuilder().addWhere(Namespace.s, RDF.type, type)
                                            .addWhere(Namespace.s, Namespace.x, mapCoord.getX())
                                            .addWhere(Namespace.s, Namespace.y, mapCoord.getY()))
                            .build()));
        } else {
            LOG.info("coordinate {} of type {} is not on the map",
                    CoordUtils.toString(coordinate, ctxt.scaleInfo.decimalPlaces()), type);
        }

    }

    /**
     * Calculate the best next position based on the map and current coordinates.
     * 
     * @param currentCoords the current coordinates
     * @return Optional containing either either the PlanRecord for the next
     * position, or empty if none found.
     */
    @Override
    public Optional<Step> getBestStep(Coordinate currentCoords, double buffer) {
        if (data.isEmpty()) {
            LOG.debug("No map points");
            return Optional.empty();
        }
        MapCoordinate mapCoords = new MapCoordinate(currentCoords);
        Literal wkt = ctxt.graphGeomFactory.asWKT(mapCoords.getCoordinate());
        // cost of trip.
        Var cost = Var.alloc("cost");

        // distance current to other
        Var dist = Var.alloc("dist");

        Var other = Var.alloc("other");
        // wkt of other
        Var otherWkt = Var.alloc("otherWkt");
        // distance from other to target
        Var otherDist = Var.alloc("otherDist");

        Var indirect = Var.alloc("indirect");
        Var visited = Var.alloc("visited");
        Var other2 = Var.alloc("other2");
        Var other2Wkt = Var.alloc("other2Wkt");

        SelectBuilder query = new SelectBuilder().addVar(cost).addVar(otherWkt).addVar(other).addVar(dist) //
                .from(Namespace.UnionModel.getURI()).addWhere(Namespace.s, RDF.type, Namespace.Coord) //
                .addWhere(Namespace.s, Namespace.wkt, wkt).addWhere(other, RDF.type, Namespace.Coord) //
                .addOptional(other, Namespace.isIndirect, indirect) //
                .addWhere(other, Namespace.wkt, otherWkt) //
                .addWhere(other, Namespace.distance, otherDist) //
                .addOptional(other, Namespace.visited, visited) //
                .addFilter(exprF.and(exprF.ne(other, Namespace.s), exprF.not(exprF.bound(visited)))) //
                .addBind(ctxt.graphGeomFactory.calcDistance(exprF, otherWkt, wkt), dist) //
                .addBind(exprF.add(otherDist, SPARQL.indirectCalc(otherDist, indirect)), cost) //
                .addOrderBy(cost, Order.ASCENDING);

        // skip coords that are within the tolerance range of visited coords
        AskBuilder ask = new AskBuilder() //
                .addWhere(other2, Namespace.visited, "?ignore") //
                .addWhere(other2, RDF.type, Namespace.Coord)//
                .addWhere(other2, Namespace.wkt, other2Wkt) //
                .addFilter(exprF.le(ctxt.graphGeomFactory.calcDistance(exprF, otherWkt, other2Wkt), buffer)) //
        ;

        StepImpl.Builder builder = StepImpl.builder();

        Predicate<QuerySolution> processor = soln -> {

            Geometry geom = ctxt.graphGeomFactory.fromWkt(soln.getLiteral(otherWkt.getName()));
            for (Coordinate candidate : geom.getCoordinates()) {
                Literal candidateWkt = ctxt.graphGeomFactory.asWKT(candidate);
                ask.setVar(otherWkt, candidateWkt);

                if (!ask(ask) && isClearPath(currentCoords, candidate, buffer)) {
                    builder.setCoordinate(candidate).setCost(soln.getLiteral(cost.getName()).getDouble())
                            .setDistance(soln.getLiteral(dist.getName()).getDouble()).setGeometry(geom);

                    LOG.debug("getBest() -> {}", builder);
                    return false;
                }
            }
            return true;
        };

        exec(query, processor);

        if (!builder.isValid(ctxt)) {
            System.out.println( "Query");
            System.out.println(MapReports.dumpQuery(MapImpl.this, query));
            System.out.println(MapReports.dumpDistance(MapImpl.this));
            System.out.println(MapReports.dumpObstacleDistance(MapImpl.this));
            MapImpl.this.getObstacles().forEach(System.out::println);
            System.out.println(MapReports.dumpModel(MapImpl.this));
            LOG.debug("No Selected map points");
            return Optional.empty();
        }
        Step step = builder.build(ctxt);
        updateCoordinate(Namespace.PlanningModel, Namespace.Coord, step.getCoordinate(), Namespace.visited,
                Boolean.TRUE);
        return Optional.of(step);
    }

    @Override
    public Coordinate recalculate(Coordinate target, double buffer) {
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
                                .addWhere(Namespace.s, Namespace.wkt, wkt) //
                                .addBind(ctxt.graphGeomFactory.calcDistance(exprF, targ, wkt), distance))
                        .build());
        doUpdate(req);

        // check each item that has a distance and see if there is a path to the target,
        // if not then add the distance
        // to the adjustment.
        req = new UpdateRequest();
        Var candidate = Var.alloc("candidate");
        SelectBuilder sb = new SelectBuilder().from(Namespace.PlanningModel.getURI()) //
                .addVar(distance).addVar(candidate).addVar(wkt) //
                .addWhere(candidate, RDF.type, Namespace.Coord) //
                .addWhere(candidate, Namespace.wkt, wkt) //
                .addWhere(candidate, Namespace.distance, distance);

        List<Triple> insertRow = new ArrayList<>();

        Predicate<QuerySolution> processor = soln -> {
            Geometry g = ctxt.graphGeomFactory.fromWkt(soln.getLiteral(wkt.getName()));
            boolean clearView = false;
            for (Coordinate c : g.getCoordinates()) {
                if (isClearPath(c, target, buffer)) {
                    clearView = true;
                    break;
                }
            }
            if (!clearView) {
                insertRow.add(Triple.create(soln.getResource(candidate.getName()).asNode(),
                        Namespace.isIndirect.asNode(), sb.makeNode(Boolean.TRUE)));
            }
            return true;
        };

        exec(sb, processor);

        if (!insertRow.isEmpty()) {
            doUpdate(new UpdateBuilder().addInsert(Namespace.PlanningModel, insertRow));
        }
        return result.getCoordinate();
    }

    @Override
    public Collection<Step> getTargets() {
        Var distance = Var.alloc("distance");
        Var wkt = Var.alloc("wkt");
        Var cost = Var.alloc("cost");
        Var x = Var.alloc("x");
        Var y = Var.alloc("y");
        Var indirect = Var.alloc("indirect");

        SelectBuilder sb = new SelectBuilder().addVar(cost).addVar(wkt).addVar(x).addVar(y).addVar(distance) //
                .from(Namespace.PlanningModel.getURI()).addWhere(Namespace.s, RDF.type, Namespace.Coord) //
                .addOptional(Namespace.s, Namespace.isIndirect, indirect) //
                .addWhere(Namespace.s, Namespace.distance, distance) //
                .addWhere(Namespace.s, Namespace.wkt, wkt) //
                .addWhere(Namespace.s, Namespace.x, x).addWhere(Namespace.s, Namespace.y, y)
                .addBind(SPARQL.indirectCalc(distance, indirect), cost).addOrderBy(cost, Order.ASCENDING);

        SortedSet<Step> candidates = new TreeSet<Step>();

        Predicate<QuerySolution> processor = soln -> {
            Geometry geom = ctxt.graphGeomFactory.fromWkt(soln.getLiteral(wkt.getName()));
            candidates.add(StepImpl.builder().setCoordinate(new Coordinate( //
                    soln.getLiteral(x.getName()).getDouble(), //
                    soln.getLiteral(y.getName()).getDouble())) //
                    .setCost(soln.getLiteral(cost.getName()).getDouble()) //
                    .setDistance(soln.getLiteral(distance.getName()).getDouble()).setGeometry(geom).build(ctxt));
            return true;
        };

        exec(sb, processor);
        return candidates;
    }

    @Override
    public void recordSolution(Solution solution, double buffer) {
        solution.simplify((x, y) -> this.isClearPath(x, y, buffer));
        addPath(Namespace.BaseModel, solution.stream().map(c -> new MapCoordinate(c)));
    }

    @Override
    public boolean areEquivalent(Coordinate a, Coordinate b) {
        return new MapCoordinate(a).equals2D(new MapCoordinate(b), ctxt.scaleInfo.getResolution());
    }

    Model getModel() {
        return data.getUnionModel();
    }

    public void doClustering(Resource type, double eps, int minCount) {
        // new Clusterer(type,eps,minCount).run();
    }

    @Override
    public void updateIsIndirect(Coordinate target, double buffer, Set<Obstacle> newObstacles) {
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

        // List<Point> newObst = newObstacles.stream().map(c ->
        // ctxt.geometryUtils.asPoint(c)).collect(Collectors.toList());

        List<Literal> updateCoords = new ArrayList<>();

        for (Coordinate c : candidates) {
            LineString ls = ctxt.geometryUtils.asLine(c, target);
            for (Obstacle obst : newObstacles) {
                if (ls.distance(obst.geom()) < buffer) {
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
                            .addWhere(Namespace.s, Namespace.wkt, wkt)
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

    class MapCoordinate implements FrontsCoordinate {

        UnmodifiableCoordinate coord;

        public MapCoordinate(Coordinate coordinate) {
            coord = UnmodifiableCoordinate.make(adopt(coordinate));
        }

        protected MapCoordinate(FrontsCoordinate coordinate) {
            this(coordinate.getCoordinate());
        }

        @Override
        public UnmodifiableCoordinate getCoordinate() {
            return coord;
        }

    }

    private class SPARQL {

        static Expr indirectCalc(Var distance, Var indirect) {
            ExprFactory exprF = new ExprFactory(getPrefixes());
            return exprF.add(exprF.cond(exprF.bound(indirect), exprF.asExpr(distance), exprF.asExpr(0)), distance);

        }
    }

    private class ObstacleImpl implements Obstacle {
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

        ObstacleImpl(Position startPostition, Location relativeLocation) {
            Position absoluteObstacle = startPostition.nextPosition(relativeLocation);
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

        Resource in(Model m) {
            Resource result = m.createResource(rdf().getURI(), Namespace.Obst);
            result.addLiteral(Namespace.wkt, wkt());
            return result;
        }

        @Override
        public int hashCode() {
            return wkt.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if ((obj == null) || (getClass() != obj.getClass()))
                return false;
            ObstacleImpl other = (ObstacleImpl) obj;
            return Objects.equals(wkt, other.wkt);
        }
        
        @Override
        public String toString() {
            return wkt.getLexicalForm();
        }
    }

    private class ObstacleHandler {

        private Geometry[] merge(Obstacle obstacle, Collection<? extends Obstacle> others) {
            LineMerger merger = new LineMerger();
            merger.add(obstacle.geom());
            others.stream().map(Obstacle::geom).forEach(merger::add);

            @SuppressWarnings("unchecked")
            Collection<Geometry> collection = merger.getMergedLineStrings();
            return collection.toArray(new Geometry[collection.size()]);
        }

        private Geometry makeCloud(Obstacle obstacle, Collection<? extends Obstacle> others) {
            Set<Coordinate> cSet = new HashSet<>();
            Consumer<Obstacle> co = o -> Arrays.stream(o.geom().getCoordinates()).forEach(cSet::add);
            co.accept(obstacle);
            others.forEach(co);
            return ctxt.geometryFactory.createLineString( cSet.toArray(new Coordinate[cSet.size()]));
            
           // List<Point> pList = cSet.stream().map(ctxt.geometryFactory::createPoint).collect(Collectors.toList());
            //return ctxt.geometryFactory.buildGeometry(pList).union();
        }

        private Set<ObstacleImpl> mergeIntersectOrTouch(Obstacle obstacle) {
            Var otherWkt = Var.alloc("otherWkt");
            SelectBuilder sb = new SelectBuilder().setDistinct(true).addVar(Namespace.s).addVar(otherWkt) //
                    .from(Namespace.UnionModel.getURI()) //
                    .addWhere(Namespace.s, Namespace.wkt, otherWkt) //
                    .addWhere(Namespace.s, RDF.type, Namespace.Obst)
                    .addFilter(ctxt.graphGeomFactory.isNearby(exprF, obstacle.wkt(), otherWkt,
                            ctxt.scaleInfo.getResolution()));

            Set<ObstacleImpl> solns = new HashSet<>();

            Predicate<QuerySolution> processor = soln -> {
                solns.add(
                        new ObstacleImpl(soln.getResource(Namespace.s.getName()), soln.getLiteral(otherWkt.getName())));
                return true;
            };
            
            exec(sb, processor);

            Set<ObstacleImpl> solution = new HashSet<>();

            if (solns.isEmpty()) {
                ObstacleImpl obstImpl = (ObstacleImpl) obstacle;
                Resource r = obstImpl.in(ModelFactory.createDefaultModel());
                doUpdate(new UpdateBuilder().addInsert(Namespace.PlanningModel, r.getModel()));
                solution.add(obstImpl);
            } else {
                solns.remove(obstacle);
                if (!solns.isEmpty()) {
                    Geometry result = makeCloud(obstacle, solns);
                    UpdateRequest req = new UpdateRequest();
                    for (Obstacle obst : solns) {
                        req.add(new UpdateBuilder()
                                .addDelete(Namespace.PlanningModel, obst.rdf(), Namespace.p, Namespace.o)
                                .addGraph(Namespace.UnionModel,
                                        new WhereBuilder().addWhere(obst.rdf(), Namespace.p, Namespace.o))
                                .build());
                    }

                    Model merged = ModelFactory.createDefaultModel();
//                    try {
//                    ObstacleImpl obst = (result instanceof MultiPoint) ?
//                            new ObstacleImpl( ConcaveHull.concaveHullByLength(result,
//                                     ctxt.scaleInfo.getResolution())) :
//                                         new ObstacleImpl(result);
//                        obst.in(merged);
//                        solution.add(obst);
//                    req.add(new UpdateBuilder().addInsert(Namespace.PlanningModel, merged).build());
//                    } catch (NullPointerException e) {
//                        System.err.println( "OUCH" );
//                        e.printStackTrace();
//                        ConcaveHull.concaveHullByLength(result,
//                                ctxt.scaleInfo.getResolution());
//                    }
                    ObstacleImpl obst = new ObstacleImpl(result);
                      obst.in(merged);
                      solution.add(obst);
                    req.add(new UpdateBuilder().addInsert(Namespace.PlanningModel, merged).build());
                    doUpdate(req);
                }
            }
            return solution;
        }

        Set<? extends Obstacle> addObstacle(Obstacle obst) {
            // find all Obstacles that this obstacle will intersect or touch
            // if there are any, merge them together.
            // if not just write this on to the graph.
            Set<ObstacleImpl> work = mergeIntersectOrTouch(obst);
            // delete any Coords that are within buffer of any of the work geometries.
            Var wkt = Var.alloc("wkt");
            Var obstRes = Var.alloc("obst");
            Var otherWkt = Var.alloc("otherWkt");
            UpdateBuilder update = new UpdateBuilder()
                    .addDelete(Namespace.PlanningModel, Namespace.s, Namespace.p, Namespace.o)
                    .addGraph(Namespace.UnionModel, new WhereBuilder() //
                            .addWhere(Namespace.s, Namespace.p, Namespace.o) //
                            .addWhere(Namespace.s, RDF.type, Namespace.Coord) //
                            .addWhere(Namespace.s, Namespace.wkt, wkt) //
                            .addWhere(obstRes, Namespace.wkt, otherWkt)
                            .addFilter(exprF.lt(ctxt.graphGeomFactory.calcDistance(exprF, wkt, otherWkt), buffer))
                            .addFilter(exprF.in(exprF.asExpr(obstRes), exprF
                                    .asList(work.stream().map(Obstacle::rdf).collect(Collectors.toList()).toArray()))));
            doUpdate(update);
            return work;
        }

        boolean isObstacle(Coordinate point) {
            Literal pointWKT = ctxt.graphGeomFactory.asWKT(point);
            Var wkt = Var.alloc("wkt");
            AskBuilder ask = new AskBuilder().addGraph(Namespace.UnionModel,
                    new WhereBuilder().addWhere(Namespace.s, RDF.type, Namespace.Obst) //
                            .addWhere(Namespace.s, Namespace.wkt, wkt) //
                            .addFilter(ctxt.graphGeomFactory.intersects(exprF, pointWKT, wkt)));
            return ask(ask);
        }

        Set<Obstacle> getObstacles() {
            Var wkt = Var.alloc("wkt");

            SelectBuilder sb = new SelectBuilder().addVar(Namespace.s).addVar(wkt) //
                    .addGraph(Namespace.UnionModel, new WhereBuilder().addWhere(Namespace.s, RDF.type, Namespace.Obst) //
                            .addWhere(Namespace.s, Namespace.wkt, wkt));

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
