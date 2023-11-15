package org.xenei.robot.planner;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

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
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.riot.Lang;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.path.Path;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateRequest;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.navigation.Coordinates;
import org.xenei.robot.navigation.Point;
import org.xenei.robot.planner.rdf.Namespace;
import org.xenei.robot.planner.rdf.PathFactory;

public class PlannerMap {
    private static final Logger LOG = LoggerFactory.getLogger(PlannerMap.class);

    private Dataset data;

    PlannerMap() {
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

    private QueryExecution doQuery(AbstractQueryBuilder<?> qb) {
        return QueryExecutionFactory.create(qb.build(), data.getUnionModel());
    }

    public PlanRecord add(Coordinates coordinates, double targetRange) {
        Resource qA = Namespace.asRDF(coordinates, Namespace.Coord);
        data.getNamedModel(Namespace.PlanningModel).removeAll(qA, Namespace.weight, null);
        UpdateBuilder ub = new UpdateBuilder().addInsert(Namespace.BaseModel, qA.getModel())
                .addInsert(Namespace.PlanningModel, qA, Namespace.weight, targetRange);
        doUpdate(ub);

        LOG.debug("Added {}", coordinates);
        return new PlanRecord(coordinates, targetRange);
    }

    public void setObstacle(Coordinates coordinates) {
        Resource r = Namespace.asRDF(coordinates, Namespace.Obst);
        UpdateRequest req = new UpdateRequest(new UpdateBuilder().addDelete(Namespace.BaseModel, r, null, null)
                .addDelete(Namespace.BaseModel, null, Namespace.path, r)
                .addDelete(Namespace.PlanningModel, r, Namespace.weight, null).build());
        req.add(new UpdateBuilder().addInsert(Namespace.BaseModel, r.getModel()).build());
        doUpdate(req);
    }

    public boolean isObstacle(Coordinates coordinates) {
        AskBuilder ask = new AskBuilder().addWhere(Namespace.urlOf(coordinates), RDF.type, Namespace.Obst);
        try (QueryExecution qexec = doQuery(ask)) {
            return qexec.execAsk();
        }
    }

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
    public Optional<PlanRecord> getPlanRecord(Coordinates coordinates) {
        coordinates = coordinates.quantize();
        Resource qA = Namespace.urlOf(coordinates);
        Var weight = Var.alloc("weight");
        SelectBuilder sb = new SelectBuilder().addVar(weight).addWhere(qA, Namespace.weight, weight).addWhere(qA,
                RDF.type, Namespace.Coord);
        try (QueryExecution qexec = doQuery(sb)) {
            ResultSet results = qexec.execSelect();
            if (results.hasNext()) {
                return Optional
                        .of(new PlanRecord(coordinates, results.next().getLiteral(weight.getName()).getDouble()));
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
    public boolean path(Coordinates a, Coordinates b) {
        Resource rA = Namespace.asRDF(a, Namespace.Coord);
        Resource rB = Namespace.asRDF(b, Namespace.Coord);

        Statement stmt = ResourceFactory.createStatement(rA, Namespace.path, rB);

        if (!data.getNamedModel(Namespace.BaseModel).contains(stmt)) {
            UpdateBuilder ub = new UpdateBuilder().addInsert(Namespace.BaseModel, rA.getModel())
                    .addInsert(Namespace.BaseModel, rB.getModel())
                    .addInsert(Namespace.BaseModel, rA, Namespace.path, rB)
                    .addInsert(Namespace.BaseModel, rB, Namespace.path, rA);

            doUpdate(ub);
            LOG.debug("Path {}", stmt);
            return true;
        }
        return false;
    }

    public void cutPath(Coordinates a, Coordinates b) {
        Resource rA = Namespace.urlOf(a);
        Resource rB = Namespace.urlOf(b);

        doUpdate(new UpdateBuilder().addDelete(Namespace.BaseModel, rA, Namespace.path, rB)
                .addDelete(Namespace.BaseModel, rB, Namespace.path, rA));
    }

    public boolean hasPath(Coordinates a, Coordinates b) {
        PathFactory pathF = new PathFactory();
        AskBuilder sb = new AskBuilder().addGraph(Namespace.BaseModel, Namespace.urlOf(a),
                pathF.oneOrMore(Namespace.path), Namespace.urlOf(b));

        try (QueryExecution exec = QueryExecutionFactory.create(sb.build(), data)) {
            return exec.execAsk();
        }
    }

    /**
     * Updates the toUpdate record to be the cost+distance(updateFom, toUpdate)
     * 
     * @param toUpdate the node to update
     * @param updateFrom the node forcing the update
     * @param cost the cost from the updating node to the target.
     */
    public void updateTargetWeight(Coordinates toUpdate, double cost) {
        LOG.debug("updatating {} weight to {}", toUpdate.getPoint().toString(1), cost);
        Resource updt = Namespace.urlOf(toUpdate);
        UpdateRequest req = new UpdateRequest(
                new UpdateBuilder().addDelete(Namespace.PlanningModel, updt, Namespace.weight, null).build());
        req.add(new UpdateBuilder().addInsert(Namespace.PlanningModel, updt, Namespace.weight, cost).build());
        doUpdate(req);
    }

    /**
     * Calculate the best next position based on the map and current coordinates.
     * 
     * @param currentCoords the current coordinates
     * @return Optional containing either either the PlanRecord for the next
     * position, or empty if none found.
     */
    public Optional<PlanRecord> getBest(Coordinates currentCoords) {
        if (data.isEmpty()) {
            LOG.debug("No map points");
            return Optional.empty();
        }
        Resource current = Namespace.urlOf(currentCoords);
        Var x = Var.alloc("x");
        Var y = Var.alloc("y");
        Var cost = Var.alloc("cost");
        Var dist = Var.alloc("dist");
        Var other = Var.alloc("other");
        Var weight = Var.alloc("weight");
        ExprFactory exprF = new ExprFactory();

        SelectBuilder sb = new SelectBuilder().addVar(x).addVar(y).addVar(cost).addVar(dist).addVar(weight)
                .addWhere(current, Namespace.path, other).addWhere(other, Namespace.x, x)
                .addWhere(other, Namespace.y, y).addWhere(other, Namespace.weight, weight)
                .addWhere(dist, Namespace.distF, List.of(current, other)).addBind(exprF.add(dist, weight), cost)
                .addOrderBy(cost, Order.ASCENDING).addFilter(exprF.ne(other, current));

        if (!LOG.isDebugEnabled()) {
            sb.setLimit(1);
        }

        try (QueryExecution qexec = doQuery(sb)) {
            ResultSet results = qexec.execSelect();
            if (results.hasNext()) {
                QuerySolution soln = results.nextSolution();
                Literal lX = soln.getLiteral(x.getName());
                Literal lY = soln.getLiteral(y.getName());
                Literal lD = soln.getLiteral(cost.getName());
                if (LOG.isDebugEnabled()) {
                    LOG.debug("getBest: {}/{} cost:{} = {}  + {}", soln.getLiteral(x.getName()).getDouble(),
                            soln.getLiteral(y.getName()).getDouble(), soln.getLiteral(cost.getName()).getDouble(),
                            soln.getLiteral(dist.getName()).getDouble(), soln.getLiteral(weight.getName()).getDouble());
                    while (results.hasNext()) {
                        soln = results.nextSolution();
                        LOG.debug("getBest: {}/{} cost:{} = {}  + {}", soln.getLiteral(x.getName()).getDouble(),
                                soln.getLiteral(y.getName()).getDouble(), soln.getLiteral(cost.getName()).getDouble(),
                                soln.getLiteral(dist.getName()).getDouble(),
                                soln.getLiteral(weight.getName()).getDouble());
                    }
                }
                PlanRecord rec = new PlanRecord(Coordinates.fromXY(lX.getDouble(), lY.getDouble()), lD.getDouble());
                LOG.debug("getBest() -> {}", rec);
                return Optional.of(rec);
            }
        }
        LOG.debug("No Selected map points");
        return Optional.empty();
    }

    /**
     * Reset the target position. Builds a new map from all known points.
     * 
     * @param target the New target.
     */
    public void reset(Coordinates target) {
        LOG.debug("reset: {}", target);
        // create a new map from all the known points
        // Add the target to the map.
        Resource targetResource = Namespace.asRDF(target, Namespace.Coord);
        Var weight = Var.alloc("weight");
        Var other = Var.alloc("other");
        UpdateRequest req = new UpdateRequest(
                new UpdateBuilder().addDelete(Namespace.PlanningModel, null, null, null).build());
        req.add(new UpdateBuilder().addInsert(Namespace.BaseModel, targetResource.getModel()).build());

        req.add(new UpdateBuilder().addInsert(Namespace.PlanningModel, other, Namespace.weight, weight)
                .addGraph(Namespace.BaseModel, new WhereBuilder().addWhere(other, RDF.type, Namespace.Coord)
                        .addWhere(weight, Namespace.distF, List.of(other, targetResource)))
                .build());
        doUpdate(req);
    }

    public Collection<PlanRecord> getPlanRecords() {
        Var x = Var.alloc("x");
        Var y = Var.alloc("y");
        Var weight = Var.alloc("weight");
        Var other = Var.alloc("other");
        SelectBuilder sb = new SelectBuilder().addVar(x).addVar(y).addVar(weight)
                .addWhere(other, RDF.type, Namespace.Coord).addWhere(other, Namespace.x, x)
                .addWhere(other, Namespace.y, y).addWhere(other, Namespace.weight, weight);

        SortedSet<PlanRecord> candidates = new TreeSet<PlanRecord>();
        try (QueryExecution qexec = doQuery(sb)) {
            ResultSet results = qexec.execSelect();
            for (; results.hasNext();) {
                QuerySolution soln = results.nextSolution();
                Literal lX = soln.getLiteral(x.getName());
                Literal lY = soln.getLiteral(y.getName());
                Literal lD = soln.getLiteral(weight.getName());
                PlanRecord pr = new PlanRecord(Coordinates.fromXY(lX.getDouble(), lY.getDouble()), lD.getDouble());
                candidates.add(pr);
            }
        }
        return candidates;
    }

    public Model getModel() {
        return data.getUnionModel();
    }

    public List<CostModelEntry> costModel() {
        Var x1 = Var.alloc("x1");
        Var y1 = Var.alloc("y1");
        Var x2 = Var.alloc("x2");
        Var y2 = Var.alloc("y2");
        Var dist = Var.alloc("dist");
        Var weight = Var.alloc("weight");
        Var node1 = Var.alloc("node1");
        Var node2 = Var.alloc("node2");
        ExprFactory exprF = new ExprFactory();
        SelectBuilder sb = new SelectBuilder().addVar(x1).addVar(y1).addVar(x2).addVar(y2).addVar(weight)
                .addWhere(node1, RDF.type, Namespace.Coord).addWhere(node1, Namespace.x, x1)
                .addWhere(node1, Namespace.y, y1).addWhere(node1, Namespace.path, node2)
                .addWhere(node2, Namespace.x, x2).addWhere(node2, Namespace.y, y2)
                .addWhere(node2, Namespace.weight, weight).addWhere(dist, Namespace.distF, List.of(node1, node2))
                .addFilter(exprF.gt(dist, 0));

        List<CostModelEntry> result = new ArrayList<>();
        try (QueryExecution qexec = doQuery(sb)) {
            ResultSet results = qexec.execSelect();
            for (; results.hasNext();) {
                QuerySolution soln = results.nextSolution();
                result.add(new CostModelEntry(
                        new Point(soln.getLiteral(x1.getName()).getDouble(), soln.getLiteral(y1.getName()).getDouble()),
                        new Point(soln.getLiteral(x2.getName()).getDouble(), soln.getLiteral(y2.getName()).getDouble()),
                        soln.getLiteral(weight.getName()).getDouble()));
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
