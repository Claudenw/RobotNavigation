package org.xenei.robot.planner;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.arq.querybuilder.ExprFactory;
import org.apache.jena.arq.querybuilder.Order;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.atlas.logging.Log;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.ARQ;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.QueryExecException;
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
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.ExecutionContext;
import org.apache.jena.sparql.engine.QueryIterator;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.expr.nodevalue.NodeFunctions;
import org.apache.jena.sparql.function.FunctionBase2;
import org.apache.jena.sparql.function.FunctionRegistry;
import org.apache.jena.sparql.pfunction.PFuncAssignToSubject;
import org.apache.jena.sparql.pfunction.PropFuncArg;
import org.apache.jena.sparql.pfunction.PropFuncArgType;
import org.apache.jena.sparql.pfunction.PropertyFunction;
import org.apache.jena.sparql.pfunction.PropertyFunctionEval;
import org.apache.jena.sparql.pfunction.PropertyFunctionFactory;
import org.apache.jena.sparql.pfunction.PropertyFunctionRegistry;
import org.apache.jena.sparql.util.IterLib;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.navigation.Coordinates;
import org.xenei.robot.utils.CoordinateGraph;


public class PlannerMap {
    private static final Logger LOG = LoggerFactory.getLogger(PlannerMap.class);
    private Model points;
    private Model complete;
    
    private static class Namespace {
        public static final String URI = "urn:org.xenei.robot:";
        public static final Resource Coord = ResourceFactory.createResource(URI+"Coord");
        public static final Property x = ResourceFactory.createProperty(URI+"x");
        public static final Property y = ResourceFactory.createProperty(URI+"y");
        public static final Property path =  ResourceFactory.createProperty(URI+"path");
        public static final Property targetRange = ResourceFactory.createProperty(URI+"targetRange");
        public static final Property distF = ResourceFactory.createProperty(URI+"fn:dist");
        
        static {
        final PropertyFunctionRegistry reg = PropertyFunctionRegistry.chooseRegistry(ARQ.getContext());
        reg.put(URI+"fn#dist", Dist.class);
        PropertyFunctionRegistry.set(ARQ.getContext(), reg);
        }
    }
    
    PlannerMap() {
        points = ModelFactory.createDefaultModel();
        complete = ModelFactory.createDefaultModel();
    }

    public boolean isEmpty() {
        return points.isEmpty();
    }

    private Resource asRDF(Coordinates a) {
        Coordinates qA = a.quantize();
        Model result = ModelFactory.createDefaultModel();
        String uri = String.format("uri:org.xenei.robot.coord/%s/%s", qA.getX(), qA.getY());
        Resource r = result.createResource(uri, Namespace.Coord);
        r.addLiteral( Namespace.x, qA.getX());
        r.addLiteral( Namespace.y, qA.getX());
        return r;
    }
    
    public PlanRecord add(Coordinates coordinates, double targetRange) {
        Resource qA = asRDF(coordinates);
        complete.add(qA.getModel());
        qA.addLiteral( Namespace.targetRange, targetRange);
        points.removeAll( qA, Namespace.targetRange, null);
        points.add(qA.getModel());
        return new PlanRecord( coordinates, targetRange);
    }
    
    /**
     * Add the plan record to the map
     * @param record the plan record to add
     * @return true if the record updated the map, false otherwise.
     */
    public boolean path(Coordinates a, Coordinates b) {
        Resource rA = asRDF(a);
        Resource rB = asRDF(b);

        Statement stmt = complete.createStatement(rA, Namespace.path, rB);
        if (!points.contains(stmt))
        {
            points.add(rA.getModel());
            points.add(rB.getModel());
            points.add(stmt);
            complete.add(rA.getModel());
            complete.add(rB.getModel());
            complete.add(stmt);
            LOG.debug("Added {}", stmt);
            return true;
        }
        return false;
    }
    
    /**
     * Remove the position from the map.
     * @param coord the coordinated to remove from the map.
     */
    public void remove(Coordinates coord) {
        Resource r = asRDF(coord);
        if (points.contains(r, RDF.type, Namespace.Coord)) {
            points.removeAll( r, null, null );
            points.removeAll(null, null, r);
            LOG.debug("Removed: {}", coord);
        }
        complete.removeAll( r, null, null );
        complete.removeAll(null, null, r);
    }

    /**
     * Calculate the best next position based on the map and current coordinates.
     * @param currentCoords the current coordinates
     * @return Optional containing either either the PlanRecord for the next position, or empty if none found.
     */
    public Optional<PlanRecord> getBest(Coordinates currentCoords) {
        if (points.isEmpty()) {
            LOG.debug("No map points");
            return Optional.empty();
        }
        Resource current = asRDF(currentCoords);
        Var x = Var.alloc("x");
        Var y = Var.alloc("y");
        Var cost = Var.alloc("cost");
        Var dist = Var.alloc("dist");
        Var delta = Var.alloc("delta");
        Var other = Var.alloc("other");
        ExprFactory exprF = new ExprFactory();
        SelectBuilder sb = new SelectBuilder()
                .addVar(x).addVar(y).addBind( exprF.add(dist, delta), cost)
                .addWhere(current, Namespace.path, other)
                .addWhere(delta, Namespace.distF, exprF.asList( current, other ))
                .addWhere(other, Namespace.x, x)
                .addWhere(other, Namespace.y, y)
                .addWhere(other, Namespace.targetRange, dist)
                .addOrderBy(cost, Order.ASCENDING)
                .setLimit(1);

        
        SortedSet<PlanRecord> candidates = new TreeSet<PlanRecord>();

        try (QueryExecution qexec = QueryExecutionFactory.create(sb.build(), points)) {
          ResultSet results = qexec.execSelect() ;
          if (results.hasNext())
          {
            QuerySolution soln = results.nextSolution() ;
            Literal lX = soln.getLiteral(x.getName()) ;
            Literal lY = soln.getLiteral(y.getName()) ; 
            Literal lD = soln.getLiteral(cost.getName()) ;
            PlanRecord rec = new PlanRecord(Coordinates.fromXY(lX.getDouble(), lY.getDouble()), lD.getDouble());
            LOG.debug("getBest() -> {}", rec );
            return Optional.of(rec);
          }
        }
        LOG.debug("No Selected map points");
        return Optional.empty();
    }
    
