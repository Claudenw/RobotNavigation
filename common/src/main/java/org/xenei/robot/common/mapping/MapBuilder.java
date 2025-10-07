package org.xenei.robot.common.mapping;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineSegment;
import org.locationtech.jts.geom.LineString;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.Obstacle;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.utils.CoordUtils;

public class MapBuilder {

    private final Map<?, ?, ?> map;
    private final List<CompletableFuture<?>> futures = new ArrayList<>();

    public enum Type {
        Obstacle, Path
    };

    public MapBuilder(Map<?, ?, ?> map) {
        this.map = map;
    }

    public MapBuilder set(int x, int y) {
        futures.add(map.createObstacle(new Coordinate(x, y)));
        return this;
    }

    public MapBuilder setY(int x, int start, int end, Type type) {
        return set(new Coordinate(x, start), new Coordinate(x, end), type);
    }

    private MapBuilder set(Coordinate first, Coordinate last, Type type) {
        switch (type) {
            case Obstacle :
                futures.add(map.createObstacle(first, last));
                break;
            case Path :
                futures.add(map.addPath(new Location(first), new Location(last)));
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
        futures.forEach(CompletableFuture::join);
        return map;
    }
}
