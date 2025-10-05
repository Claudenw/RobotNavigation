package org.xenei.robot.mapper.map;

import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.geosparql.implementation.vocabulary.Geo;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.update.UpdateRequest;
import org.apache.jena.vocabulary.RDF;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.common.FrontsCoordinate;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.UnmodifiableCoordinate;
import org.xenei.robot.common.mapping.Map;
import org.xenei.robot.common.utils.CoordUtils;
import org.xenei.robot.common.utils.RobutContext;
import org.xenei.robot.mapper.MapReports;
import org.xenei.robot.mapper.rdf.Namespace;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * The map coordinates for this implementation.
 */
public class MapLocation implements Map.Loc<MapLocation> {

    private static final Logger LOG = LoggerFactory.getLogger(MapLocation.class);
    private static final String urnFmt = "java:" + MapLocation.class.getName() + ":%s:%s";
    private static final Var visitedV = Var.alloc("visited");
    private static final Var distanceV = Var.alloc("distance");
    private static final Var indirectV = Var.alloc("indirect");
    private static final Var targetV = Var.alloc("target");

    final MapImpl map;
    private final Resource urn;
    private boolean visited;

    private final HashMap<Coordinate, TargetData> targets = new HashMap<>();
    private final UnmodifiableCoordinate coordinate;

    MapLocation(MapImpl map, Coordinate coordinate) {
        this.coordinate = UnmodifiableCoordinate.make(coordinate);
        this.map = map;
        double multiplier = Math.pow(10, map.getContext().scaleInfo.decimalPlaces());
        long lX = (long) (getCoordinate().x * multiplier);
        long lY = (long) (getCoordinate().y * multiplier);
        Resource r = ResourceFactory.createResource(String.format(urnFmt, lX, lY));
        if (map.ask(askExists(r))) {
            urn = map.readSubModel(r).join();
        } else {
            urn = ModelFactory.createDefaultModel().createResource(r.getURI(), Namespace.Coord);
        }
        boolean dirty = false;
        if (!urn.hasProperty(RDF.type)) {
            urn.addProperty(RDF.type, Namespace.Coord);
            dirty = true;
        }
        if (!urn.hasProperty(Namespace.x) || !urn.hasProperty(Namespace.y)) {
            urn.addLiteral(Namespace.x, getCoordinate().x);
            urn.addLiteral(Namespace.y, getCoordinate().y);
            dirty = true;
        }
        if (!urn.hasProperty(Geo.AS_WKT_PROP)) {
            Geometry geometry = map.getContext().geometryUtils.asPoint(getCoordinate());
            urn.addLiteral(Geo.AS_WKT_PROP, map.getContext().graphGeomFactory.asWKT(geometry));
            dirty = true;
        }
        if (!urn.hasProperty(Namespace.visited)) {
            urn.addLiteral(Namespace.visited, false);
            dirty = true;
        }
        if (dirty) {
            map.updateSubModel(urn, false);
        }
        loadTargets();
    }

    /**
     * Reads the targets from the grap and adds them to the target list.
     */
    private void loadTargets() {
        List<Map.TargetData> newTargets = new ArrayList<>();

        CompletableFuture<ResultSet> rsFuture = map.exec(new SelectBuilder()
                .addVar(Namespace.s)
                .addWhere(Namespace.s, Namespace.point, getWkt()));
        CompletableFuture<List<CompletableFuture<Resource>>> resFuture = rsFuture.thenApply(resultSet -> {
            List<CompletableFuture<Resource>> futures = new ArrayList<>();
            resultSet.forEachRemaining(soln -> {
                futures.add(map.readSubModel(soln.getResource(Namespace.s.getName())));
            });
            return futures;
        });
        resFuture.thenApply(futures -> CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])));
    }

    public Resource getUrn() {
        return urn;
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
        return (Geometry) getWkt().getValue();
    }

    MapImpl getMap() {
        return map;
    }

    void removeTarget(MapLocation target) {
        targets.remove(target.coordinate);
    }

    public void removeTargets(Collection<MapLocation> deadTargets) {
        deadTargets.forEach(this::removeTarget);
    }

    public Map.TargetData addTarget(FrontsCoordinate target) {
        TargetData targetData = targets.get(target.getCoordinate());
        if (targetData == null) {
            targetData = new TargetData(target.getCoordinate());
            targets.put(target.getCoordinate(), targetData);
        } else {
            map.readSubModel(targetData.data).thenAccept(resource -> targets.put(target.getCoordinate(),
                    new TargetData(resource)));
        }
        return targetData;
    }

