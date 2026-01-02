package org.xenei.robot.mapper.map;

import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.arq.querybuilder.ConstructBuilder;
import org.apache.jena.arq.querybuilder.ExprFactory;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.geosparql.implementation.vocabulary.Geo;
import org.apache.jena.geosparql.implementation.vocabulary.GeoSPARQL_URI;
import org.apache.jena.geosparql.implementation.vocabulary.SRS_URI;
import org.apache.jena.geosparql.spatial.SpatialIndex;
import org.apache.jena.geosparql.spatial.SpatialIndexException;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.riot.Lang;
import org.apache.jena.shared.Lock;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.path.Path;
import org.apache.jena.sparql.path.PathFactory;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateRequest;
import org.apache.jena.vocabulary.RDF;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.OctagonalEnvelope;
import org.xenei.robot.common.GeometricObject;
import org.xenei.robot.common.Obstacle;
import org.xenei.robot.common.mapping.MapLocation;
import org.xenei.robot.common.mapping.MapObstacle;
import org.xenei.robot.common.mapping.MapStorage;
import org.xenei.robot.common.utils.GeometryUtils;
import org.xenei.robot.common.utils.RobutContext;
import org.xenei.robot.common.DoubleHalfMatrix;
import org.xenei.robot.mapper.rdf.Namespace;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class RDFStorage implements MapStorage {
    public static final Path PATH_QUERY_PREDICATE = PathFactory.pathOneOrMore1(PathFactory.pathLink(Namespace.path.asNode()));
    private static final String MAP_LOCATION_URI = Namespace.CLASS_URI + MapLocation.class.getName() + ":%s:%s";

    private final RobutContext ctxt;
    private final Dataset data;
    private final ExprFactory exprF;

    public RDFStorage(RobutContext ctxt) {
        this.ctxt = ctxt;
        data = DatasetFactory.create();
        data.getDefaultModel().setNsPrefixes(getPrefixes());
        data.addNamedModel(Namespace.BaseModel, defaultModel());
        data.addNamedModel(Namespace.PlanningModel, defaultModel());
        data.addNamedModel(Namespace.KnownModel, defaultModel());
        exprF = new ExprFactory(data.getPrefixMapping());

        try {
            SpatialIndex.buildSpatialIndex(data, SRS_URI.DEFAULT_WKT_CRS84);
        } catch (SpatialIndexException e) {
            throw new RuntimeException(e);
        }
    }

    public static PrefixMapping getPrefixes() {
        return PrefixMapping.Factory.create().setNsPrefixes(GeoSPARQL_URI.getPrefixes())
                .setNsPrefixes(PrefixMapping.Standard).setNsPrefix("robut", Namespace.URI);
    }

    public static PrefixMapping getPrefixMapping() {
        PrefixMapping pm = PrefixMapping.Factory.create();
        pm.setNsPrefixes(GeoSPARQL_URI.getPrefixes());
        pm.setNsPrefixes(PrefixMapping.Standard.getNsPrefixMap());
        pm.setNsPrefix("robut", "urn:org.xenei.robot:");
        return pm;
    }

    private static Model defaultModel() {
        return ModelFactory.createDefaultModel().setNsPrefixes(getPrefixMapping());
    }

    private class LockHandler implements AutoCloseable {
        Lock lock;

        private LockHandler(boolean flag) {
            lock = data.getLock();
            lock.enterCriticalSection(flag);
        }

        private LockHandler(Model model, boolean flag) {
            lock = model.getLock();
            lock.enterCriticalSection(flag);
        }

        @Override
        public void close() {
            lock.leaveCriticalSection();
        }
    }

    private boolean ask(AskBuilder ask) {
        try (LockHandler ignored = new LockHandler(Lock.READ);
             QueryExecution exec = QueryExecutionFactory.create(ask.build(), data)) {
            return exec.execAsk();
        }
    }

    private CompletableFuture<?> doUpdate(UpdateBuilder update) {
        return ctxt.submit(() -> {
            try (LockHandler ignored = new LockHandler(Lock.WRITE)) {
                UpdateExecutionFactory.create(update.build(), data).execute();
            }
        });
    }

    private CompletableFuture<?> doUpdate(UpdateRequest request) {
        return ctxt.submit(() -> {
            try (LockHandler ignored = new LockHandler(Lock.WRITE)) {
                UpdateExecutionFactory.create(request, data).execute();
            }
        });
    }

    private CompletableFuture<Model> construct(ConstructBuilder select) {
        return ctxt.submit(() -> {
            try (LockHandler ignore = new LockHandler(Lock.READ);
                 QueryExecution qexec = QueryExecutionFactory.create(select.build(), data)) {
                return qexec.execConstruct();
            }
        });
    }

    /**
     * executes the select query and processes the result with the processor.
     * Processing stops when processor returns false.
     *
     * @param select the SelectBuilder to execute.
     */
    private CompletableFuture<ResultSet> exec(SelectBuilder select) {
        return ctxt.submit(() -> {
            try (LockHandler ignored = new LockHandler(Lock.READ);
                 QueryExecution qexec = QueryExecutionFactory.create(select.build(), data)) {
                return qexec.execSelect().materialise();
            }
        });
    }


    private AskBuilder askExists(Resource urn) {
        return new AskBuilder().from(Namespace.UnionModel.getURI()).addWhere(urn, RDF.type, Namespace.Coord);
    }

    /**
     * Read the resource and all properties and return it as a Resource with a
     * {@link Model} containing all the properties attached.
     *
     * @param resource the Resource to read.
     * @return a CompletableFuture containing the Resource with the Model.
     */
    CompletableFuture<Resource> readSubModel(Resource resource) {
        return construct(new ConstructBuilder().addConstruct(resource, Namespace.p, Namespace.o)
                .addGraph(Namespace.UnionModel, new WhereBuilder().addWhere(resource, Namespace.p, Namespace.o)))
                .thenApply(m -> m.getResource(resource.getURI()));
    }

    /**
     * Updates the Resource in the PlanningModel to contain all the data associated
     * with the resource. The resource must have a model attached.
     *
     * @param resource the resource to write. Must have a {@link Model} attached.
     * @return a CompletableFuture containing the resource.
     */
    private CompletableFuture<Resource> updateLocation(Resource resource) {
        if (resource.getModel() == null) {
            throw new IllegalStateException(String.format("Resource %s must have model", resource));
        }
        UpdateRequest req = new UpdateRequest()
                .add(new UpdateBuilder().addDelete(Namespace.PlanningModel, resource, Namespace.p, Namespace.o)
                        .addGraph(Namespace.PlanningModel, new WhereBuilder()
                                .addWhere(resource, Namespace.p, Namespace.o)
                                .addFilter(exprF.not(exprF.eq(Namespace.p, Namespace.path)))
                        ).build())
                .add(new UpdateBuilder().addInsert(Namespace.PlanningModel, resource.getModel()).build());
        return doUpdate(req).thenApply(x -> resource);
    }

    private Coordinate parseLocationResource(Resource resource) {
        String[] parts = resource.getURI().split(":");
        double lX = Double.parseDouble(parts[parts.length - 2]);
        double lY = Double.parseDouble(parts[parts.length - 1]);
        return new Coordinate(lX, lY);
    }

    public static Resource asResource(MapLocation mapLocation) {
        return ResourceFactory.createResource(String.format(MAP_LOCATION_URI, mapLocation.getCoordinate().x, mapLocation.getCoordinate().y));
    }

    @Override
    public Reports<Resource> getReports() {
        return new RDFReports();
    }

    @Override
    public CompletableFuture<MapLocation> saveLocation(MapLocation mapLocation) {
        Resource r = asResource(mapLocation);
        Resource urn = ask(askExists(r)) ? readSubModel(r).join() : ModelFactory.createDefaultModel().createResource(r.getURI(), Namespace.Coord);

        boolean dirty = false;
        if (!urn.hasProperty(RDF.type)) {
            urn.addProperty(RDF.type, Namespace.Coord);
            dirty = true;
        }
        if (!urn.hasProperty(Namespace.x) || !urn.hasProperty(Namespace.y)) {
            urn.addLiteral(Namespace.x, mapLocation.getCoordinate().x);
            urn.addLiteral(Namespace.y, mapLocation.getCoordinate().y);
            dirty = true;
        }
        if (!urn.hasProperty(Geo.AS_WKT_PROP)) {
            urn.addLiteral(Geo.AS_WKT_PROP, ctxt.graphGeomFactory.asWKT(mapLocation.getGeometry()));
            dirty = true;
        }
        Statement stmt = urn.getProperty(Namespace.visited);
        if (stmt == null) {
            urn.addLiteral(Namespace.visited, mapLocation.wasVisited());
            dirty = true;
        } else if (stmt.getLiteral().getBoolean() != mapLocation.wasVisited()) {
            stmt.changeLiteralObject(mapLocation.wasVisited());
            dirty = true;
        }
        return (dirty ? updateLocation(urn) : CompletableFuture.completedFuture(urn))
                .thenApply(x -> mapLocation);
    }

    @Override
    public CompletableFuture<Map<Coordinate, Set<Coordinate>>> getPath(MapLocation start, MapLocation target) {
        Var sWkt = Var.alloc("swkt");
        Var oWkt = Var.alloc("swkt");
        OctagonalEnvelope envelope = GeometryUtils.createBoundingBox(start.getCoordinate(), target.getCoordinate());
        Literal boundingBox = ctxt.graphGeomFactory.asWKT(envelope.toGeometry(ctxt.geometryFactory));
        HashMap<Coordinate, Set<Coordinate>> segmentPairs = new HashMap<>();
        /*
        Select segments from within the bounding box.
         */
        return exec(new SelectBuilder().addVar(Namespace.s).addVar(Namespace.o).addGraph(Namespace.UnionModel, new WhereBuilder()
                .addWhere(Namespace.s, Namespace.path, Namespace.o)
                .addFilter(exprF.lt(Namespace.s, Namespace.o))
                .addWhere(Namespace.s, Geo.AS_WKT_PROP, sWkt)
                .addFilter(ctxt.graphGeomFactory.intersects(exprF, sWkt, boundingBox))
                .addWhere(Namespace.o, Geo.AS_WKT_PROP, oWkt)
                .addFilter(ctxt.graphGeomFactory.intersects(exprF, oWkt, boundingBox))))
                .thenAccept(rs -> rs.forEachRemaining(qs -> segmentPairs.computeIfAbsent(parseLocationResource(qs.getResource(Namespace.s.getName())), k ->
                        new HashSet<>()).add(parseLocationResource(qs.getResource(Namespace.o.getName()))))).thenApply(x -> segmentPairs);
    }

    /**
     * Add the plan record to the map
     *
     * @param locations A collection of locations for the path.
     * @return An array of coordinates on the path.
     */
    @Override
    public CompletableFuture<?> addPath(Collection<MapLocation> locations) {
        Iterator<MapLocation> iter = locations.iterator();
        Literal start = ctxt.graphGeomFactory.asWKT(iter.next().getGeometry());
        UpdateBuilder updateBuilder = new UpdateBuilder();
        while (iter.hasNext()) {
            Literal nxt = ctxt.graphGeomFactory.asWKT(iter.next().getGeometry());
            updateBuilder.addInsert(Namespace.BaseModel, start, Namespace.path, nxt);
            start = nxt;
        }
        return doUpdate(updateBuilder);
    }

    @Override
    public void clear() {
        data.getNamedModel(Namespace.PlanningModel).removeAll();
    }

    private UUID parseUUID(Resource r) {
        return UUID.fromString(r.getURI().split(":")[2].trim());
    }

    private Resource fromUUID(UUID uuid) {
        return ResourceFactory.createResource("urn:uuid:" + uuid.toString());
    }

    @Override
    public CompletableFuture<Stream<Obstacle>> findTouchingObstacles(GeometricObject geometricObject) {
        Var wkt = Var.alloc("wkt");
        Literal object = ctxt.graphGeomFactory.asWKT(geometricObject.getGeometry());
        SelectBuilder sb = new SelectBuilder().setDistinct(true).addVar(Namespace.s).addVar(wkt) //
                .addGraph(Namespace.UnionModel, new WhereBuilder().addWhere(Namespace.s, RDF.type, Namespace.Obst) //
                        .addWhere(Namespace.s, Geo.AS_WKT_PROP, wkt)
                        .addFilter(ctxt.graphGeomFactory.isNearby(exprF, wkt, object, ctxt.scaleInfo.getResolution())));

        return exec(sb).thenApply(resultSet -> {
            List<Obstacle> result = new ArrayList<>();
            resultSet.forEachRemaining(soln -> result.add(Obstacle.asObstacle(parseUUID(soln.getResource(Namespace.s.getName())),
                    (Geometry) soln.getLiteral(wkt.getName()).getValue())));
            return result.stream();
        });
    }

    @Override
    public CompletableFuture<?> removeObstacles(Stream<UUID> obstacleIds) {
        UpdateRequest request = new UpdateRequest();
        obstacleIds.map(this::fromUUID).forEach(resource ->
                request.add(
                        new UpdateBuilder().addDelete(Namespace.UnionModel, resource, Namespace.p, Namespace.o)
                                .addGraph(Namespace.UnionModel, new WhereBuilder()
                                        .addWhere(resource, Namespace.p, Namespace.o)
                                        .addWhere(resource, RDF.type, Namespace.Obst)
                                ).build()
                )
        );
        return doUpdate(request);
    }

    @Override
    public CompletableFuture<?> addObstacle(MapObstacle mapObstacle) {
        Resource urn = fromUUID(mapObstacle.uuid());
        Literal wkt = ctxt.graphGeomFactory.asWKT(mapObstacle.getGeometry());
        return doUpdate(new UpdateBuilder()
                .addInsert(Namespace.KnownModel, urn, RDF.type, Namespace.Obst)
                .addInsert(Namespace.KnownModel, urn, Geo.AS_WKT_PROP, wkt));
    }

    @Override
    public CompletableFuture<Stream<Obstacle>> getObstacles() {
        return getObstacles(Namespace.UnionModel);
    }

    private CompletableFuture<Stream<Obstacle>> getObstacles(Resource graphName) {
        Var wkt = Var.alloc("wkt");

        return exec(new SelectBuilder()
                .addVar(Namespace.s).addVar(wkt)
                .addGraph(graphName, new WhereBuilder()
                        .addWhere(Namespace.s, RDF.type, Namespace.Obst)
                        .addWhere(Namespace.s, Geo.AS_WKT_PROP, wkt)
                )).thenApply(resultSet -> {
            List<Obstacle> result = new ArrayList<>();
            resultSet.forEachRemaining(soln -> result.add(Obstacle.asObstacle(
                    parseUUID(soln.getResource(Namespace.s.getName())),
                    (Geometry) soln.getLiteral(wkt.getName()).getValue())));
            return result.stream();
        });
    }

    @Override
    public CompletableFuture<Stream<Coordinate>> getLocations(Geometry boundingBox) {
        Var x = Var.alloc("x");
        Var y = Var.alloc("y");

        List<Coordinate> result = new ArrayList<>();
        WhereBuilder graphWhere = new WhereBuilder()
                .addWhere(Namespace.s, Namespace.x, x)
                .addWhere(Namespace.s, Namespace.y, y)
                .addWhere(Namespace.s, RDF.type, Namespace.Coord);
        if (boundingBox != null) {
            Var wkt = Var.alloc("wkt");
            Literal boundingBoxLiteral = ctxt.graphGeomFactory.asWKT(boundingBox);
            graphWhere.addWhere(Namespace.s, Geo.AS_WKT_PROP, wkt)
                    .addFilter(ctxt.graphGeomFactory.covers(exprF, boundingBoxLiteral, wkt));
        }
        return exec(new SelectBuilder().addVar(x).addVar(y)
                .addGraph(Namespace.UnionModel, graphWhere))
                .thenAccept(resultSet -> resultSet.forEachRemaining(soln -> result.add(new Coordinate(soln.getLiteral(x.getName()).getDouble(),
                        soln.getLiteral(y.getName()).getDouble())))).thenApply(z -> result.stream());
    }

    @Override
    public CompletableFuture<?> removeCoordinates(GeometricObject boundingBox) {
        Var wkt = Var.alloc("wkt");
        Literal boundingBoxLiteral = ctxt.graphGeomFactory.asWKT(boundingBox.getGeometry());
        SelectBuilder sb = new SelectBuilder().setDistinct(true).addVar(Namespace.s).addVar(wkt) //
                .addGraph(Namespace.UnionModel, new WhereBuilder().addWhere(Namespace.s, RDF.type, Namespace.Coord) //
                        .addWhere(Namespace.s, Geo.AS_WKT_PROP, wkt)
                        .addFilter(ctxt.graphGeomFactory.isCoveredBy(exprF, wkt, boundingBoxLiteral))
                );

        return exec(sb).thenApply(resultSet -> {
            Set<CompletableFuture<?>> futures = new HashSet<>();
            resultSet.forEachRemaining(soln -> futures.add(
                    removeCoordinate(soln.getResource(Namespace.s.getName()))));
            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        });
    }

    private CompletableFuture<?> removeCoordinate(Resource resource) {
        UpdateRequest request = new UpdateRequest();
        request.add(
                new UpdateBuilder().addDelete(Namespace.UnionModel, resource, Namespace.p, Namespace.o)
                        .addGraph(Namespace.UnionModel, new WhereBuilder()
                                .addWhere(resource, Namespace.p, Namespace.o)
                                .addWhere(resource, RDF.type, Namespace.Coord)
                        ).build()
        ).add(
                new UpdateBuilder().addDelete(Namespace.UnionModel, Namespace.s, Namespace.p, resource)
                        .addGraph(Namespace.UnionModel, new WhereBuilder()
                                .addWhere(Namespace.s, Namespace.p, resource)
                        ).build()
        );
        return doUpdate(request);
    }

    @Override
    public CompletableFuture<Boolean> hasPath(MapLocation a, MapLocation b) {
        return CompletableFuture.completedFuture(ask(new AskBuilder()
                .addGraph(Namespace.UnionModel, new WhereBuilder().addWhere(
                        asResource(a), PATH_QUERY_PREDICATE, asResource(b)))));
    }

    class RDFReports implements MapStorage.Reports<Resource> {

        private Resource modelOrDefault(Resource subModel) {
            return subModel == null ? Namespace.UnionModel : subModel;
        }

        public String dumpModel(Resource model) {
            Dumper d = new Dumper();
            dump(modelOrDefault(model) , d);
            return d.toString();
        }

        private void dump(Resource modelName, Consumer<Model> consumer) {
            try (LockHandler ignored = new LockHandler(Lock.READ)) {
                consumer.accept(data.getNamedModel(modelName));
            }
        }

        public String dumpModel(Model model) {
            Dumper d = new Dumper();
            try (LockHandler ignored = new LockHandler(model, Lock.READ)) {
                d.accept(model);
            }
            return d.toString();
        }

        public String dumpQuery(SelectBuilder sb) {
            StringBuilder builder = new StringBuilder();
            exec(sb).thenAccept(resultSet -> resultSet.forEachRemaining(s -> builder.append(s.toString()).append("\n"))).join();
            return builder.isEmpty() ? "No data" : builder.toString();
        }

        public String dumpObstacles(Resource subModel) {
            StringBuilder builder = new StringBuilder();
            getObstacles(modelOrDefault(subModel)).join().forEach(obst -> builder.append(obst).append("\n"));
            return builder.toString();
        }

        public String dumpObstacleDistance(Resource subModel) {
            StringBuilder builder = new StringBuilder();
            TreeSet<Obstacle> obs = new TreeSet<>(Obstacle.comp);
            getObstacles(modelOrDefault(subModel)).join().forEach(obs::add);
            List<Obstacle> lst = new ArrayList<>(obs);
            DoubleHalfMatrix dist = new DoubleHalfMatrix(lst.size());

            for (int i = 0; i < lst.size() - 1; i++) {
                for (int j = i + 1; j < lst.size(); j++) {
                    dist.set(i, j, lst.get(i).getGeometry().distance(lst.get(j).getGeometry()));
                }
            }
            for (int i = 0; i < lst.size(); i++) {
                builder.append(lst.get(i).uuid()).append(' ');
                for (int j = 0; j < lst.size(); j++) {
                    builder.append(String.format("%.3f ", dist.get(i, j)));
                }
                builder.append("\n");
            }
            return builder.toString();
        }

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
}
