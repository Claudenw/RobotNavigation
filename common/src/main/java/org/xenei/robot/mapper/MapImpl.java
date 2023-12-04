package org.xenei.robot.mapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Function;

import org.apache.jena.arq.querybuilder.AbstractQueryBuilder;
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
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
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
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateRequest;
import org.apache.jena.vocabulary.RDF;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.mapping.Map;
import org.xenei.robot.common.planning.Solution;
import org.xenei.robot.common.planning.Step;
import org.xenei.robot.common.utils.CoordUtils;
import org.xenei.robot.mapper.rdf.Namespace;

public class MapImpl implements Map {
    private static final Logger LOG = LoggerFactory.getLogger(MapImpl.class);

    private double scale;

    public static double M_SCALE = 1.0;
    public static double CM_SCALE = 1.0 / 100;

    Dataset data;
    private ExprFactory exprF;

    public MapImpl(double scale) {
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
    public double getScale() {
        return scale;
    }

    private static Model defaultModel() {
        return ModelFactory.createDefaultModel().setNsPrefixes(GeoSPARQL_URI.getPrefixes())
                .setNsPrefixes(PrefixMapping.Standard).setNsPrefix("robut", "urn:org.xenei.robot:");
    }

    public boolean isEmpty() {
        return data.getUnionModel().isEmpty();
    }

    private void doUpdate(UpdateBuilder update) {
        UpdateExecutionFactory.create(update.build(), data).execute();
    }

    private void doUpdate(UpdateRequest request) {
        UpdateExecutionFactory.create(request, data).execute();
    }

    QueryExecution doQuery(AbstractQueryBuilder<?> qb) {
        return QueryExecutionFactory.create(qb.build(), data);
    }

    @Override
    public void addTarget(Step target) {
        // TODO this should calculate distance to real target and add adjustment for
        // no-clear
        // path.
        Resource qA = GraphModFactory.asRDF(target, Namespace.Coord, GraphModFactory.asPoint(target.getCoordinate()));
        data.getNamedModel(Namespace.PlanningModel).removeAll(qA, Namespace.distance, null);
        UpdateBuilder ub = new UpdateBuilder().addInsert(Namespace.PlanningModel, qA.getModel())
                .addInsert(Namespace.PlanningModel, qA, Namespace.distance, target.cost());
        doUpdate(ub);
        LOG.debug("Added {}", target);
    }

    @Override
    public void addObstacle(Coordinate point) {
        // TODO update adjustments for all points that do not have adjustments but for
        // which this obstacle
        // blocks the target.
        Polygon obstacle = GraphModFactory.asPolygon(point, scale);
        Literal obstacleWkt = GraphModFactory.asWKT(obstacle);
        Resource r = GraphModFactory.asRDF(point, Namespace.Obst, obstacle);
        Expr notClause = exprF.not(exprF.exists(new WhereBuilder().addWhere(Namespace.s, RDF.type, Namespace.Obst)));
        UpdateRequest req = new UpdateRequest()
                // delete any points in the planning model that are covered by the obstacle
                .add(new UpdateBuilder().addPrefixes(data.getPrefixMapping())
                        .addDelete(Namespace.PlanningModel, Namespace.s, Namespace.p, Namespace.o)
                        .addWhere(Namespace.s, RDF.type, Namespace.Point)
                        .addWhere(Namespace.s, Namespace.p, Namespace.o)
                        .addFilter(GraphModFactory.checkCollision(exprF, Namespace.s, obstacleWkt)).addFilter(notClause)
                        .build())
                .add(new UpdateBuilder().addInsert(Namespace.PlanningModel, r.getModel()).build());
        doUpdate(req);
    }

    @Override
    public boolean isObstacle(Coordinate point) {
        Literal pointWKT = GraphModFactory.asWKT(point);
        AskBuilder ask = new AskBuilder().addGraph(Namespace.UnionModel,
                new WhereBuilder().addWhere(Namespace.s, RDF.type, Namespace.Obst)
                        .addWhere(Namespace.s, Geo.AS_WKT_PROP, "?wkt")
                        .addFilter(GraphModFactory.checkCollision(exprF, "?wkt", pointWKT)));
        try (QueryExecution qexec = doQuery(ask)) {
            return qexec.execAsk();
        }
    }

    @Override
    public Set<Geometry> getObstacles() {
        Var wkt = Var.alloc("wkt");

        SelectBuilder sb = new SelectBuilder().addVar(wkt).addGraph(Namespace.UnionModel, new WhereBuilder()
                .addWhere(Namespace.s, RDF.type, Namespace.Obst).addWhere(Namespace.s, Geo.AS_WKT_PROP, wkt));

        Set<Geometry> result = new HashSet<>();
        try (QueryExecution qexec = doQuery(sb)) {
            ResultSet results = qexec.execSelect();
            while (results.hasNext()) {
                QuerySolution soln = results.nextSolution();
                Geometry g = GraphModFactory.fromWkt(soln.getLiteral(wkt.getName()));
                result.add(g);
            }
        }
        return result;
    }

    /**
     * Gets the Step for the coordinates.
     * 
     * @param location The location to get the Step for
     * @return the Step for the location.
     */
    public Optional<Step> getStep(Location location) {
        Resource a = Namespace.urlOf(location.getCoordinate());
        Var distance = Var.alloc("distance");
        Var wkt = Var.alloc("wkt");
        SelectBuilder sb = new SelectBuilder().addVar(distance).addVar(wkt).addGraph(Namespace.PlanningModel,
                new WhereBuilder().addWhere(a, Namespace.distance, distance).addWhere(a, RDF.type, Namespace.Coord)
                        .addWhere(a, Geo.AS_WKT_PROP, wkt));
        try (QueryExecution qexec = doQuery(sb)) {
            ResultSet results = qexec.execSelect();
            if (results.hasNext()) {
                QuerySolution soln = results.next();
                Geometry geom = GraphModFactory.fromWkt(soln.getLiteral(wkt.getName()));
                return Optional
                        .of(new Step(location.getCoordinate(), soln.getLiteral(distance.getName()).getDouble(), geom));
            }
            return Optional.empty();
        }
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
        doUpdate(GraphModFactory.addPath(model, points));
        LOG.debug("Path <{} {}>", points[0], points[points.length - 1]);
    }

    @Override
    public void cutPath(Coordinate a, Coordinate b) {
        cutPath(Namespace.PlanningModel, a, b);
    }

    public void cutPath(Resource model, Coordinate a, Coordinate b) {
        Resource rA = Namespace.urlOf(a);
        Resource rB = Namespace.urlOf(b);
        Node tn = NodeFactory.createTripleNode(rA.asNode(), Namespace.Path.asNode(), rB.asNode());

        doUpdate(new UpdateBuilder().addDelete(model, tn, Namespace.p, Namespace.o).addWhere(tn, Namespace.p,
                Namespace.o));
    }

    public boolean hasPath(Location a, Location b) {
        Literal aWkt = GraphModFactory.asWKT(GraphModFactory.asPolygon(a, scale));
        Literal bWkt = GraphModFactory.asWKT(GraphModFactory.asPolygon(b, scale));

        WhereBuilder wb = new WhereBuilder().addWhere(Namespace.s, RDF.type, Namespace.Path)
                .addWhere(Namespace.s, Geo.AS_WKT_PROP, Namespace.o)
                .addFilter(exprF.and(GraphModFactory.checkCollision(exprF, Namespace.o, aWkt),
                        GraphModFactory.checkCollision(exprF, Namespace.o, bWkt)));
        AskBuilder ab = new AskBuilder().addGraph(Namespace.UnionModel, wb);
        try (QueryExecution exec = doQuery(ab)) {
            return exec.execAsk();
        }
    }

    @Override
    public boolean clearView(Coordinate from, Coordinate target) {
        Literal pathWkt = GraphModFactory.asWKTString(from, target);
        Var wkt = Var.alloc("wkt");
        AskBuilder ask = new AskBuilder().from(Namespace.UnionModel.getURI())
                .addWhere(Namespace.s, RDF.type, Namespace.Obst).addWhere(Namespace.s, Geo.AS_WKT_PROP, wkt)
                .addFilter(GraphModFactory.checkCollision(exprF, pathWkt, wkt));
        try (QueryExecution qexec = doQuery(ask)) {
            return !qexec.execAsk();
        }
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
        Resource updt = Namespace.urlOf(c);
        doUpdate(new UpdateRequest()
                .add(new UpdateBuilder().addDelete(model, updt, p, Namespace.o)
                        .addGraph(model, new WhereBuilder().addWhere(updt, p, Namespace.o)).build())
                .add(new UpdateBuilder().addInsert(model, updt, p, value).build()));
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
        Resource current = Namespace.urlOf(currentCoords);
        Literal wkt = GraphModFactory.asWKT(currentCoords);
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

        SelectBuilder sb = new SelectBuilder().addVar(cost).addVar(otherWkt).from(Namespace.UnionModel.getURI())
                .addWhere(other, Namespace.distance, otherDist).addWhere(other, Geo.AS_WKT_PROP, otherWkt)
                .addFilter(exprF.ne(other, current)).addOptional(other, Namespace.adjustment, adjustment)
                .addBind(GraphModFactory.calcDistance(exprF, otherWkt, wkt), dist)
                .addBind(exprF.add(distCalc, dist), cost).addOrderBy(cost, Order.ASCENDING);

        try (QueryExecution qexec = doQuery(sb)) {
            ResultSet results = qexec.execSelect();

            while (results.hasNext()) {
                QuerySolution soln = results.nextSolution();
                Geometry geom = GraphModFactory.fromWkt(soln.getLiteral(otherWkt.getName()));
                for (Coordinate candidate : geom.getCoordinates()) {
                    if (clearView(currentCoords, candidate)) {
                        Step rec = new Step(candidate, soln.getLiteral(cost.getName()).getDouble(), geom);
                        LOG.debug("getBest() -> {}", rec);
                        return Optional.of(rec);
                    }
                }
            }
        }
        LOG.debug("No Selected map points");
        return Optional.empty();
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
        Literal targ = GraphModFactory.asWKT(target);
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
                                        .addBind(GraphModFactory.calcDistance(exprF, targ, wkt), distance))
                        .build())
                .add(new UpdateBuilder().addInsert(Namespace.PlanningModel, Namespace.s, Namespace.distance, distance)
                        .addGraph(Namespace.UnionModel,
                                new WhereBuilder().addWhere(Namespace.s, RDF.type, Namespace.Path)
                                        .addWhere(Namespace.s, Geo.AS_WKT_PROP, wkt)
                                        .addBind(GraphModFactory.calcDistance(exprF, targ, wkt), distance))
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
        try (QueryExecution qexec = doQuery(sb)) {
            ResultSet results = qexec.execSelect();
            while (results.hasNext()) {
                QuerySolution soln = results.nextSolution();

                Geometry g = GraphModFactory.fromWkt(soln.getLiteral(wkt.getName()));
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
            }
        }
        if (!insertRow.isEmpty()) {
            doUpdate(new UpdateBuilder().addInsert(Namespace.PlanningModel, insertRow));
        }
    }

