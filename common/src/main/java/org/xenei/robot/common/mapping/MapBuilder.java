package org.xenei.robot.common.mapping;

import java.util.UUID;

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;

public class MapBuilder {

    private final Map map;
    

    public enum Type {
        Obstacle, Path
    };

    public MapBuilder(Map map) {
        this.map = map;
    }

    public MapBuilder set(int x, int y) {
        map.addObstacle(new ObstacleImpl(new Coordinate(x, y)));
        return this;
    }

    public MapBuilder setY(int x, int start, int end, Type type) {
        return set(new Coordinate(x, start), new Coordinate(x, end), type);
    }

    private MapBuilder set(Coordinate first, Coordinate last, Type type) {
        switch (type) {

        case Obstacle:
            map.addObstacle(new ObstacleImpl(first, last));
            break;
        case Path:
            map.addPath(first, last);
        }
        return this;
    }

    public MapBuilder setX(int y, int start, int end, Type type) {
        return set(new Coordinate(start, y), new Coordinate(end, y), type);
    }

    public MapBuilder border(int x, int y, int xLength, int yLength) {
        setY(x, y, y + yLength - 1, Type.Obstacle);
        setY(x + xLength - 1, y, y + yLength - 1, Type.Obstacle);
        setX(y, x, x + xLength - 1, Type.Obstacle);
        setX(y + yLength - 1, x, x + xLength - 1, Type.Obstacle);
        return this;
    }

    public Map build() {
        return map;
    }

    class ObstacleImpl implements Obstacle {

        final UUID uuid;
        final Geometry geom;

        ObstacleImpl(Coordinate c) {
            uuid = UUID.randomUUID();
            geom = map.getContext().geometryUtils.asPoint(c);
        }

        ObstacleImpl(Coordinate start, Coordinate end) {
            double d = start.distance(end);
            int parts = (int) (d / map.getContext().scaleInfo.getHalfResolution());
            double xIncr = (end.x - start.x) / (parts + 1);
            double yIncr = (end.y - start.y) / (parts + 1);
            Coordinate[] part = new Coordinate[parts + 1];
            part[0] = start;
            for (int i = 1; i < parts; i++) {
                part[i] = new Coordinate(part[i - 1].x + xIncr, part[i - 1].y + yIncr);
            }
            part[parts] = end;
            geom = map.getContext().geometryUtils.asLine(part);
            uuid = UUID.randomUUID();
        }

        @Override
        public Literal wkt() {
            return map.getContext().graphGeomFactory.asWKT(geom);
        }

        @Override
        public Geometry geom() {
            return geom;
        }

        @Override
        public UUID uuid() {
            return uuid;
        }

        @Override
        public Resource rdf() {
            return ResourceFactory.createResource("urn:uuid:" + uuid().toString());
        }

    }
}
