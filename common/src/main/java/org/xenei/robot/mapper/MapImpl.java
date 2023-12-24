package org.xenei.robot.mapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
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
import org.apache.jena.sparql.expr.aggregate.AggCount;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateRequest;
import org.apache.jena.vocabulary.RDF;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.common.AbstractFrontsCoordinate;
import org.xenei.robot.common.FrontsCoordinate;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.Mover;
import org.xenei.robot.common.ScaleInfo;
import org.xenei.robot.common.mapping.Map;
import org.xenei.robot.common.planning.Solution;
import org.xenei.robot.common.planning.Step;
import org.xenei.robot.common.utils.CoordUtils;
import org.xenei.robot.common.utils.DoubleUtils;
import org.xenei.robot.common.utils.GeometryUtils;
import org.xenei.robot.mapper.rdf.Namespace;

public class MapImpl implements Map {
    private static final Logger LOG = LoggerFactory.getLogger(MapImpl.class);

    private final ScaleInfo scale;
    private final Dataset data;
    private final ExprFactory exprF;

    public MapImpl(ScaleInfo scale) {
        this.scale = scale;
        data = DatasetFactory.create();
        data.getDefaultModel().setNsPrefixes(GeoSPARQL_URI.getPrefixes()).setNsPrefixes(PrefixMapping.Standard);
        // data.getDefaultModel().add(GraphModFactory.asRDF(new Coordinate(0,0), null,
        // null).getModel());
        data.addNamedModel(Namespace.BaseModel, defaultModel());
        data.addNamedModel(Namespace.PlanningModel, defaultModel());
        exprF = new ExprFactory(data.getPrefixMapping());

        try {
            SpatialIndex.buildSpatialIndex(data, SRS_URI.DEFAULT_WKT_CRS84);
        } catch (SpatialIndexException e) {
            throw new RuntimeException(e);
        }
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
    public ScaleInfo getScale() {
        return scale;
    }

    @Override
    public Coordinate adopt(Coordinate c) {

        double x = scale.scale(c.getX());
        double y = scale.scale(c.getY()); 
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
    public Step addCoord(Coordinate target, double distance, boolean visited, boolean isIndirect) {
        MapCoordinate mapCoord = new MapCoordinate(target);
        UpdateRequest req = new UpdateRequest();
        if (exists(mapCoord, Namespace.Coord)) {
            WhereBuilder where = new WhereBuilder().addWhere(Namespace.s, RDF.type, Namespace.Coord)
                    .addWhere(Namespace.s, Geo.AS_WKT_PROP, GraphGeomFactory.asWKT(mapCoord.getCoordinate()));
            UpdateBuilder newDat = new UpdateBuilder()
                    .addInsert(Namespace.PlanningModel, Namespace.s, Namespace.distance, distance).addWhere(where);
            if (visited) {
                newDat.addInsert(Namespace.PlanningModel, Namespace.s, Namespace.visited, visited);
            }
            if (isIndirect) {
                newDat.addInsert(Namespace.PlanningModel, Namespace.s,Namespace.isIndirect, isIndirect);
            }
            // clear and set existing value
            req.add(new UpdateBuilder().addDelete(Namespace.PlanningModel, Namespace.s, Namespace.distance, Namespace.o)
                    .addWhere(Namespace.s, Namespace.distance, Namespace.o).addWhere(where).build())
                    .add(newDat.build());
        } else {
            // no existing record
            Resource qA = GraphGeomFactory.asRDF(mapCoord, Namespace.Coord);
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
        return new StepImpl(mapCoord, distance);
    }

    private Geometry cell(Coordinate point) {
        Coordinate[] coords = { point, //
                new Coordinate(point.x+scale.getResolution(), point.y), //
                new Coordinate(point.x+scale.getResolution(), point.y+scale.getResolution()), //
                new Coordinate(point.x, point.y+scale.getResolution()), //
                point };
        return GeometryUtils.asPolygon(coords);
        
    }
    @Override
    public Coordinate addObstacle(Coordinate point) {
        MapCoordinate coord = new MapCoordinate(point);
        add(Namespace.PlanningModel, coord, Namespace.Obst, cell(coord.getCoordinate()));
        return coord.getCoordinate();
    }

    @Override
    public boolean isObstacle(Coordinate point) {
        Literal pointWKT = GraphGeomFactory.asWKT(adopt(point));
        Var wkt = Var.alloc("wkt");
        AskBuilder ask = new AskBuilder().addGraph(Namespace.UnionModel,
                new WhereBuilder().addWhere(Namespace.s, RDF.type, Namespace.Obst) //
                        .addWhere(Namespace.s, Geo.AS_WKT_PROP, wkt) //
                        .addFilter(GraphGeomFactory.isNearby(exprF, wkt, pointWKT, scale.getResolution())));
        return ask(ask);
    }

    @Override
    public Set<Geometry> getObstacles() {
        Var wkt = Var.alloc("wkt");

        SelectBuilder sb = new SelectBuilder().addVar(wkt).addGraph(Namespace.UnionModel, new WhereBuilder()
                .addWhere(Namespace.s, RDF.type, Namespace.Obst).addWhere(Namespace.s, Geo.AS_WKT_PROP, wkt));

        Set<Geometry> result = new HashSet<>();

        Predicate<QuerySolution> processor = soln -> {
            Geometry g = GraphGeomFactory.fromWkt(soln.getLiteral(wkt.getName()));
            result.add(g);
            return true;
        };

        exec(sb, processor);

        return result;
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

        SelectBuilder sb = new SelectBuilder().addVar(dist).addVar(geom) //
                .from(Namespace.PlanningModel.getURI()) //
                .addWhere(Namespace.s, Namespace.distance, dist) //
                .addWhere(Namespace.s, Namespace.x, coordinate.getX()) //
                .addWhere(Namespace.s, Namespace.y, coordinate.getY()) //
                .addWhere(Namespace.s, RDF.type, Namespace.Coord) //
                .addWhere(Namespace.s, Geo.AS_WKT_PROP, geom);

        Step result[] = { null };

        Predicate<QuerySolution> processor = soln -> {
            Geometry geometry = GraphGeomFactory.fromWkt(soln.getLiteral(geom.getName()));
            result[0] = new StepImpl(coordinate.getCoordinate(), soln.getLiteral(dist.getName()).getDouble(), geometry);
            return false;
        };

        exec(sb, processor);

        return result[0] == null ? Optional.empty() : Optional.of(result[0]);
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
        Literal path = GraphGeomFactory.asWKTString(points);
        Resource tn = ResourceFactory.createResource();
        UpdateRequest req = new UpdateRequest();
        WhereBuilder wb = new WhereBuilder();
        List<Triple> triples = new ArrayList<>();
        triples.add(Triple.create(tn.asNode(), RDF.type.asNode(), Namespace.Path.asNode()));
        triples.add(Triple.create(tn.asNode(), Geo.AS_WKT_PROP.asNode(), path.asNode()));
        int i = 0;
        for (MapCoordinate c : lst) {
            Optional<UpdateBuilder> updt = addBuilder(Namespace.PlanningModel, c, Namespace.Coord, cell(c.getCoordinate()));
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

//    /**
//     * Creates a coordinate of the type.
//     * @param coordinate the coordinate
//     * @param type the type of record.
//     */
//    private Model createType(Coordinate coordinate, Resource type, Literal wkt) {
//        Model result = ModelFactory.createDefaultModel();
//        Resource r = result.createResource(type);
//        r.addLiteral( Namespace.x, coordinate.getX());
//        r.addLiteral( Namespace.y, coordinate.getY());
//        r.addLiteral( Geo.AS_WKT_PROP, wkt);
//        return result;
//    }
//    
//    /**
//     * Creates a coordinate of the type.
//     * @param coordinate the coordinate
//     * @param type the type of record.
//     */
//    private Model createType(Coordinate coordinate, Resource type) {
//        return createType(coordinate, type, GraphGeomFactory.asWKT(coordinate));
//    }
//    
    /**
     * Finds the WKT of the nearest node to {@code coordinate} within the resolution
     * of the map, that is of type {@code type}.
     * 
     * @param coordinate
     * @param type
     * @return an optional that is either the WKT of the nearest node or empty.
     */
    private Model nearest(MapCoordinate coordinate, Resource type) {
        Literal wkt = GraphGeomFactory.asWKT(coordinate.getCoordinate());
        Var dist = Var.alloc("dist");
        Var result = Var.alloc("result");

        ConstructBuilder cb = new ConstructBuilder().addConstruct(Namespace.s, Namespace.p, Namespace.o)
                .from(Namespace.UnionModel.getURI()).addWhere(Namespace.s, Namespace.p, Namespace.o)
                .addSubQuery(new SelectBuilder().from(Namespace.UnionModel.getURI())
                        .addWhere(Namespace.s, Geo.AS_WKT_PROP, result).addWhere(Namespace.s, RDF.type, type)
                        .addBind(GraphGeomFactory.calcDistance(exprF, wkt, result), dist)
                        .addOrderBy(dist, Order.ASCENDING).setLimit(1));

        return construct(cb);
    }

    private Model get(Resource model, MapCoordinate coordinate, Resource type) {
        ConstructBuilder cb = new ConstructBuilder().addConstruct(Namespace.s, Namespace.p, Namespace.o)
                .from(model.getURI()).addWhere(Namespace.s, Namespace.p, Namespace.o)
                .addWhere(Namespace.s, Namespace.x, coordinate.getX())
                .addWhere(Namespace.s, Namespace.y, coordinate.getY()).addWhere(Namespace.s, RDF.type, type);

        return construct(cb);
    }

    private boolean exists(MapCoordinate coordinate, Resource type) {
        Literal wkt = GraphGeomFactory.asWKT(coordinate.getCoordinate());
        AskBuilder ask = new AskBuilder().from(Namespace.UnionModel.getURI()) //
                .addWhere(Namespace.s, RDF.type, type) //
                .addWhere(Namespace.s, Namespace.x, coordinate.getX()) //
                .addWhere(Namespace.s, Namespace.y, coordinate.getY());
        return ask(ask);
    }

    private Optional<UpdateBuilder> addBuilder(Resource model, MapCoordinate coordinate, Resource type, Geometry geometry) {
        return (!exists(coordinate, type)) ? Optional.of(new UpdateBuilder().addInsert(model,
                GraphGeomFactory.asRDF(coordinate.getCoordinate(), type, geometry).getModel())) : Optional.empty();
    }

    private boolean add(Resource model, MapCoordinate coordinate, Resource type, Geometry geometry) {
        Optional<UpdateBuilder> builder = addBuilder(model, coordinate, type, geometry);
        if (builder.isPresent()) {
            doUpdate(builder.get());
            return true;
        }
        return false;
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
    public boolean clearView(Coordinate from, Coordinate target, double buffer) {
        LOG.debug("checking {} to {} ", from, target);
        Literal pathWkt = GraphGeomFactory.asWKTString(from, target);
        Var wkt = Var.alloc("wkt");

        AskBuilder ask = new AskBuilder().from(Namespace.UnionModel.getURI()) //
                .addWhere(Namespace.s, RDF.type, Namespace.Obst) //
                .addWhere(Namespace.s, Geo.AS_WKT_PROP, wkt)
                .addFilter(GraphGeomFactory.checkCollision(exprF, pathWkt, wkt, buffer));
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
                    CoordUtils.toString(coordinate, scale.decimalPlaces()), type);
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
        Literal wkt = GraphGeomFactory.asWKT(mapCoords.getCoordinate());
        // cost of trip.
        Var cost = Var.alloc("cost");

        // distance current to other
        Var dist = Var.alloc("dist");

        Var other = Var.alloc("other");
        // wkt of other
        Var otherWkt = Var.alloc("otherWkt");
        // distance from other to target
        Var otherDist = Var.alloc("otherDist");
        // additional adjustment from other to target
        Var adjustment = Var.alloc("adjustment");
        Var indirect = Var.alloc("indirect");
        Var visited = Var.alloc("visited");
        Var other2 = Var.alloc("other2");
        Var other2Wkt = Var.alloc("other2Wkt");



        Expr adjCalc = exprF.cond(exprF.bound(adjustment), exprF.add(otherDist, adjustment), exprF.asExpr(otherDist));
        Expr indirectCalc = exprF.cond(exprF.bound(indirect), exprF.asExpr(otherDist), exprF.asExpr(0));
        Expr distCalc = exprF.add( adjCalc, indirectCalc);
        
//        SelectBuilder query2 = new SelectBuilder().addVar("?sX").addVar("?sY").addVar("?oX").addVar("?oY")
//                .addVar(otherDist).addVar(adjustment).addVar(dist).addVar(cost) //
//                .from(Namespace.UnionModel.getURI()) //
//                .addWhere(Namespace.s, RDF.type, Namespace.Coord) //
//                .addWhere(Namespace.s, Geo.AS_WKT_PROP, wkt) //
//                .addWhere(Namespace.s, Namespace.x, "?sX").addWhere(Namespace.s, Namespace.y, "?sY")
//                .addWhere(other, RDF.type, Namespace.Coord) //
//                .addOptional(other, Namespace.visited, visited) //
//                .addFilter(exprF.and(exprF.ne(other, Namespace.s), exprF.not(exprF.bound(visited)))) //
//                .addWhere(other, Namespace.x, "?oX").addWhere(other, Namespace.y, "?oY")
//                .addWhere(other, Namespace.distance, otherDist) //
//                .addWhere(other, Geo.AS_WKT_PROP, otherWkt) //
//                .addOptional(other, Namespace.adjustment, adjustment) //
//                .addOptional(other, Namespace.isIndirect, indirect) //
//                .addBind(GraphGeomFactory.calcDistance(exprF, otherWkt, wkt), dist) //
//                .addBind(exprF.add(distCalc, dist), cost) //
//                .addOrderBy(cost, Order.ASCENDING);
//        System.out.println(MapReports.dumpModel(this));

        SelectBuilder query = new SelectBuilder().addVar(cost).addVar(otherWkt).addVar(other) //
                .from(Namespace.UnionModel.getURI()) //
                .addWhere(Namespace.s, RDF.type, Namespace.Coord) //
                .addWhere(Namespace.s, Geo.AS_WKT_PROP, wkt) //
                .addWhere(other, RDF.type, Namespace.Coord) //
                .addOptional(other, Namespace.visited, visited) //
                .addFilter(exprF.and(exprF.ne(other, Namespace.s), exprF.not(exprF.bound(visited)))) //
                .addWhere(other, Namespace.distance, otherDist) //
                .addWhere(other, Geo.AS_WKT_PROP, otherWkt) //
                .addOptional(other, Namespace.adjustment, adjustment) //
                .addOptional(other, Namespace.isIndirect, indirect) //
                .addBind(GraphGeomFactory.calcDistance(exprF, otherWkt, wkt), dist) //
                .addBind(exprF.add(distCalc, dist), cost) //
                .addOrderBy(cost, Order.ASCENDING);

        // skip coords that are within the tolerance range of visited coords
        AskBuilder ask = new AskBuilder() //
                .addWhere(other2, Namespace.visited, "?ignore") //
                .addWhere(other2, RDF.type, Namespace.Coord)//
                .addWhere(other2, Geo.AS_WKT_PROP, other2Wkt) //
                .addFilter(exprF.le(GraphGeomFactory.calcDistance(exprF, otherWkt, other2Wkt), buffer)) //
        ;

        Step[] rec = { null };

        Predicate<QuerySolution> processor = soln -> {

            Geometry geom = GraphGeomFactory.fromWkt(soln.getLiteral(otherWkt.getName()));
            for (Coordinate candidate : geom.getCoordinates()) {
                Literal candidateWkt = GraphGeomFactory.asWKT(candidate);
                ask.setVar(otherWkt, candidateWkt);

                if (!ask(ask) && clearView(currentCoords, candidate, buffer)) {
                    rec[0] = new StepImpl(candidate, soln.getLiteral(cost.getName()).getDouble(), geom);
                    LOG.debug("getBest() -> {}", rec[0]);
                    return false;
                }
            }
            return true;
        };

        exec(query, processor);

        if (rec[0] == null) {
            LOG.debug("No Selected map points");
            return Optional.empty();
        }
        updateCoordinate(Namespace.PlanningModel, Namespace.Coord, rec[0].getCoordinate(), Namespace.visited,
                Boolean.TRUE);
        return Optional.of(rec[0]);
    }

    @Override
    public Coordinate recalculate(Coordinate target, double buffer) {
        LOG.debug("recalculate: {}", target);
        Var distance = Var.alloc("distance");
        Var wkt = Var.alloc("wkt");
        MapCoordinate result = new MapCoordinate(target);
        Literal targ = GraphGeomFactory.asWKT(result.getCoordinate());
        // remove all the distance and adjustments and then calculate the distance to
        // the new target

        UpdateRequest req = new UpdateRequest()
                .add(new UpdateBuilder() //
                        .addDelete(Namespace.PlanningModel, Namespace.s, Namespace.p, Namespace.o) //
                        .addGraph(Namespace.PlanningModel, new WhereBuilder() //
                                .addWhere(Namespace.s, Namespace.p, List.of(Namespace.adjustment, Namespace.distance)) //
                        ).build()).add(new UpdateBuilder() //
                                .addInsert(Namespace.PlanningModel, Namespace.s, Namespace.distance, distance) //
                                .addGraph(Namespace.UnionModel, new WhereBuilder() //
                                        .addWhere(Namespace.s, RDF.type, List.of(Namespace.Coord, Namespace.Path)) //
                                        .addWhere(Namespace.s, Geo.AS_WKT_PROP, wkt) //
                                        .addBind(GraphGeomFactory.calcDistance(exprF, targ, wkt), distance))
                                .build());
        doUpdate(req);

        // check each item that has a distance and see if there is a path to the target,
        // if not then add the distance
        // to the adjustment.
        req = new UpdateRequest();
        Var candidate = Var.alloc("candidate");
        SelectBuilder sb = new SelectBuilder().from(Namespace.PlanningModel.getURI()) //
                .addVar(distance).addVar(candidate).addVar(wkt) //
                .addWhere(candidate, RDF.type, List.of(Namespace.Coord, Namespace.Path)) //
                .addWhere(candidate, Geo.AS_WKT_PROP, wkt) //
                .addWhere(candidate, Namespace.distance, distance);

        List<Triple> insertRow = new ArrayList<>();

        Predicate<QuerySolution> processor = soln -> {
            Geometry g = GraphGeomFactory.fromWkt(soln.getLiteral(wkt.getName()));
            boolean clearView = false;
            for (Coordinate c : g.getCoordinates()) {
                if (clearView(c, target, buffer)) {
                    clearView = true;
                    break;
                }
            }
            if (!clearView) {
                insertRow.add(Triple.create(soln.getResource(candidate.getName()).asNode(),
                        Namespace.adjustment.asNode(), soln.getLiteral(distance.getName()).asNode()));
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
    public void setTemporaryCost(Coordinate target, double cost) {
        updateCoordinate(Namespace.PlanningModel, Namespace.Coord, target, Namespace.adjustment, cost);
    }

    @Override
    public Collection<Step> getTargets() {
        Var distance = Var.alloc("distance");
        Var adjustment = Var.alloc("adjustment");
        Var wkt = Var.alloc("wkt");
        Var cost = Var.alloc("cost");

        Expr costCalc = exprF.cond(exprF.bound(adjustment), exprF.add(distance, adjustment), exprF.asExpr(distance));

        SelectBuilder sb = new SelectBuilder().addVar(cost).addVar(wkt) //
                .addGraph(Namespace.PlanningModel, new WhereBuilder() //
                        .addWhere(Namespace.s, Namespace.distance, distance) //
                        .addWhere(Namespace.s, RDF.type, Namespace.Coord) //
                        .addWhere(Namespace.s, Geo.AS_WKT_PROP, wkt) //
                        .addOptional(Namespace.s, Namespace.adjustment, adjustment) //
                        .addBind(costCalc, cost)) //
                .addOrderBy(cost, Order.ASCENDING);

        SortedSet<Step> candidates = new TreeSet<Step>();

        Predicate<QuerySolution> processor = soln -> {
            Geometry geom = GraphGeomFactory.fromWkt(soln.getLiteral(wkt.getName()));
            candidates.add(new StepImpl(geom.getCoordinate(), soln.getLiteral(cost.getName()).getDouble(), geom));
            return true;
        };

        exec(sb, processor);
        return candidates;
    }

    @Override
    public void recordSolution(Solution solution, double buffer) {
        solution.simplify((x, y) -> this.clearView(x, y, buffer));
        addPath(Namespace.BaseModel, solution.stream().map(c -> new MapCoordinate(c)));
    }

    @Override
    public boolean areEquivalent(Coordinate a, Coordinate b) {
        return new MapCoordinate(a).equals2D(new MapCoordinate(b), Precision.EPSILON);
    }

    Model getModel() {
        return data.getUnionModel();
    }

    public void doClustering(Resource type, double eps, int minCount) {
        // new Clusterer(type,eps,minCount).run();
    }

    public void updateIsIndirect(Coordinate target, double buffer, Set<Coordinate> newObstacles) {
        Var isIndirect = Var.alloc("isIndirect");
        Var wkt = Var.alloc("wkt");
        Var x = Var.alloc("x");
        Var y = Var.alloc("y");
        
        SelectBuilder sb=new SelectBuilder().addVar(x).addVar(y).setDistinct(true) //
                .addGraph( Namespace.UnionModel, new WhereBuilder() //
                .addWhere(Namespace.s, RDF.type, Namespace.Coord) //
                .addWhere(Namespace.s, Namespace.x, x) //
                .addWhere(Namespace.s, Namespace.y, y) //
                .addOptional(Namespace.s, Namespace.isIndirect, isIndirect) //
                .addFilter(exprF.not(exprF.bound(isIndirect)))
            );
        
        List<Coordinate> candidates = new ArrayList<>();

        Predicate<QuerySolution> processor = soln -> {
            candidates.add( new Coordinate(soln.getLiteral(x.getName()).getDouble(),
                    soln.getLiteral(y.getName()).getDouble()));
            return true;
        };
        
        this.exec(sb, processor);
        
        List<Point> newObst = newObstacles.stream().map(c -> GeometryUtils.asPoint(c)).collect(Collectors.toList());
        
        List<Literal> updateCoords = new ArrayList<>();
        
        for (Coordinate c : candidates) {
            LineString ls = GeometryUtils.asLine(c,target);
            for (Point obst : newObst) {
                if (ls.distance(obst) < buffer) {
                    updateCoords.add(GraphGeomFactory.asWKT(c));
                    break;
                }
            }
        }
        if (!updateCoords.isEmpty()) {
        UpdateBuilder ub = new UpdateBuilder()
                .addInsert(Namespace.PlanningModel, Namespace.s, Namespace.isIndirect, Boolean.TRUE) //
                .addGraph( Namespace.UnionModel, new WhereBuilder() //
                    .addWhere(Namespace.s, RDF.type, Namespace.Coord) //
                    .addWhere(Namespace.s, Geo.AS_WKT_NODE, wkt)
                    .addFilter(exprF.in(wkt, updateCoords.toArray()))
                );
        doUpdate(ub);
        }
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

    private class MapCoordinate extends AbstractFrontsCoordinate {

        public MapCoordinate(Coordinate coordinate) {
            super(adopt(coordinate));
        }

        protected MapCoordinate(AbstractFrontsCoordinate coordinate) {
            this(coordinate.getCoordinate());
        }

        @Override
        public FrontsCoordinate copy() {
            return new MapCoordinate(this.getCoordinate());
        }

        @Override
        protected <T extends AbstractFrontsCoordinate> T fromCoordinate(Coordinate base) {
            return (T) new MapCoordinate(base);
        }

    }
}
