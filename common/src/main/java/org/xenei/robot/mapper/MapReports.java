package org.xenei.robot.mapper;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.apache.jena.arq.querybuilder.ExprFactory;
import org.apache.jena.arq.querybuilder.Order;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.geosparql.implementation.vocabulary.Geo;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.vocabulary.RDF;
import org.xenei.robot.common.mapping.CoordinateMap;
import org.xenei.robot.common.mapping.CoordinateMap.Coord;
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
        return dumpModel(map, Namespace.UnionModel);
    }

    public static String dumpModel(MapImpl map, Resource model) {
        Dumper d = new Dumper();
        map.dump(model, d);
        return d.toString();
    }

    public static String dumpModel(Model model) {
        Dumper d = new Dumper();
        d.accept(model);
        return d.toString();
    }

    public static String dumpQuery(MapImpl map, SelectBuilder sb) {
        StringBuilder builder = new StringBuilder();
        map.exec(sb, (s) -> {
            builder.append(s.toString()).append("\n");
            return true;
        });
        return builder.toString();
    }

    public static String dumpDistance(MapImpl map) {

        // cost of trip.
        Var cost = Var.alloc("cost");
        // wkt of other
        Var otherWkt = Var.alloc("otherWkt");
        // distance from other to target
        Var otherDist = Var.alloc("otherDist");
        // additional adjustment from other to target

        Var indirect = Var.alloc("indirect");
        Var indirectFlg = Var.alloc("indirectFlg");
        Var visited = Var.alloc("visited");
        Var x = Var.alloc("x");
        Var y = Var.alloc("y");

        ExprFactory exprF = new ExprFactory(MapImpl.getPrefixMapping());

        Expr indirectCalc = exprF.cond(exprF.bound(indirect), exprF.asExpr(otherDist), exprF.asExpr(0));
        Expr distCalc = indirectCalc;

        SelectBuilder query = new SelectBuilder().addVar(x).addVar(y).addVar(cost) //
                .addVar(visited).addVar(otherDist).addVar(indirect) //
                .from(Namespace.UnionModel.getURI()) //
                .addWhere(Namespace.s, RDF.type, Namespace.Coord) //
                .addWhere(Namespace.s, Namespace.x, x).addWhere(Namespace.s, Namespace.y, y)
                .addOptional(Namespace.s, Namespace.visited, visited) //
                // .addFilter( exprF.not(exprF.bound(visited))) //
                .addWhere(Namespace.s, Namespace.distance, otherDist) //
                .addWhere(Namespace.s, Geo.AS_WKT_PROP, otherWkt) //
                .addOptional(Namespace.s, Namespace.isIndirect, indirect) //
                .addBind(distCalc, cost) //
                .addBind(exprF.cond(exprF.bound(indirect), exprF.asExpr(-1), exprF.asExpr(0)), indirectFlg)
                .addOrderBy(cost, Order.ASCENDING);

        StringBuilder builder = new StringBuilder().append("'x','y','cost','dist','visited','indirect'\n");
        Predicate<QuerySolution> p = soln -> {
            builder.append(String.format("%s,%s,%s,%s,%s,%s\n", //
                    soln.getLiteral(x.getName()).getDouble(), //
                    soln.getLiteral(y.getName()).getDouble(), //
                    soln.getLiteral(cost.getName()).getDouble(), //
                    soln.getLiteral(otherDist.getName()).getDouble(), //
                    soln.contains(visited.getName()), //
                    soln.contains(indirect.getName()) //
            ));
            return true;
        };

        map.exec(query, p);
        return builder.toString();
    }

//    /**
//     * Generates the map for display.
//     * 
//     * @param c the character to use for enabled items.
//     * @return the StringBuilder.
//     */
//    public StringBuilder stringBuilder(MapImpl map) {
//        StringBuilder sb = new StringBuilder();
//        List<CoordinateMap.Coord> points = new ArrayList<>();
//        SelectBuilder sb = new SelectBuilder()
//                
//        
//        int minX = points.stream().map(c -> asInt(c.getX())).min(Integer::compare).get();
//        Coord row = points.first();
//        int rowY = asInt(row.getY());
//        StringBuilder rowBuilder = new StringBuilder();
//        for (Coord point : points) {
//            if (rowY != asInt(point.getY())) {
//                sb.append(rowBuilder.append("\n"));
//                rowBuilder = new StringBuilder();
//                row = point;
//                rowY = asInt(row.getY());
//            }
//            int x = asInt(point.getX()) - minX;
//            if (x > -rowBuilder.length()) {
//                for (int i = rowBuilder.length(); i < x; i++) {
//                    rowBuilder.append(' ');
//                }
//            }
//            rowBuilder.append(point.c);
//        }
//        sb.append(rowBuilder.append("\n"));
//        return sb;
//    }
    
    private static class Dumper implements Consumer<Model> {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        @Override
        public void accept(Model t) {
            t.write(bos, Lang.TURTLE.getName());
        }

        @Override
        public String toString() {
            return bos.toString();
        }
    }
   
}
