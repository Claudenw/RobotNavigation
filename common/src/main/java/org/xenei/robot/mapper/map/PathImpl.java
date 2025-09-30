package org.xenei.robot.mapper.map;

import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.geosparql.implementation.vocabulary.Geo;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.vocabulary.RDF;
import org.locationtech.jts.geom.Coordinate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.common.FrontsCoordinate;
import org.xenei.robot.common.mapping.Map;
import org.xenei.robot.common.utils.RobutContext;
import org.xenei.robot.mapper.rdf.Namespace;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PathImpl implements Map.Path {
    private static final Logger LOG = LoggerFactory.getLogger(PathImpl.class);
    private final MapImpl map;
    private final List<MapLocation> path;
    private final Literal geometry;

    PathImpl(MapImpl map, final Stream<? extends FrontsCoordinate> coords) {
        this.map = map;
        path = coords.map(map::asMapCoordinate).filter(Objects::nonNull).collect(Collectors.toList());
        Coordinate[] points = path.stream().map(MapLocation::getCoordinate).toArray(Coordinate[]::new);
        geometry = map.getContext().graphGeomFactory.asWKTString(points);
        LOG.debug("Path <{} {}>", points[0], points[points.length - 1]);
    }

    CompletableFuture<PathImpl> update(final Resource model) {
        Node tn = ResourceFactory.createResource().asNode();
        List<Triple> triples = new ArrayList<>();
        triples.add(Triple.create(tn, RDF.type.asNode(), Namespace.Path.asNode()));
        triples.add(Triple.create(tn, Geo.AS_WKT_PROP.asNode(), geometry.asNode()));
        return map.doUpdate(new UpdateBuilder().addInsert(model, triples).buildRequest()).thenApply(n -> this);
    }

    public static boolean hasPath(MapLocation a, MapLocation b) {
        Var wkt = Var.alloc("wkt");
        MapImpl map = a.getMap();
        RobutContext ctxt = map.getContext();
        WhereBuilder wb = new WhereBuilder().addWhere(Namespace.s, RDF.type, Namespace.Path) //
                .addWhere(Namespace.s, Geo.AS_WKT_NODE, wkt)
                .addFilter(ctxt.graphGeomFactory.isNearby(map.exprF, wkt, a.getWkt(), ctxt.scaleInfo.getResolution()))
                .addFilter(ctxt.graphGeomFactory.isNearby(map.exprF, wkt, b.getWkt(), ctxt.scaleInfo.getResolution()));

        AskBuilder ask = new AskBuilder().addGraph(Namespace.UnionModel, wb);
        return map.ask(ask);
    }

}
