package org.xenei.robot.mapper.map;

import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.geosparql.implementation.vocabulary.Geo;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.sparql.core.Var;
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

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

/**
 * The map coordinates for this implementation.
 */
public class MapLocation implements Map.Loc<MapLocation> {

    private static final Logger LOG = LoggerFactory.getLogger(MapLocation.class);
    private static final String urnFmt = Namespace.CLASS_URI + MapLocation.class.getName() + ":%s:%s";
    private static final Var visitedV = Var.alloc("visited");
    private static final Var distanceV = Var.alloc("distance");
    private static final Var indirectV = Var.alloc("indirect");
    private static final Var targetV = Var.alloc("target");

    private final Map.TargetData selfTargetData = new Map.TargetData() {
        @Override
        public Coordinate getTarget() {
            return MapLocation.this.coordinate;
        }

        @Override
        public double distance() {
            return 0;
        }

        @Override
        public boolean indirect() {
            return false;
        }
    };

    final MapImpl map;
    private final Resource urn;
    private boolean visited;

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

    CompletableFuture<?> removeTarget(MapLocation target) {
        if (target.coordinate.equals2D(this.coordinate)) {
            return CompletableFuture.completedFuture(null);
        }
        return map.deleteSubModel(createTargetUri(target.coordinate));
    }

    public void removeTargets(Collection<MapLocation> deadTargets) {
        deadTargets.forEach(this::removeTarget);
    }

    public CompletableFuture<? extends Map.TargetData> addTarget(FrontsCoordinate target) {
        Coordinate coordinate = this.map.scaleCoordinate(target.getCoordinate());
        if (coordinate.equals2D(this.coordinate)) {
            return CompletableFuture.completedFuture(selfTargetData);
        }
        return getTargetData(coordinate);
    }

    // private Map.TargetData createTargetData(MapLocation target,
    // Map.TargetData<Coordinate> value) {
    // if (value != null) {
    // return value;
    // }
    //
    // SelectBuilder sb = new SelectBuilder().addVar(distanceV).addVar(indirectV)
    // .addWhere(urn, Namespace.target, targetV).addWhere(targetV, Namespace.point,
    // target.wkt)
    // .addOptional(targetV, Namespace.distance, distanceV)
    // .addOptional(targetV, Namespace.isIndirect, indirectV);
    //
    // return map.exec(sb).thenApply(rs -> {
    // Double distance = null;
    // Boolean indirect = null;
    // if (rs.hasNext()) {
    // QuerySolution solution = rs.next();
    // Literal literal = solution.getLiteral(distanceV.getVarName());
    // distance = (literal == null) ? null : literal.getDouble();
    // literal = solution.getLiteral(indirectV.getVarName());
    // indirect = (literal == null) ? null : literal.getBoolean();
    // }
    // if (distance == null) {
    // distance = distance(target.getCoordinate());
    // }
    // if (indirect == null) {
    // indirect = this.hasClearPath(target);
    // }
    // return new Map.TargetData<>(target, distance, indirect);
    // }).join();
    // }

    @Override
    public boolean isIndirect(FrontsCoordinate target) {
        return addTarget(target).join().indirect();
    }

    public Literal getWkt() {
        return urn.getProperty(Geo.AS_WKT_PROP).getLiteral();
    }

    boolean isVisited() {
        return urn.getProperty(Namespace.visited).getLiteral().getBoolean();
    }

    // CompletableFuture<MapLocation> updateValue(Property property, Object value) {
    // return map
    // .doUpdate(new UpdateRequest()
    // .add(new UpdateBuilder().addDelete(Namespace.PlanningModel, urn, property,
    // Namespace.o)
    // .addGraph(Namespace.PlanningModel,
    // new SelectBuilder().addWhere(urn, property, Namespace.o))
    // .build())
    // .add(new UpdateBuilder().addInsert(Namespace.PlanningModel, urn, property,
    // value).build()))
    // .thenApply(n -> this);
    // }

