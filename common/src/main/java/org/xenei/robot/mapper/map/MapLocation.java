package org.xenei.robot.mapper.map;

import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.geosparql.implementation.vocabulary.Geo;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.update.UpdateRequest;
import org.apache.jena.vocabulary.RDF;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.common.FrontsCoordinate;
import org.xenei.robot.common.UnmodifiableCoordinate;
import org.xenei.robot.common.mapping.Map;
import org.xenei.robot.common.utils.CoordUtils;
import org.xenei.robot.common.utils.RobutContext;
import org.xenei.robot.mapper.rdf.Namespace;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

/**
 * The map coordinates for this implementation.
 */
public class MapLocation implements Map.Loc<MapLocation> {

    private static final Logger LOG = LoggerFactory.getLogger(MapLocation.class);
    private static final String urnFmt = "java:org.xenei.robot.mapper.map.Coord:%s:%s";
    private static final Var visitedV = Var.alloc("visited");
    private static final Var distanceV = Var.alloc("distance");
    private static final Var indirectV = Var.alloc("indirect");
    private static final Var targetV = Var.alloc("target");

    final MapImpl map;
    private final Resource urn;
    private final Literal wkt;
    private final Geometry geometry;
    private boolean visited;
    private boolean exists;
    private CompletableFuture<?> future = CompletableFuture.completedFuture(null);

    private final HashMap<MapLocation, Map.TargetData<MapLocation>> targets = new HashMap<>();
    private final UnmodifiableCoordinate coordinate;

    MapLocation(MapImpl map, Coordinate coordinate) {
        this.coordinate = UnmodifiableCoordinate.make(coordinate);
        this.map = map;
        double multiplier = Math.pow(10, map.getContext().scaleInfo.decimalPlaces());
        long lX = (long) (getCoordinate().x * multiplier);
        long lY = (long) (getCoordinate().x * multiplier);
        geometry = map.getContext().geometryUtils.asPoint(getCoordinate());
        wkt = map.getContext().graphGeomFactory.asWKT(geometry);
        urn = ModelFactory.createMemModelMaker().createDefaultModel().createResource(String.format(urnFmt, lX, lY));
        urn.addLiteral(Namespace.x, getCoordinate().x);
        urn.addLiteral(Namespace.y, getCoordinate().y);
        urn.addLiteral(Geo.AS_WKT_PROP, wkt);
        urn.addProperty(RDF.type, Namespace.Coord);
        visited = map.ask(new AskBuilder().addWhere(urn, Namespace.visited, visitedV));
        if (visited) {
            urn.addLiteral(Namespace.visited, visited);
        }
        exists = map.ask(askExists());
    }

    @Override
    public MapLocation buildLocation(Coordinate coordinate) {
        return new MapLocation(map, coordinate);
    }

    @Override
    public UnmodifiableCoordinate unmodifiableCoordinate() {
        return coordinate;
    }

    @Override
    public Geometry getGeometry() {
        return geometry;
    }

    MapImpl getMap() {
        return map;
    }

    CompletableFuture<?> removeTarget(MapLocation target) {
        targets.remove(target);
        return map.doUpdate(new UpdateRequest()
                .add(new UpdateBuilder().addDelete(Namespace.PlanningModel, urn, Namespace.target, targetV)
                        .addGraph(Namespace.PlanningModel, new SelectBuilder().addWhere(urn, Namespace.target, targetV)
                                .addWhere(targetV, Namespace.point, target.wkt))
                        .build()));
    }

