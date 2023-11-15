package org.xenei.robot.planner.rdf;

import java.util.List;

import org.apache.jena.atlas.logging.Log;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.ExecutionContext;
import org.apache.jena.sparql.engine.QueryIterator;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.pfunction.PropFuncArg;
import org.apache.jena.sparql.pfunction.PropFuncArgType;
import org.apache.jena.sparql.pfunction.PropertyFunctionEval;
import org.apache.jena.sparql.util.IterLib;

public class Dist extends PropertyFunctionEval {
    public Dist() {
        super(PropFuncArgType.PF_ARG_SINGLE, PropFuncArgType.PF_ARG_LIST);
    }

    private double getValue(Graph g, Node a, Node p) {
        return (Double) g.find(a, p, Node.ANY).next().getObject().getLiteralValue();
    }

    @Override
    public QueryIterator execEvaluated(Binding binding, PropFuncArg argSubject, Node predicate,
            PropFuncArg argObject, ExecutionContext execCxt) {
        // Subject bound to something other a literal.
        if (!argSubject.isNode() || argSubject.getArg().isURI() || argSubject.getArg().isBlank()) {
            Log.warn(this, "Invalid subject type");
            return IterLib.noResults(execCxt);
        }

        if (!argObject.isList()) {
            Log.warn(this, "Invalid object type");
            return IterLib.noResults(execCxt);
        }

        List<Node> args = argObject.getArgList();
        if (args.size() != 2) {
            Log.warn(this, "Expected 2 arguments");
            return IterLib.noResults(execCxt);
        }

        if (!args.get(0).isURI()) {
            Log.warn(this, "Object argument 0 must be URI");
            return IterLib.noResults(execCxt);
        }
        if (!args.get(1).isURI()) {
            Log.warn(this, "Object argument 1 must be URI");
            return IterLib.noResults(execCxt);
        }

        Graph graph = execCxt.getActiveGraph();

        double deltaX = getValue(graph, args.get(0), Namespace.x.asNode())
                - getValue(graph, args.get(1), Namespace.x.asNode());
        double deltaY = getValue(graph, args.get(0), Namespace.y.asNode())
                - getValue(graph, args.get(1), Namespace.y.asNode());
        double value = Math.sqrt((deltaX * deltaX) + (deltaY * deltaY));
        Node nValue = ResourceFactory.createTypedLiteral(value).asNode();

        Node subject = argSubject.getArg();
        if (Var.isVar(subject)) {
            return IterLib.oneResult(binding, Var.alloc(subject), nValue, execCxt);
        }

        // Subject bound : check it.
        if (subject.equals(nValue))
            return IterLib.result(binding, execCxt);
        return IterLib.noResults(execCxt);

    }
}