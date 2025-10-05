package org.xenei.robot.mapper.map;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.apache.jena.geosparql.implementation.vocabulary.Geo;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.locationtech.jts.geom.Geometry;
import org.xenei.robot.common.Obstacle;
import org.xenei.robot.common.ObstacleI;
import org.xenei.robot.common.mapping.Map;
import org.xenei.robot.common.utils.RobutContext;
import org.xenei.robot.mapper.rdf.Namespace;

/**
 * An RDF map construct containing a shape, and RDF Literal for the shape, and
 * the RDF resource.
 */
public class MapObstacle extends Obstacle implements Map.Obstacle {
    private final Literal wkt;
    private CompletableFuture<?> future;

    private static UUID parseUUID(Resource rdf) {
        return UUID.fromString(rdf.getURI().substring("urn:uuid:".length()));
    }

    private static ObstacleI asObstacle(RobutContext ctxt, Resource rdf, Literal wkt) {
        return new ObstacleI() {
            @Override
            public UUID uuid() {
                return parseUUID(rdf);
            }
            @Override
            public Geometry getGeometry() {
                return ctxt.graphGeomFactory.fromWkt(wkt);
            }
        };
    }

    MapObstacle(RobutContext ctxt, ObstacleI obstacle) {
        super(obstacle);
        this.wkt = ctxt.graphGeomFactory.asWKT(getGeometry());
    }

    MapObstacle(RobutContext ctxt, Resource rdf, Literal wkt) {
        this(ctxt, asObstacle(ctxt, rdf, wkt));
    }

    MapObstacle(RobutContext ctxt, Geometry geometry) {
        super(geometry);
        this.wkt = ctxt.graphGeomFactory.asWKT(geometry);
    }

    public Resource rdf() {
        return ResourceFactory.createResource("urn:uuid:" + uuid().toString());
    }

    public Resource in(Model model) {
        Resource result = model.createResource(rdf().getURI(), Namespace.Obst);
        result.addLiteral(Geo.AS_WKT_PROP, wkt());
        return result;
    }

    public Literal wkt() {
        return wkt;
    }
}