    public CompletableFuture<Map.TargetData<MapLocation>> addTarget(FrontsCoordinate target) {
        MapLocation mapTarget = map.asMapCoordinate(target);
        Map.TargetData<MapLocation> result = targets.get(mapTarget);
        if (result != null) {
            return CompletableFuture.completedFuture(result);
        }
        SelectBuilder sb = new SelectBuilder().addVar(distanceV).addVar(indirectV)
                .addWhere(urn, Namespace.target, targetV).addWhere(targetV, Namespace.point, mapTarget.wkt)
                .addOptional(targetV, Namespace.distance, distanceV)
                .addOptional(targetV, Namespace.isIndirect, indirectV);

        return map.exec(sb).thenApply(rs -> {
            Double distance = null;
            Boolean indirect = null;
            if (rs.hasNext()) {
                QuerySolution solution = rs.next();
                Literal literal = solution.getLiteral(distanceV.getVarName());
                distance = (literal == null) ? null : literal.getDouble();
                literal = solution.getLiteral(indirectV.getVarName());
                indirect = (literal == null) ? null : literal.getBoolean();
            }
            if (distance == null) {
                distance = distance(mapTarget.getCoordinate());
            }
            if (indirect == null) {
                indirect = !map.isClearPath(this, mapTarget);
            }
            Map.TargetData<MapLocation> result2 = new Map.TargetData<>(mapTarget, distance, indirect);
            targets.put(mapTarget, result2);
            return result2;
        });
    }

    @Override
    public boolean isIndirect(FrontsCoordinate target) {
        return addTarget(target).join().indirect();
    }

    public Literal getWkt() {
        return wkt;
    }

    boolean isVisited() {
        return visited;
    }

    CompletableFuture<MapLocation> updateValue(Property property, Object value) {
        return map
                .doUpdate(new UpdateRequest()
                        .add(new UpdateBuilder().addDelete(Namespace.PlanningModel, urn, property, Namespace.o)
                                .addGraph(Namespace.PlanningModel,
                                        new SelectBuilder().addWhere(urn, property, Namespace.o))
                                .build())
                        .add(new UpdateBuilder().addInsert(Namespace.PlanningModel, urn, property, value).build()))
                .thenApply(n -> this);
    }

    CompletableFuture<MapLocation> setVisited() {
        if (!visited) {
            visited = true;
            urn.addLiteral(Namespace.visited, true);
            return exists ? updateValue(Namespace.visited, true) : update();
        }
        return CompletableFuture.completedFuture(this);
    }

    AskBuilder askExists() {
        return new AskBuilder().from(Namespace.UnionModel.getURI()).addWhere(urn, RDF.type, Namespace.Coord);
    }

    boolean exists() {
        return exists;
    }

    @Override
    public String toString() {
        return "MapLocation [" + CoordUtils.toString(getCoordinate(), 1) + "]";
    }

    CompletableFuture<MapLocation> update() {
        UpdateRequest req = new UpdateRequest();
        req.add(new UpdateBuilder().addDelete(Namespace.PlanningModel, urn, Namespace.target, targetV)
                .addGraph(Namespace.PlanningModel, new SelectBuilder().addWhere(urn, Namespace.target, targetV))
                .build());
        org.apache.jena.rdf.model.Model model = ModelFactory.createDefaultModel();
        model.add(urn.getModel());
        for (Map.TargetData<MapLocation> targetData : targets.values()) {
            Resource targ = model.createResource();
            model.add(urn, Namespace.target, targ);
            targ.addLiteral(Namespace.point, targetData.target().wkt);
            targ.addLiteral(Namespace.distance, targetData.distance());
            targ.addLiteral(Namespace.isIndirect, targetData.indirect());
        }
        req.add(new UpdateBuilder().addInsert(Namespace.PlanningModel, model).build());
        return map.doUpdate(req).thenApply(rs -> this);
    }

    public boolean hasClearPath(MapLocation target) {
        RobutContext ctxt = map.getContext();
        Literal pathWkt = ctxt.graphGeomFactory.asWKTPath(ctxt.chassisInfo.radius, this.getCoordinate(),
                target.getCoordinate());
        Var wkt = Var.alloc("wkt");

        boolean result = map.ask(new AskBuilder().from(Namespace.UnionModel.getURI()) //
                .addWhere(Namespace.s, RDF.type, Namespace.Obst) //
                .addWhere(Namespace.s, Geo.AS_WKT_PROP, wkt)
                .addFilter(map.exprF.eq(ctxt.graphGeomFactory.calcDistance(map.exprF, pathWkt, wkt), 0)));

        LOG.debug("checked clearView from {} to {}: {}", this, target, result);
        return result;
    }
}