    public void reset(Coordinates target) {
        points = ModelFactory.createDefaultModel();
        points.add(complete);
        Resource targetResource = asRDF(target);
        points.add(targetResource.getModel());
        ExprFactory exprF = new ExprFactory();
        Var dist = Var.alloc("dist");
        Var other = Var.alloc("other");
        UpdateBuilder ub = new UpdateBuilder() 
            .addInsert( other, Namespace.targetRange, dist )
            .addWhere( other, RDF.type, Namespace.Coord)
            .addWhere( dist, Namespace.distF, exprF.asList(other,targetResource));
        UpdateExecutionFactory.create(ub.build(), DatasetFactory.create(points)).execute();
    }
    
    public Collection<PlanRecord> getPlanRecords() {
        Var x = Var.alloc("x");
        Var y = Var.alloc("y");
        Var dist = Var.alloc("dist");
        Var other = Var.alloc("other");
        SelectBuilder sb = new SelectBuilder()
                .addVar(x).addVar(y).addVar(dist)
                .addWhere(other, RDF.type, Namespace.Coord)
                .addWhere(other, Namespace.x, x)
                .addWhere(other, Namespace.y, y)
                .addWhere(other, Namespace.targetRange, dist);

        
        SortedSet<PlanRecord> candidates = new TreeSet<PlanRecord>();
        try (QueryExecution qexec = QueryExecutionFactory.create(sb.build(), points)) {
          ResultSet results = qexec.execSelect() ;
          for ( ; results.hasNext() ; )
          {
            QuerySolution soln = results.nextSolution() ;
            Literal lX = soln.getLiteral(x.getName()) ;
            Literal lY = soln.getLiteral(y.getName()) ; 
            Literal lD = soln.getLiteral(dist.getName()) ;
            PlanRecord pr = new PlanRecord(Coordinates.fromXY(lX.getDouble(), lY.getDouble()), lD.getDouble());
            candidates.add(pr);
          }
        }
        return candidates;
    }
    
    public class Dist extends PropertyFunctionEval
    {
        public Dist(PropFuncArgType subjArgType, PropFuncArgType objFuncArgType) {
            super(subjArgType, objFuncArgType);
        }
        
//        @Override
//        public Node calc(Node node) {
//            // TODO Auto-generated method stub
//            return null;
//        }

        private double getValue(Node a, Node p) {
            return (Double) a.getGraph().find( a, p, Node.ANY).next().getObject().getLiteralValue();
        }
        
        
        @Override
        public QueryIterator execEvaluated(Binding binding, PropFuncArg argSubject, Node predicate,
                PropFuncArg argObject, ExecutionContext execCxt) {
         // Subject bound to something other a literal. 
            if ( !argSubject.isNode() || argSubject.getArg().isURI() || argSubject.getArg().isBlank() ) {
                Log.warn(this, "Invalid subject type") ;
                return IterLib.noResults(execCxt) ;
            }
            
            if (!argObject.isList()) {
                Log.warn(this, "Invalid object type");
                return IterLib.noResults(execCxt) ;
            }
            
            List<Node> args = argObject.getArgList();
            if (args.size()!= 2) {
                Log.warn(this, "Expected 2 arguments") ;
                return IterLib.noResults(execCxt) ;
            }
            

            if (!args.get(0).isURI()) {
                Log.warn(this, "Object argument 0 must be URI") ;
                return IterLib.noResults(execCxt) ;
            }
            if (!args.get(1).isURI()) {
                Log.warn(this, "Object argument 1 must be URI") ;
                return IterLib.noResults(execCxt) ;
            }
            

            //Graph graph = execCxt.getActiveGraph();
        

            double deltaX = getValue(args.get(0), Namespace.x.asNode()) - getValue(args.get(1), Namespace.x.asNode());
            double deltaY = getValue(args.get(0), Namespace.y.asNode()) - getValue(args.get(1), Namespace.y.asNode());
            double value = Math.sqrt( (deltaX*deltaX) + (deltaY*deltaY));
            Node nValue = ResourceFactory.createTypedLiteral(value).asNode();

            Node subject = argSubject.getArg();
            if ( Var.isVar(subject) ) {
                return IterLib.oneResult(binding, Var.alloc(subject), nValue, execCxt);
            }
        
            // Subject bound : check it.
            if ( subject.equals(nValue) )
                return IterLib.result(binding, execCxt) ;
            return IterLib.noResults(execCxt) ;
            
            
        }
    }
}
