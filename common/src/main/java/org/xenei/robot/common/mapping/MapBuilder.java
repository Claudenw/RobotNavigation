package org.xenei.robot.common.mapping;

import java.util.concurrent.TimeUnit;

import org.locationtech.jts.geom.Coordinate;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.Obstacle;

public class MapBuilder {

    private final Map<?, ?, ?> map;

    public enum Type {
        Obstacle, Path
    };

    public MapBuilder(Map<?, ?, ?> map) {
        this.map = map;
    }

    public MapBuilder set(int x, int y) {
        map.createObstacle(new Coordinate(x, y));
        return this;
    }

    public MapBuilder setY(int x, int start, int end, Type type) {
        return set(new Coordinate(x, start), new Coordinate(x, end), type);
    }

    private MapBuilder set(Coordinate first, Coordinate last, Type type) {
        switch (type) {
            case Obstacle :
                new Obstacle(map.getContext().geometryUtils.asLine(first, last));
                break;
            case Path :
                map.addPath(new Location(first), new Location(last));
                break;
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

    public Map<?, ?, ?> build() {
        map.getContext().awaitQuiescence(30, TimeUnit.SECONDS);
        return map;
    }
}