    @Override
    public void setTemporaryCost(Step target) {
        update(Namespace.PlanningModel, target.getCoordinate(), Namespace.adjustment, target.cost());
    }

    /**
     * Reset the target position. Builds a new map from all known points.
     * 
     * @param target the New target.
     */
//    @Override
//    public void reset(Coordinate target) {
//        LOG.debug("reset: {}", target);
//        // create a new map from all the known points
//        // Add the target to the map.
//        Resource targetResource = GraphModFactory.asRDF(target, Namespace.Coord, asPoint(target));
//        Var distance = Var.alloc("distance");
//        Var other = Var.alloc("other");
//        data.getNamedModel(Namespace.PlanningModel).removeAll();
//        UpdateRequest req = new UpdateRequest(
//                new UpdateBuilder().addInsert(Namespace.BaseModel, targetResource.getModel()).build())
//                        .add(new UpdateBuilder().addInsert(Namespace.PlanningModel, other, Namespace.distance, distance)
//                                .addGraph(Namespace.BaseModel,
//                                        new WhereBuilder().addWhere(other, RDF.type, Namespace.Coord).addWhere(distance,
//                                                Namespace.distF, List.of(other, targetResource)))
//                                .build());
//        doUpdate(req);
//    }

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
        try (QueryExecution qexec = doQuery(sb)) {
            ResultSet results = qexec.execSelect();
            while (results.hasNext()) {
                QuerySolution soln = results.nextSolution();
                Geometry geom = GraphModFactory.fromWkt(soln.getLiteral(wkt.getName()));
                candidates.add(new Step(geom.getCoordinate(), soln.getLiteral(cost.getName()).getDouble(), geom));
            }
        }
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

//    public class DistancePF extends GenericPropertyFunction {
//
//        public DistancePF() {
//            super(new DistanceFF());
//        }
//        private class GFFDistance extends GenericFilterFunction {
//            DistanceFF ff = new DistanceFF();
//        
//      //SimmpleFeatures equals patterns differs from those stated in JTS equals, see GeoSPARQL standard page 8.
//        //This method will return true for two identical points.
//        @Override
//        protected boolean relate(GeometryWrapper sourceGeometry, GeometryWrapper targetGeometry) throws FactoryException, MismatchedDimensionException, TransformException {
//            return sourceGeometry.equalsTopo(targetGeometry);
//        }
//
//        @Override
//        public boolean isDisjoint() {
//            return false;
//        }
//
//        @Override
//        protected boolean permittedTopology(DimensionInfo sourceDimensionInfo, DimensionInfo targetDimensionInfo) {
//            return true;
//        }
//
//        @Override
//        public boolean isDisconnected() {
//            return false;
//        }
//        }
//    }
}
