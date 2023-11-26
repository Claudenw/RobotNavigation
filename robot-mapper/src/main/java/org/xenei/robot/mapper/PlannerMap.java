package org.xenei.robot.mapper;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.jena.arq.querybuilder.AbstractQueryBuilder;
import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.arq.querybuilder.ExprFactory;
import org.apache.jena.arq.querybuilder.Order;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
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
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.riot.Lang;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.path.Path;
import org.apache.jena.sparql.path.PathFactory;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateRequest;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.common.Coordinates;
import org.xenei.robot.common.Map;
import org.xenei.robot.common.Point;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.Solution;
import org.xenei.robot.common.Target;
import org.xenei.robot.mapper.rdf.Namespace;

public class PlannerMap implements Map {
    private static final Logger LOG = LoggerFactory.getLogger(PlannerMap.class);

    Dataset data;

    public PlannerMap() {
        data = DatasetFactory.create();
        data.addNamedModel(Namespace.BaseModel, ModelFactory.createDefaultModel());
        data.addNamedModel(Namespace.PlanningModel, ModelFactory.createDefaultModel());
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
        return QueryExecutionFactory.create(qb.build(), data.getUnionModel());
    }

    @Override
    public void add(Target target) {
        Resource qA = Namespace.asRDF(target.coordinates(), Namespace.Coord);
        data.getNamedModel(Namespace.PlanningModel).removeAll(qA, Namespace.distance, null);
        UpdateBuilder ub = new UpdateBuilder().addInsert(Namespace.BaseModel, qA.getModel())
                .addInsert(Namespace.PlanningModel, qA, Namespace.distance, target.cost());
        doUpdate(ub);

        LOG.debug("Added {}", target);
    }

    @Override
    public void setObstacle(Coordinates coordinates) {
        setObstacle(coordinates.getPoint());
    }

    public void setObstacle(Point point) {
        Resource r = Namespace.asRDF(point, Namespace.Obst);
        UpdateRequest req = new UpdateRequest(
                new UpdateBuilder().addDelete(Namespace.BaseModel, r, Namespace.p, Namespace.o)
                        .addWhere(r, Namespace.p, Namespace.o).build())
                                .add(new UpdateBuilder().addDelete(Namespace.BaseModel, Namespace.s, Namespace.path, r)
                                        .addWhere(Namespace.s, Namespace.path, r).build())
                                .add(new UpdateBuilder()
                                        .addDelete(Namespace.PlanningModel, Namespace.s, Namespace.path, r)
                                        .addWhere(Namespace.s, Namespace.path, r).build())
                                .add(new UpdateBuilder()
                                        .addDelete(Namespace.PlanningModel, r, Namespace.distance, Namespace.o)
                                        .addWhere(r, Namespace.distance, Namespace.o).build())
                                .add(new UpdateBuilder().addInsert(Namespace.BaseModel, r.getModel()).build());
        doUpdate(req);
    }

    @Override
    public boolean isObstacle(Coordinates coordinates) {
        AskBuilder ask = new AskBuilder().addWhere(Namespace.urlOf(coordinates), RDF.type, Namespace.Obst);
        try (QueryExecution qexec = doQuery(ask)) {
            return qexec.execAsk();
        }
    }

    @Override
    public Set<Coordinates> getObstacles() {
        Var node = Var.alloc("node");
        Var x = Var.alloc("x");
        Var y = Var.alloc("y");
        SelectBuilder sb = new SelectBuilder().addVar(x).addVar(y).addWhere(node, RDF.type, Namespace.Obst)
                .addWhere(node, Namespace.x, x).addWhere(node, Namespace.y, y);

        Set<Coordinates> result = new HashSet<>();
        try (QueryExecution qexec = doQuery(sb)) {
            ResultSet results = qexec.execSelect();
            for (; results.hasNext();) {
                QuerySolution soln = results.nextSolution();
                result.add(Coordinates.fromXY(
                        new Point(soln.getLiteral(x.getName()).getDouble(), soln.getLiteral(y.getName()).getDouble())));
            }
        }
        return result;
    }