    // CompletableFuture<MapLocation>
    // updateOrCreate(Supplier<CompletableFuture<MapLocation>> updateFunction) {
    // return exists ? updateFunction.get() : update();
    // }

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

    // CompletableFuture<MapLocation> update() {
    // UpdateRequest req = new UpdateRequest();
    // req.add(new UpdateBuilder().addDelete(Namespace.PlanningModel, urn,
    // Namespace.target, targetV)
    // .addGraph(Namespace.PlanningModel, new SelectBuilder().addWhere(urn,
    // Namespace.target, targetV))
    // .build());
    // org.apache.jena.rdf.model.Model model = ModelFactory.createDefaultModel();
    // model.add(urn.getModel());
    // for (Map.TargetData targetData : targets.values()) {
    // Resource targ = model.createResource();
    // model.add(urn, Namespace.target, targ);
    // targ.addLiteral(Namespace.point, targetData.target().wkt);
    // targ.addLiteral(Namespace.distance, targetData.distance());
    // targ.addLiteral(Namespace.isIndirect, targetData.indirect());
    // }
    // req.add(new UpdateBuilder().addInsert(Namespace.PlanningModel,
    // model).build());
    // return map.doUpdate(req).thenApply(rs -> this);
    // }

    private boolean calcIntersects(Coordinate target) {
        RobutContext ctxt = map.getContext();
        // create a path between the two points.
        Literal pathWkt = ctxt.graphGeomFactory.asWKTString(this.getCoordinate(),
                target);
        Var wkt = Var.alloc("wkt");

        // see if the path intersects any obstacles.
        boolean intersects = map.ask(new AskBuilder().from(Namespace.UnionModel.getURI()) //
                .addWhere(Namespace.s, RDF.type, Namespace.Obst) //
                .addWhere(Namespace.s, Geo.AS_WKT_PROP, wkt)
                .addFilter(map.exprF.le(ctxt.graphGeomFactory.calcDistance(map.exprF, pathWkt, wkt), ctxt.chassisInfo.radius)));

        LOG.debug("calcIntersects from {} to {}: {}", this, target, intersects);
        return intersects;
    }

    public boolean hasClearPath(MapLocation target) {
        return !getTargetData(target.coordinate).join().indirect();
    }

    private Resource createTargetUri(Coordinate target) {
        double multiplier = Math.pow(10, map.getContext().scaleInfo.decimalPlaces());
        long lX = (long) (getCoordinate().x * multiplier);
        long lY = (long) (getCoordinate().y * multiplier);
        long tX = (long) (target.x * multiplier);
        long tY = (long) (target.y * multiplier);
        return ResourceFactory.createResource(String.format(TargetData.fmt, lX, lY, tX, tY));
    }

    private CompletableFuture<TargetData> getTargetData(Coordinate target) {
        return map.readSubModel(createTargetUri(target))
                .thenApply(data -> {
            if (data.getModel().isEmpty()) {
                data.addProperty(RDF.type, Namespace.TargetData);
                data.addLiteral(Namespace.point, map.getContext().graphGeomFactory.asWKT(target));
                data.addLiteral(Namespace.point, MapLocation.this.getWkt());
                data.addLiteral(Namespace.distance, MapLocation.this.distance(target));
                data.addLiteral(Namespace.isIndirect, calcIntersects(target));
                return map.updateSubModel(data);
            } else {
                return CompletableFuture.completedFuture(data);
            }
        }).thenCompose(future -> future.thenApply(TargetData::new));
    }

    public class TargetData implements Map.TargetData {
        private static String fmt = Namespace.CLASS_URI + TargetData.class.getName() + ":%s:%s:%s:%s";
        private final Resource data;

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
            Literal wkt = data.listProperties(Namespace.point)
                    .filterDrop(s -> s.getLiteral().equals(MapLocation.this.getWkt())).next().getLiteral();
            return MapLocation.this.getMap().getContext().graphGeomFactory.fromWkt(wkt).getCoordinate();
        }
    }
}
