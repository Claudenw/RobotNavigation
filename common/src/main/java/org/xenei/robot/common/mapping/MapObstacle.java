package org.xenei.robot.common.mapping;

import org.locationtech.jts.geom.Geometry;
import org.xenei.robot.common.Obstacle;


import java.util.UUID;

public final class MapObstacle implements Obstacle, MapObject {
    private final Map onMap;
    private final UUID uuid;
    private final Geometry geometry;

    MapObstacle(Map map, Geometry geometry, UUID uuid) {
        this.onMap = map;
        this.uuid = uuid;
        this.geometry = geometry;
    }

    MapObstacle(Map map, Geometry geometry) {
        this(map, geometry, UUID.randomUUID());
    }

    public final UUID uuid() {
        return uuid;
    }

    @Override
    public Geometry getGeometry() {
        return geometry;
    }

    @Override
    public Map getMap() {
        return onMap;
    }

    @Override
    public String toString() {
        return ObstacleUtils.toString(this);
    }

    @Override
    public int hashCode() {
        return ObstacleUtils.hashCode(this);
    }

    @Override
    public boolean equals(Object obj) {
        return ObstacleUtils.equals(this, obj);
    }
}