    /**
     * Gets the plan record for the coordinates.
     * 
     * @param coordinates
     * @return
     */
    public Optional<Target> getTarget(Coordinates coordinates) {
        coordinates = coordinates.quantize();
        Resource qA = Namespace.urlOf(coordinates);
        Var distance = Var.alloc("distance");
        SelectBuilder sb = new SelectBuilder().addVar(distance).addWhere(qA, Namespace.distance, distance).addWhere(qA,
                RDF.type, Namespace.Coord);
        try (QueryExecution qexec = doQuery(sb)) {
            ResultSet results = qexec.execSelect();
            if (results.hasNext()) {
                return Optional.of(new Target(coordinates, results.next().getLiteral(distance.getName()).getDouble()));
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
    public boolean path(Coordinates a, Coordinates b) {
        return path(Namespace.PlanningModel, a, b);
    }

    /**
     * Add the plan record to the map
     * 
     * @param record the plan record to add
     * @return true if the record updated the map, false otherwise.
     */
    public boolean path(Resource model, Coordinates a, Coordinates b) {
        Resource rA = Namespace.asRDF(a, Namespace.Coord);
        Resource rB = Namespace.asRDF(b, Namespace.Coord);

        Statement stmt = ResourceFactory.createStatement(rA, Namespace.path, rB);

        if (!data.getNamedModel(model).contains(stmt)) {
            UpdateBuilder ub = new UpdateBuilder().addInsert(model, rA, Namespace.path, rB).addInsert(model, rB,
                    Namespace.path, rA);
            if (model.equals(Namespace.BaseModel)) {
                ub.addInsert(Namespace.BaseModel, rB.getModel()).addInsert(Namespace.BaseModel, rA.getModel());
            }
            doUpdate(ub);
            LOG.debug("Path {}", stmt);
            return true;
        }
        return false;
    }

    @Override
    public void cutPath(Coordinates a, Coordinates b) {
        cutPath(Namespace.PlanningModel, a, b);
    }

    public void cutPath(Resource model, Coordinates a, Coordinates b) {
        Resource rA = Namespace.urlOf(a);
        Resource rB = Namespace.urlOf(b);

        doUpdate(new UpdateBuilder().addDelete(model, rA, Namespace.path, rB).addDelete(model, rB, Namespace.path, rA));
    }

    public boolean hasPath(Coordinates a, Coordinates b) {
        AskBuilder sb = new AskBuilder();
        Path p = PathFactory.pathOneOrMoreN(PathFactory.pathLink(sb.makeNode(Namespace.path)));
        sb.addGraph(Namespace.UnionModel, Namespace.urlOf(a), p, Namespace.urlOf(b));
        try (QueryExecution exec = QueryExecutionFactory.create(sb.build(), data)) {
            return exec.execAsk();
        }
    }

    @Override
    public boolean clearView(Coordinates from, Coordinates target) {
        Position position = new Position(from, from.angleTo(target));
        return getObstacles().stream().filter(obstacle -> !position.hasClearView(target, obstacle)).findFirst()
                .isEmpty();
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
    public void update(Resource model, Coordinates c, Property p, Object value) {
        LOG.debug("updatating {} {} {} to {}", model.getLocalName(), c.getPoint().toString(1), p.getLocalName(), value);
        Resource updt = Namespace.urlOf(c);
        data.getNamedModel(model).removeAll(updt, p, null);
        doUpdate(new UpdateBuilder().addInsert(model, updt, p, value));
    }

    /**
     * Calculate the best next position based on the map and current coordinates.
     * 
     * @param currentCoords the current coordinates
     * @return Optional containing either either the PlanRecord for the next
     * position, or empty if none found.
     */
    @Override
    public Optional<Target> getBestTarget(Coordinates currentCoords) {
        if (data.isEmpty()) {
            LOG.debug("No map points");
            return Optional.empty();
        }
        Function<Literal, Double> nullIsZero = (l) -> l == null ? 0.0 : l.getDouble();
        Resource current = Namespace.urlOf(currentCoords);
        Var x = Var.alloc("x");
        Var y = Var.alloc("y");
        Var cost = Var.alloc("cost");
        // distance from other to target
        Var dist = Var.alloc("dist");
        // additional adjustment from other to target
        Var adjustment = Var.alloc("adjustment");
        // distance curent to other
        Var codist = Var.alloc("codist");
        Var other = Var.alloc("other");
        ExprFactory exprF = new ExprFactory();

        Expr distCalc = exprF.cond(exprF.bound(adjustment), exprF.add(dist, adjustment), exprF.asExpr(dist));
        SelectBuilder sb = new SelectBuilder().addVar(x).addVar(y).addVar(cost).addVar(dist).addVar(adjustment)
                .addVar(codist).addWhere(current, Namespace.path, other).addWhere(other, Namespace.x, x)
                .addWhere(other, Namespace.y, y).addWhere(other, Namespace.distance, dist)
                .addOptional(other, Namespace.adjustment, adjustment)
                .addWhere(codist, Namespace.distF, List.of(current, other)).addBind(exprF.add(distCalc, codist), cost)
                .addOrderBy(cost, Order.ASCENDING).addFilter(exprF.ne(other, current));

//        if (!LOG.isDebugEnabled()) {
//            sb.setLimit(1);
//        }

        Optional<Target> result = Optional.empty();
        try (QueryExecution qexec = doQuery(sb)) {
            ResultSet results = qexec.execSelect();
            Consumer<QuerySolution> log = (sln) -> LOG.debug("getBest({}): {}/{} cost:{} = {}  + {} + {}",
                    currentCoords, sln.getLiteral(x.getName()).getDouble(), sln.getLiteral(y.getName()).getDouble(),
                    sln.getLiteral(cost.getName()).getDouble(), sln.getLiteral(dist.getName()).getDouble(),
                    sln.getLiteral(codist.getName()).getDouble(),
                    nullIsZero.apply(sln.getLiteral(adjustment.getName())));

            while (results.hasNext()) {
                QuerySolution soln = results.nextSolution();
                Literal lX = soln.getLiteral(x.getName());
                Literal lY = soln.getLiteral(y.getName());
                Literal lD = soln.getLiteral(cost.getName());
                if (LOG.isDebugEnabled()) {
                    log.accept(soln);
                }
                if (result.isEmpty()) {
                    Coordinates candidate = Coordinates.fromXY(lX.getDouble(), lY.getDouble());
                    if (clearView(currentCoords, candidate)) {
                        Target rec = new Target(candidate, lD.getDouble());
                        LOG.debug("getBest() -> {}", rec);
                        result = Optional.of(rec);
                        if (!LOG.isDebugEnabled()) {
                            return result;
                        }
                    }
                }
            }
        }
        if (result.isEmpty()) {
            LOG.debug("No Selected map points");
        }
        return result;
    }

    /**
     * Update the planning model with new distances based on the new target
     * 
     * @param target the new target.
     */
    @Override
    public void recalculate(Coordinates target) {
        LOG.debug("recalculate: {}", target);
        Resource targetResource = Namespace.urlOf(target);
        Var distance = Var.alloc("distance");
        Var other = Var.alloc("other");
        data.getNamedModel(Namespace.PlanningModel).removeAll(null, Namespace.distance, null);
        doUpdate(new UpdateBuilder().addInsert(Namespace.PlanningModel, other, Namespace.distance, distance)
                .addGraph(Namespace.BaseModel, new WhereBuilder().addWhere(other, RDF.type, Namespace.Coord)
                        .addWhere(distance, Namespace.distF, List.of(other, targetResource))));
    }

    @Override
    public void setTemporaryCost(Target target) {
        update(Namespace.PlanningModel, target.coordinates(), Namespace.adjustment, target.cost());
    }

    /**
     * Reset the target position. Builds a new map from all known points.
     * 
     * @param target the New target.
     */
    @Override
    public void reset(Coordinates target) {
        LOG.debug("reset: {}", target);
        // create a new map from all the known points
        // Add the target to the map.
        Resource targetResource = Namespace.asRDF(target, Namespace.Coord);
        Var distance = Var.alloc("distance");
        Var other = Var.alloc("other");
        data.getNamedModel(Namespace.PlanningModel).removeAll();
        UpdateRequest req = new UpdateRequest(
                new UpdateBuilder().addInsert(Namespace.BaseModel, targetResource.getModel()).build())
                        .add(new UpdateBuilder().addInsert(Namespace.PlanningModel, other, Namespace.distance, distance)
                                .addGraph(Namespace.BaseModel,
                                        new WhereBuilder().addWhere(other, RDF.type, Namespace.Coord).addWhere(distance,
                                                Namespace.distF, List.of(other, targetResource)))
                                .build());
        doUpdate(req);
    }

    @Override
    public Collection<Target> getTargets() {
        Var x = Var.alloc("x");
        Var y = Var.alloc("y");
        Var distance = Var.alloc("distance");
        Var other = Var.alloc("other");
        Var cost = Var.alloc("cost");
        Var adjustment = Var.alloc("adjustment");
        ExprFactory exprF = new ExprFactory();
        SelectBuilder sb = new SelectBuilder().addVar(x).addVar(y).addVar(distance).addWhere(other, Namespace.x, x)
                .addWhere(other, Namespace.y, y).addWhere(other, Namespace.distance, distance)
                .addOptional(other, Namespace.adjustment, adjustment).addBind(exprF.add(distance, adjustment), cost)
                .addOrderBy(cost, Order.ASCENDING);

        SortedSet<Target> candidates = new TreeSet<Target>();
        try (QueryExecution qexec = doQuery(sb)) {
            ResultSet results = qexec.execSelect();
            for (; results.hasNext();) {
                QuerySolution soln = results.nextSolution();
                Literal lX = soln.getLiteral(x.getName());
                Literal lY = soln.getLiteral(y.getName());
                Literal lD = soln.getLiteral(distance.getName());
                Target pr = new Target(Coordinates.fromXY(lX.getDouble(), lY.getDouble()), lD.getDouble());
                candidates.add(pr);
            }
        }
        return candidates;
    }

    @Override
    public void recordSolution(Solution solution) {
        solution.simplify(this::clearView);
        Coordinates[] previous = { null };
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

    public List<CostModelEntry> costModel() {
        Var x1 = Var.alloc("x1");
        Var y1 = Var.alloc("y1");
        Var x2 = Var.alloc("x2");
        Var y2 = Var.alloc("y2");
        Var cost = Var.alloc("cost");
        Var distance = Var.alloc("distance");
        Var node1 = Var.alloc("node1");
        Var node2 = Var.alloc("node2");
        ExprFactory exprF = new ExprFactory();
        SelectBuilder sb = new SelectBuilder().addVar(x1).addVar(y1).addVar(x2).addVar(y2).addVar(distance)
                .addWhere(node1, RDF.type, Namespace.Coord).addWhere(node1, Namespace.x, x1)
                .addWhere(node1, Namespace.y, y1).addWhere(node1, Namespace.path, node2)
                .addWhere(node2, Namespace.x, x2).addWhere(node2, Namespace.y, y2)
                .addWhere(node2, Namespace.distance, distance).addWhere(cost, Namespace.distF, List.of(node1, node2))
                .addFilter(exprF.gt(cost, 0));

        List<CostModelEntry> result = new ArrayList<>();
        try (QueryExecution qexec = doQuery(sb)) {
            ResultSet results = qexec.execSelect();
            for (; results.hasNext();) {
                QuerySolution soln = results.nextSolution();
                result.add(new CostModelEntry(
                        new Point(soln.getLiteral(x1.getName()).getDouble(), soln.getLiteral(y1.getName()).getDouble()),
                        new Point(soln.getLiteral(x2.getName()).getDouble(), soln.getLiteral(y2.getName()).getDouble()),
                        soln.getLiteral(distance.getName()).getDouble()));
            }
        }
        return result;
    }

    public String dumpModel() {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        data.getUnionModel().write(bos, Lang.TURTLE.getName());
        return bos.toString();
    }

    public String dumpPlanningModel() {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        data.getNamedModel(Namespace.PlanningModel).write(bos, Lang.TURTLE.getName());
        return bos.toString();
    }

    public String dumpBaseModel() {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        data.getNamedModel(Namespace.BaseModel).write(bos, Lang.TURTLE.getName());
        return bos.toString();
    }
}