//    private Map.TargetData createTargetData(MapLocation target, Map.TargetData<Coordinate> value) {
//        if (value != null) {
//            return value;
//        }
//
//        SelectBuilder sb = new SelectBuilder().addVar(distanceV).addVar(indirectV)
//                .addWhere(urn, Namespace.target, targetV).addWhere(targetV, Namespace.point, target.wkt)
//                .addOptional(targetV, Namespace.distance, distanceV)
//                .addOptional(targetV, Namespace.isIndirect, indirectV);
//
//        return map.exec(sb).thenApply(rs -> {
//            Double distance = null;
//            Boolean indirect = null;
//            if (rs.hasNext()) {
//                QuerySolution solution = rs.next();
//                Literal literal = solution.getLiteral(distanceV.getVarName());
//                distance = (literal == null) ? null : literal.getDouble();
//                literal = solution.getLiteral(indirectV.getVarName());
//                indirect = (literal == null) ? null : literal.getBoolean();
//            }
//            if (distance == null) {
//                distance = distance(target.getCoordinate());
//            }
//            if (indirect == null) {
//                indirect = this.hasClearPath(target);
//            }
//            return new Map.TargetData<>(target, distance, indirect);
//        }).join();
//    }

    @Override
    public boolean isIndirect(FrontsCoordinate target) {
        return addTarget(target).indirect();
    }

    public Literal getWkt() {
        return urn.getProperty(Geo.AS_WKT_PROP).getLiteral();
    }

    boolean isVisited() {
        return urn.getProperty(Namespace.visited).getLiteral().getBoolean();
    }

//    CompletableFuture<MapLocation> updateValue(Property property, Object value) {
//        return map
//                .doUpdate(new UpdateRequest()
//                        .add(new UpdateBuilder().addDelete(Namespace.PlanningModel, urn, property, Namespace.o)
//                                .addGraph(Namespace.PlanningModel,
//                                        new SelectBuilder().addWhere(urn, property, Namespace.o))
//                                .build())
//                        .add(new UpdateBuilder().addInsert(Namespace.PlanningModel, urn, property, value).build()))
//                .thenApply(n -> this);
//    }

//    CompletableFuture<MapLocation> updateOrCreate(Supplier<CompletableFuture<MapLocation>> updateFunction) {
//        return exists ? updateFunction.get() : update();
//    }

    void setVisited() {
        if (!isVisited()) {
            urn.removeAll(Namespace.visited);
            urn.addLiteral(Namespace.visited, true);
            map.updateSubModel(urn, false);
        }
    }

    private AskBuilder askExists(Resource urn) {
        return new AskBuilder().from(Namespace.UnionModel.getURI()).addWhere(urn, RDF.type, Namespace.Coord);
    }

    @Override
    public String toString() {
        return "MapLocation [" + CoordUtils.toString(getCoordinate(), 1) + "]";
    }

