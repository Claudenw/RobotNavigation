package org.xenei.robot.common;

import org.locationtech.jts.geom.Geometry;

import java.util.UUID;

public class Obstacle implements ObstacleI {
    private final UUID uuid;
    private final Geometry geom;

    protected Obstacle(ObstacleI other) {
        this.uuid = other.uuid();
        this.geom = other.getGeometry();
    }

    public Obstacle(Geometry geom) {
        this.geom = geom;
        uuid = UUID.randomUUID();
    }

    // public Obstacle(RobutContext ctxt, Coordinate c) {
    // this(ctxt.geometryUtils.asPoint(c));
    // }
    //
    // public Obstacle(RobutContext ctxt, Coordinate start, Coordinate end) {
    // double d = start.distance(end);
    // int parts = (int) (d / (ctxt.scaleInfo.getResolution() / 2));
    // double xIncr = (end.x - start.x) / (parts + 1);
    // double yIncr = (end.y - start.y) / (parts + 1);
    // Coordinate[] part = new Coordinate[parts + 1];
    // part[0] = start;
    // for (int i = 1; i < parts; i++) {
    // part[i] = new Coordinate(part[i - 1].x + xIncr, part[i - 1].y + yIncr);
    // }
    // part[parts] = end;
    // geom = ctxt.geometryUtils.asLine(part);
    // uuid = UUID.randomUUID();
    // }

    @Override
    public final int hashCode() {
        return GeometricObject.hashCode(this);
    }

    @Override
    public final boolean equals(Object obj) {
        return obj instanceof Obstacle && GeometricObject.equals(this, obj);
    }

    @Override
    public final Geometry getGeometry() {
        return geom;
    }

    @Override
    public final UUID uuid() {
        return uuid;
    }

    @Override
    public String toString() {
        return geom.toString();
    }
}
