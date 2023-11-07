package org.xenei.robot.utils;

import java.util.Collection;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.lang3.NotImplementedException;
import org.xenei.robot.navigation.Coordinates;

public class CoordinateMap {
    SortedSet<Coord> points;

    final double scale;

    private static int fitRange(long x) {
        return x > Integer.MAX_VALUE ? Integer.MAX_VALUE : (x < Integer.MIN_VALUE ? Integer.MIN_VALUE : (int) x);
    }

    public CoordinateMap(double scale) {
        this.points = new TreeSet<Coord>();
        this.scale = scale;
    }

    public double scale() {
        return scale;
    }

    public void enable(Coordinates location, char c) {
        enable(new Coord(location, c));
    }

    public void enable(Collection<Coordinates> location, char c) {
        location.stream().forEach(loc -> enable(loc, c));
    }

    void enable(Coord coord) {
        if (!points.add(coord)) {
            points.tailSet(coord).first().c = coord.c;
        }
    }

    public void disable(Coordinates location) {
        points.remove(new Coord(location, ' '));
    }

    public void disable(Collection<Coordinates> location) {
        location.stream().forEach(loc -> disable(loc));
    }

    public boolean isEnabled(Coordinates location) {
        return points.contains(new Coord(location, ' '));
    }

    @Override
    public String toString() {
        return stringBuilder().toString();

    }

    // a location in the map
    public class Coord implements Comparable<Coord> {
        public final int x;
        public final int y;
        public char c;

        Coord(double x, double y, char c) {
            this.x = fitRange(Math.round(x / scale));
            this.y = fitRange(Math.round(y / scale));
            this.c = c;
        }

        public Coord(Coordinates coords, char c) {
            this(coords.getX(), coords.getY(), c);
        }

        @Override
        public String toString() {
            return String.format("Coord[%s,%s]", x, y);
        }

        @Override
        public int compareTo(Coord other) {
            int result = -1 * Integer.compare(y, other.y);
            return result == 0 ? Integer.compare(x, other.x) : result;
        }
    }

    /**
     * Generates the map for display.
     * 
     * @param c the character to use for enabled items.
     * @return the StringBuilder.
     */
    public StringBuilder stringBuilder() {
        StringBuilder sb = new StringBuilder();
        int minX = points.stream().map(c -> c.x).min(Integer::compare).get();
        Coord row = points.first();
        StringBuilder rowBuilder = new StringBuilder();
        for (Coord point : points) {
            if (row.y != point.y) {
                sb.append(rowBuilder.append("\n"));
                rowBuilder = new StringBuilder();
                row = point;
            }
            int x = point.x - minX;
            if (x > -rowBuilder.length()) {
                for (int i = rowBuilder.length(); i < x; i++) {
                    rowBuilder.append(' ');
                }
            }
            rowBuilder.append(point.c);
        }
        sb.append(rowBuilder.append("\n"));
        return sb;
    }
    
    public Optional<Coordinates> look(Coordinates position, double heading, double maxRange) {
        throw new NotImplementedException();
        
//        Coordinates c = null;
//        for (int range = 1; range <= ActiveMap.maxRange; range++) {
//            c = Coordinates.fromRadians(heading, range * map.scale());
//            if (map.isEnabled(position.plus(c))) {
//                return c;
//            }
//        }
//        return Coordinates.fromRadians(heading, Double.POSITIVE_INFINITY);
    }
}