//    CompletableFuture<MapLocation> update() {
//        UpdateRequest req = new UpdateRequest();
//        req.add(new UpdateBuilder().addDelete(Namespace.PlanningModel, urn, Namespace.target, targetV)
//                .addGraph(Namespace.PlanningModel, new SelectBuilder().addWhere(urn, Namespace.target, targetV))
//                .build());
//        org.apache.jena.rdf.model.Model model = ModelFactory.createDefaultModel();
//        model.add(urn.getModel());
//        for (Map.TargetData targetData : targets.values()) {
//            Resource targ = model.createResource();
//            model.add(urn, Namespace.target, targ);
//            targ.addLiteral(Namespace.point, targetData.target().wkt);
//            targ.addLiteral(Namespace.distance, targetData.distance());
//            targ.addLiteral(Namespace.isIndirect, targetData.indirect());
//        }
//        req.add(new UpdateBuilder().addInsert(Namespace.PlanningModel, model).build());
//        return map.doUpdate(req).thenApply(rs -> this);
//    }

    public boolean hasClearPath(MapLocation target) {
        TargetData targetData = targets.get(target.coordinate);
        if (targetData == null) {
            targetData = new TargetData(target.coordinate);
            targets.put(target.coordinate, targetData);
        }
        return !targetData.indirect();
    }

    public class TargetData implements Map.TargetData {
        private static String fmt = "java:" + TargetData.class.getName() + ":%s:%s:%s:%s";
        private final Resource data;

        private Resource createResource(Coordinate target) {
            double multiplier = Math.pow(10, map.getContext().scaleInfo.decimalPlaces());
            long lX = (long) (getCoordinate().x * multiplier);
            long lY = (long) (getCoordinate().y * multiplier);
            long tX = (long) (target.x * multiplier);
            long tY = (long) (target.y * multiplier);
            return ResourceFactory.createResource(String.format(fmt, lX, lY, tX, tY));
        }

        private void populateTargetData(Coordinate target) {
            data.addProperty(RDF.type, Namespace.TargetData);
            data.addLiteral(Namespace.point, map.getContext().graphGeomFactory.asWKT(target));
            data.addProperty(Namespace.point, MapLocation.this.getWkt());
            data.addLiteral(Namespace.distance, MapLocation.this.distance(target));
            boolean clearPath = MapLocation.this.map.isClearPath(MapLocation.this, new Location(target));
            data.addLiteral(Namespace.isIndirect, !clearPath);
        }

//        TargetData(Coordinate target, double distance, boolean indirect) {
//            data = map.readSubModel(createResource(target))
//                    .thenApply(r -> {
//                                r.addProperty(RDF.type, Namespace.TargetData);
//                                r.addLiteral(Namespace.point, map.getContext().graphGeomFactory.asWKT(target));
//                                r.addLiteral(Namespace.distance, distance);
//                                r.addProperty(Namespace.point, MapLocation.this.wkt);
//                                r.addLiteral(Namespace.isIndirect, indirect);
//                                return r;
//                            })
//                    .thenCompose(map::updateSubModel).join();
//        }

        TargetData(Coordinate target) {
            data = map.readSubModel(createResource(target)).join();
            if (data.getModel().isEmpty()) {
                populateTargetData(target);
                map.updateSubModel(data);
            }
        }

        private TargetData(Resource resource) {
            if (resource.getModel() == null) {
                throw new IllegalStateException(String.format("Resource %s has no model", resource));
            }
            data = resource;
        }

        @Override
        public double distance() {
            return data.getProperty(Namespace.distance).getLiteral().getDouble();
        }

        @Override
        public boolean indirect() {
            return data.getProperty(Namespace.isIndirect).getLiteral().getBoolean();
        }

        public void setIndirect(boolean state) {
            data.getProperty(Namespace.isIndirect).changeLiteralObject(state);
            map.updateSubModel(data);
        }

        @Override
        public Coordinate getTarget() {
            Literal wkt = data.listProperties(Namespace.point).filterDrop(s -> s.getLiteral().equals(MapLocation.this.getWkt()))
                    .next().getLiteral();
            return MapLocation.this.getMap().getContext().graphGeomFactory.fromWkt(wkt).getCoordinate();
        }
    }
}
