package org.xenei.robot.mapper;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.jena.arq.querybuilder.ExprFactory;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.vocabulary.RDF;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.xenei.robot.mapper.rdf.Namespace;

public class MapReports {
    

//    public static List<CostModelEntry> costModel(MapImpl map) {
//        Var x1 = Var.alloc("x1");
//        Var y1 = Var.alloc("y1");
//        Var x2 = Var.alloc("x2");
//        Var y2 = Var.alloc("y2");
//        Var cost = Var.alloc("cost");
//        Var distance = Var.alloc("distance");
//        Var node1 = Var.alloc("node1");
//        Var node2 = Var.alloc("node2");
//        ExprFactory exprF = new ExprFactory();
//        SelectBuilder sb = new SelectBuilder().addVar(x1).addVar(y1).addVar(x2).addVar(y2).addVar(distance)
//                .addWhere(node1, RDF.type, Namespace.Coord).addWhere(node1, Namespace.x, x1)
//                .addWhere(node1, Namespace.y, y1).addWhere(node1, Namespace.path, node2)
//                .addWhere(node2, Namespace.x, x2).addWhere(node2, Namespace.y, y2)
//                .addWhere(node2, Namespace.distance, distance).addWhere(cost, Namespace.distF, List.of(node1, node2))
//                .addFilter(exprF.gt(cost, 0));
//
//        List<CostModelEntry> result = new ArrayList<>();
//        try (QueryExecution qexec = map.doQuery(sb)) {
//            ResultSet results = qexec.execSelect();
//            for (; results.hasNext();) {
//                QuerySolution soln = results.nextSolution();
//                result.add(new CostModelEntry(
//                        new Point(soln.getLiteral(x1.getName()).getDouble(), soln.getLiteral(y1.getName()).getDouble()),
//                        new Point(soln.getLiteral(x2.getName()).getDouble(), soln.getLiteral(y2.getName()).getDouble()),
//                        soln.getLiteral(distance.getName()).getDouble()));
//            }
//        }
//        return result;
//    }

    public static String dumpModel(MapImpl map) {
        return dumpModel(map.data.getUnionModel());
    }

    public static String dumpModel(MapImpl map, Resource model) {
        return dumpModel(map.data.getNamedModel(model));
    }
    
    public static String dumpModel(Model m) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        m.write(bos, Lang.TURTLE.getName());
        return bos.toString();
    }
    
    public static String dumpQuery(MapImpl map, SelectBuilder sb) {
        StringBuilder builder = new StringBuilder();
        try (QueryExecution qexec = map.doQuery(sb)) {
            ResultSet results = qexec.execSelect();
            while (results.hasNext()) {
                builder.append( results.next().toString()).append("\n");
            }
        }
        return builder.toString();
    }
}
