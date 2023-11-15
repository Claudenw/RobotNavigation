package org.xenei.robot.utils;

import org.xenei.robot.navigation.Coordinates;
import org.xenei.robot.utils.CoordinateMap.Coord;

public class CoordinateMapBuilder {

    private CoordinateMap map;

    public CoordinateMapBuilder(double scale) {
        map = new CoordinateMap(scale);
    }

    public CoordinateMapBuilder set(int x, int y, char c) {
        map.enable(Coordinates.fromXY(x, y), c);
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
        Coordinates c = Coordinates.fromXY(x, y);
        map.disable(c);
        return this;
    }
    
    public CoordinateMapBuilder border(int x, int y, int xLength, int yLength, char c) {
        setY(x, y, y+yLength-1, c);
        setY(x+xLength-1, y, y+yLength-1, c);
        setX(y, x, x+xLength-1, c);
        setX(y+yLength-1, x, x+xLength-1, c);
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
