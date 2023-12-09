package org.xenei.robot.mapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.apache.commons.math3.ml.clustering.Cluster;
import org.apache.commons.math3.ml.clustering.Clusterable;
import org.apache.commons.math3.ml.clustering.DBSCANClusterer;
import org.apache.jena.arq.querybuilder.AskBuilder;
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
import org.xenei.robot.common.HasCoordinate;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.ScaleInfo;
import org.xenei.robot.common.mapping.Map;
import org.xenei.robot.common.planning.Solution;
import org.xenei.robot.common.planning.Step;
import org.xenei.robot.common.utils.CoordUtils;
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
    public ScaleInfo getScale() {
        return scale;
    }
    
    public static java.util.Map<String,String> getPrefixMapping() {
        java.util.Map<String,String> map = new HashMap<>();
        map.putAll(GeoSPARQL_URI.getPrefixes());
        map.putAll(PrefixMapping.Standard.getNsPrefixMap());
        map.put("robut", "urn:org.xenei.robot:");
        return map;
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

    boolean ask(AskBuilder ask) {
        try (LockHandler lh = new LockHandler(Lock.READ);
                QueryExecution exec = QueryExecutionFactory.create(ask.build(), data)) {
            return exec.execAsk();
        }
    }

    void dump(Resource modelName, Consumer<Model> consumer) {
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

    @Override
    public void addTarget(Step target) {
        Literal wkt = GraphGeomFactory.asWKT(target.getGeometry());
        Var wkt2 = Var.alloc("wkt2");
        Var s2 = Var.alloc("s2");
        ExprFactory exprF = new ExprFactory(data.getPrefixMapping());
        UpdateRequest req = new UpdateRequest();
        WhereBuilder where = new WhereBuilder().addWhere(Namespace.s, Geo.AS_WKT_PROP, wkt)
                .addWhere(s2, Geo.AS_WKT_PROP, wkt2)
                .addFilter(GraphGeomFactory.isNearby(exprF, Namespace.s, s2, scale.getTolerance()));

        AskBuilder ask = new AskBuilder().addGraph(Namespace.PlanningModel, where);
        if (ask(ask)) {
            // clear and set existing value
            req.add(new UpdateBuilder().addDelete(Namespace.PlanningModel, s2, Namespace.distance, Namespace.o)
                    .addWhere(where).build())
                    .add(new UpdateBuilder().addInsert(Namespace.PlanningModel, s2, RDF.type, Namespace.Coord)
                            .addInsert(Namespace.PlanningModel, s2, Namespace.distance, target.cost()).addWhere(where)
                            .build());
        } else {
            // no existing record
            Resource qA = GraphGeomFactory.asRDF(target, Namespace.Coord);
            qA.addLiteral(Namespace.distance, target.cost());
            req.add(new UpdateBuilder().addInsert(Namespace.PlanningModel, qA.getModel()).build());
        }

        doUpdate(req);
        LOG.debug("Added {}", target);
    }

    @Override
    public void addObstacle(Coordinate point) {
        Resource r = GraphGeomFactory.asRDF(point, Namespace.Obst);
        r.addProperty(RDF.type, Namespace.Point);
        UpdateRequest req = new UpdateRequest()
                .add(new UpdateBuilder().addInsert(Namespace.PlanningModel, r.getModel()).build());
        doUpdate(req);
    }

    @Override
    public boolean isObstacle(Coordinate point) {
        Literal pointWKT = GraphGeomFactory.asWKT(point);
        AskBuilder ask = new AskBuilder().addGraph(Namespace.UnionModel,
                new WhereBuilder().addWhere(Namespace.s, RDF.type, Namespace.Obst)
                        .addWhere(Namespace.s, Geo.AS_WKT_PROP, "?wkt")
                        .addFilter(GraphGeomFactory.isNearby(exprF, "?wkt", pointWKT, scale.getTolerance())));
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
    public Optional<Step> getStep(Location location) {
        Var distance = Var.alloc("distance");
        Var wkt = Var.alloc("wkt");
        SelectBuilder sb = new SelectBuilder().addVar(distance).addVar(wkt).addGraph(Namespace.PlanningModel,
                new WhereBuilder().addWhere(Namespace.s, Namespace.distance, distance)
                        .addWhere(Namespace.s, RDF.type, Namespace.Coord)
                        .addWhere(Namespace.s, Namespace.x, location.getX())
                        .addWhere(Namespace.s, Namespace.y, location.getY())
                        .addWhere(Namespace.s, Geo.AS_WKT_PROP, wkt));

        Step result[] = { null };

        Predicate<QuerySolution> processor = soln -> {
            Geometry geom = GraphGeomFactory.fromWkt(soln.getLiteral(wkt.getName()));
            result[0] = new Step(location.getCoordinate(), soln.getLiteral(distance.getName()).getDouble(), geom);
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
    public void addPath(Coordinate... coords) {
        path(Namespace.PlanningModel, coords);
    }

    /**
     * Add the plan record to the map
     * 
     * @param record the plan record to add
     * @return true if the record updated the map, false otherwise.
     */
    private void path(Resource model, Coordinate... points) {
        doUpdate(GraphGeomFactory.addPath(model, points));
        LOG.debug("Path <{} {}>", points[0], points[points.length - 1]);
    }

    @Override
    public void cutPath(Coordinate a, Coordinate b) {
        cutPath(Namespace.PlanningModel, a, b);
    }

    public void cutPath(Resource model, Coordinate a, Coordinate b) {
        Var ra = Var.alloc("a");
        Var rb = Var.alloc("b");

        UpdateBuilder ub = new UpdateBuilder().addDelete(model, Namespace.s, Namespace.p, Namespace.o)
                .addWhere(Namespace.s, Namespace.p, Namespace.o).addWhere(Namespace.s, Namespace.point, ra)
                .addWhere(Namespace.s, Namespace.point, rb);
        Namespace.addData(ub, ra, b);
        Namespace.addData(ub, rb, a);
        doUpdate(ub);
    }

    public boolean hasPath(Location a, Location b) {
        Literal aWkt = GraphGeomFactory.asWKT(GeometryUtils.asPolygon(a, scale.getScale()));
        Literal bWkt = GraphGeomFactory.asWKT(GeometryUtils.asPolygon(b, scale.getScale()));

        WhereBuilder wb = new WhereBuilder().addWhere(Namespace.s, RDF.type, Namespace.Path)
                .addWhere(Namespace.s, Geo.AS_WKT_PROP, Namespace.o)
                .addFilter(exprF.and(GraphGeomFactory.checkCollision(exprF, Namespace.o, aWkt, scale.getTolerance()),
                        GraphGeomFactory.checkCollision(exprF, Namespace.o, bWkt, scale.getTolerance())));
        AskBuilder ask = new AskBuilder().addGraph(Namespace.UnionModel, wb);
        return ask(ask);
    }

    @Override
    public boolean clearView(Coordinate from, Coordinate target) {
        Literal pathWkt = GraphGeomFactory.asWKTString(from, target);
        Var wkt = Var.alloc("wkt");
        AskBuilder ask = new AskBuilder().from(Namespace.UnionModel.getURI())
                .addWhere(Namespace.s, RDF.type, Namespace.Obst).addWhere(Namespace.s, Geo.AS_WKT_PROP, wkt)
                .addFilter(GraphGeomFactory.checkCollision(exprF, pathWkt, wkt, scale.getTolerance()));
        return !ask(ask);
    }

    /**
     * Updates the property of the coordinates record in the model to have the
     * specified value.
     * 
     * 
     * @param model The model to update.
     * @param c the node to update
     * @param p the property to update
     * @param value the value to set the property to.
     */
    public void update(Resource model, Coordinate c, Property p, Object value) {
        LOG.debug("updatating {} {} {} to {}", model.getLocalName(), CoordUtils.toString(c, 1), p.getLocalName(),
                value);

        doUpdate(new UpdateRequest()
                .add(new UpdateBuilder().addDelete(model, Namespace.s, p, Namespace.o)
                        .addGraph(model,
                                Namespace.addData(new WhereBuilder().addWhere(Namespace.s, p, Namespace.o), Namespace.s,
                                        c))
                        .build())
                .add(new UpdateBuilder().addInsert(model, Namespace.s, p, value)
                        .addGraph(Namespace.UnionModel, Namespace.addData(new WhereBuilder(), Namespace.s, c))
                        .build()));

    }

    /**
     * Calculate the best next position based on the map and current coordinates.
     * 
     * @param currentCoords the current coordinates
     * @return Optional containing either either the PlanRecord for the next
     * position, or empty if none found.
     */
    @Override
    public Optional<Step> getBestTarget(Coordinate currentCoords) {
        if (data.isEmpty()) {
            LOG.debug("No map points");
            return Optional.empty();
        }
        Literal wkt = GraphGeomFactory.asWKT(currentCoords);
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

        Expr distCalc = exprF.cond(exprF.bound(adjustment), exprF.add(otherDist, adjustment), exprF.asExpr(otherDist));

        // @format:off
        SelectBuilder sb = new SelectBuilder().addVar(cost).addVar(otherWkt).from(Namespace.UnionModel.getURI());
        Namespace.addData(sb, Namespace.s, currentCoords).addWhere(other, Namespace.distance, otherDist)
                .addWhere(other, RDF.type, Namespace.Coord).addWhere(other, Geo.AS_WKT_PROP, otherWkt)
                .addFilter(exprF.ne(other, Namespace.s)).addOptional(other, Namespace.adjustment, adjustment)
                .addBind(GraphGeomFactory.calcDistance(exprF, otherWkt, wkt), dist)
                .addBind(exprF.add(distCalc, dist), cost).addOrderBy(cost, Order.ASCENDING);
        // @format:on

        Step[] rec = { null };

        Predicate<QuerySolution> processor = soln -> {
            Geometry geom = GraphGeomFactory.fromWkt(soln.getLiteral(otherWkt.getName()));
            for (Coordinate candidate : geom.getCoordinates()) {
                if (clearView(currentCoords, candidate)) {
                    rec[0] = new Step(candidate, soln.getLiteral(cost.getName()).getDouble(), geom);
                    LOG.debug("getBest() -> {}", rec[0]);
                    return false;
                }
            }
            return true;
        };

        exec(sb, processor);
        if (rec[0] == null) {
            LOG.debug("No Selected map points");
            return Optional.empty();
        }
        return Optional.of(rec[0]);
    }

    /**
     * Update the planning model with new distances based on the new target
     * 
     * @param target the new target.
     */
    @Override
    public void recalculate(Coordinate target) {
        LOG.debug("recalculate: {}", target);
        Var distance = Var.alloc("distance");
        Var wkt = Var.alloc("wkt");
        Literal targ = GraphGeomFactory.asWKT(target);
        // remove all the distance and adjustments and then calculate the distance to
        // the new target
        UpdateRequest req = new UpdateRequest()
                .add(new UpdateBuilder()
                        .addDelete(Namespace.PlanningModel, Namespace.s, Namespace.distance, Namespace.o)
                        .addGraph(Namespace.PlanningModel,
                                new WhereBuilder().addWhere(Namespace.s, Namespace.distance, Namespace.o))
                        .build())
                .add(new UpdateBuilder()
                        .addDelete(Namespace.PlanningModel, Namespace.s, Namespace.adjustment, Namespace.o)
                        .addGraph(Namespace.PlanningModel,
                                new WhereBuilder().addWhere(Namespace.s, Namespace.adjustment, Namespace.o))
                        .build())
                .add(new UpdateBuilder().addInsert(Namespace.PlanningModel, Namespace.s, Namespace.distance, distance)
                        .addGraph(Namespace.UnionModel,
                                new WhereBuilder().addWhere(Namespace.s, RDF.type, Namespace.Coord)
                                        .addWhere(Namespace.s, Geo.AS_WKT_PROP, wkt)
                                        .addBind(GraphGeomFactory.calcDistance(exprF, targ, wkt), distance))
                        .build())
                .add(new UpdateBuilder().addInsert(Namespace.PlanningModel, Namespace.s, Namespace.distance, distance)
                        .addGraph(Namespace.UnionModel,
                                new WhereBuilder().addWhere(Namespace.s, RDF.type, Namespace.Path)
                                        .addWhere(Namespace.s, Geo.AS_WKT_PROP, wkt)
                                        .addBind(GraphGeomFactory.calcDistance(exprF, targ, wkt), distance))
                        .build());
        doUpdate(req);

        // check each item that has a distance and see if there is a path to the target,
        // if not then add the distance
        // to the adjustment.
        req = new UpdateRequest();
        Var candidate = Var.alloc("candidate");
        SelectBuilder sb = new SelectBuilder().from(Namespace.PlanningModel.getURI()).addVar(distance).addVar(candidate)
                .addVar(wkt).addWhere(candidate, Geo.AS_WKT_PROP, wkt)
                .addWhere(candidate, Namespace.distance, distance);

        List<Triple> insertRow = new ArrayList<>();

        Predicate<QuerySolution> processor = soln -> {
            Geometry g = GraphGeomFactory.fromWkt(soln.getLiteral(wkt.getName()));
            boolean clearView = false;
            for (Coordinate c : g.getCoordinates()) {
                if (clearView(c, target)) {
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
    }

    @Override
    public void setTemporaryCost(Step target) {
        update(Namespace.PlanningModel, target.getCoordinate(), Namespace.adjustment, target.cost());
    }

    @Override
    public Collection<Step> getTargets() {
        Var distance = Var.alloc("distance");
        Var adjustment = Var.alloc("adjustment");
        Var wkt = Var.alloc("wkt");
        Var cost = Var.alloc("cost");

        Expr costCalc = exprF.cond(exprF.bound(adjustment), exprF.add(distance, adjustment), exprF.asExpr(distance));

        SelectBuilder sb = new SelectBuilder().addVar(cost).addVar(wkt).addGraph(Namespace.PlanningModel,
                new WhereBuilder().addWhere(Namespace.s, Namespace.distance, distance)
                        .addWhere(Namespace.s, RDF.type, Namespace.Coord).addWhere(Namespace.s, Geo.AS_WKT_PROP, wkt)
                        .addOptional(Namespace.s, Namespace.adjustment, adjustment).addBind(costCalc, cost))
                .addOrderBy(cost, Order.ASCENDING);

        SortedSet<Step> candidates = new TreeSet<Step>();

        Predicate<QuerySolution> processor = soln -> {
            Geometry geom = GraphGeomFactory.fromWkt(soln.getLiteral(wkt.getName()));
            candidates.add(new Step(geom.getCoordinate(), soln.getLiteral(cost.getName()).getDouble(), geom));
            return true;
        };

        exec(sb, processor);

        return candidates;
    }

    @Override
    public void recordSolution(Solution solution) {
        solution.simplify(this::clearView);
        Coordinate[] previous = { null };
        solution.stream().forEach(c -> {
            if (previous[0] != null) {
                path(Namespace.BaseModel, previous[0], c);
            }
            previous[0] = c;
        });
    }

    Model getModel() {
        return data.getUnionModel();
    }

    public void doClustering(Resource type, double eps, int minCount) {
        // new Clusterer(type,eps,minCount).run();
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

//    private class Clusterer implements Runnable {
//        private final Resource type;
//        private final double eps;
//        private final int minCount;
//
//        Clusterer(Resource type, double eps, int minCount) {
//            this.type = type;
//            this.eps = eps;
//            this.minCount = minCount;
//        }
//
//        @Override
//        public final void run() {
//            Var x = Var.alloc("x");
//            Var y = Var.alloc("y");
//            SelectBuilder sb = new SelectBuilder().from(Namespace.PlanningModel.getURI()).addVar(x).addVar(y)
//                    .addWhere(Namespace.s, RDF.type, type).addWhere(Namespace.s, RDF.type, Namespace.Point)
//                    .addWhere(Namespace.s, Namespace.x, x).addWhere(Namespace.s, Namespace.y, y);
//
//            List<Point> points = new ArrayList<>();
//
//            Predicate<QuerySolution> processor = soln -> {
//                points.add(
//                        new Point(soln.getLiteral(x.getName()).getDouble(), soln.getLiteral(y.getName()).getDouble()));
//                return true;
//            };
//            exec(sb, processor);
//
//            DBSCANClusterer<Point> clusterer = new DBSCANClusterer<>(eps, minCount);
//            Model result = ModelFactory.createDefaultModel();
//            for (Cluster<Point> cluster : clusterer.cluster(points)) {
//                Geometry geom = GeometryUtils.asCluster(GeometryUtils.asCollection(cluster.getPoints()));
//
//                Resource r = result.createResource();
//                r.addProperty(RDF.type, type);
//                r.addProperty(RDF.type, Namespace.Cluster);
//                r.addLiteral(Geo.AS_WKT_PROP, GraphGeomFactory.asWKT(geom));
//            }
//
//            if (!result.isEmpty()) {
//                UpdateRequest req = new UpdateRequest()
//                        .add(new UpdateBuilder()
//                                .addDelete(Namespace.PlanningModel, Namespace.s, Namespace.p, Namespace.o)
//                                .addGraph(Namespace.PlanningModel,
//                                        new WhereBuilder().addWhere(Namespace.s, Namespace.p, Namespace.o)
//                                                .addWhere(Namespace.s, RDF.type, Namespace.Cluster)
//                                                .addWhere(Namespace.s, RDF.type, Namespace.Obst))
//                                .build())
//                        .add(new UpdateBuilder().addInsert(Namespace.PlanningModel, result).build());
//                doUpdate(req);
//            }
//        }
//
//        private class Point implements Clusterable, HasCoordinate {
//            Coordinate c;
//
//            Point(double x, double y) {
//                c = new Coordinate(x, y);
//            }
//
//            @Override
//            public double[] getPoint() {
//                return new double[] { getCoordinate().x, getCoordinate().y };
//            }
//
//            @Override
//            public Coordinate getCoordinate() {
//                return c;
//            }
//
//        }
//    }
    
    
}
