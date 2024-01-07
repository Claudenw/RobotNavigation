package org.xenei.robot.common.mapping;

import org.locationtech.jts.geom.Coordinate;
import org.xenei.robot.common.mapping.CoordinateMap.Coord;

public class CoordinateMapBuilder {

    private CoordinateMap map;
    private double offset;

    public CoordinateMapBuilder(double scale) {
        map = new CoordinateMap(scale);
        offset = scale/2;
    }

    public CoordinateMapBuilder set(int x, int y, char c) {
        map.enable(new Coordinate(x+offset, y+offset), c);
        return this;
    }

    public CoordinateMapBuilder setY(int x, int start, int end, char c) {
        for (int y = start; y <= end; y++) {
            set(x, y, c);
        }
        return this;
    }

    public CoordinateMapBuilder setX(int y, int start, int end, char c) {
        for (int x = start; x <= end; x++) {
            set(x, y, c);
        }
        return this;
    }

    public CoordinateMapBuilder clear(int x, int y) {
        map.disable(new Coordinate(x, y));
        return this;
    }

    public CoordinateMapBuilder border(int x, int y, int xLength, int yLength, char c) {
        setY(x, y, y + yLength - 1, c);
        setY(x + xLength - 1, y, y + yLength - 1, c);
        setX(y, x, x + xLength - 1, c);
        setX(y + yLength - 1, x, x + xLength - 1, c);
        return this;
    }

    public CoordinateMapBuilder merge(CoordinateMap other) {
        for (Coord coord : other.points) {
            map.enable(coord);
        }
        return this;
    }

    public CoordinateMap build() {
        return map;
    }

}
