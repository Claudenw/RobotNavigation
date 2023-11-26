package org.xenei.robot.common;

import java.util.Collection;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Stream;

//import org.xenei.robot.planner.PlannerMap;

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

    public void enable(Coord coord) {
        if (!points.add(coord)) {
            points.tailSet(coord).first().c = coord.c;
        }
    }

    public Stream<Point> getObstacles() {
        return points.stream().map(Coord::asPoint);
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

    /**
     * For testing.
     * 
     * @param from
     * @param target
     * @return true if there are no obstructions.
     */
    public boolean clearView(Coordinates from, Coordinates target) {
        Position position = new Position(from, from.angleTo(target));
        return getObstacles().filter(obstacle -> !position.hasClearView(target, Coordinates.fromXY(obstacle)))
                .findFirst().isEmpty();
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

    /**
     * Scan the map for an entry from the position on the heading upto the max
     * range.
     * 
     * @param position the position to start from
     * @param heading the heading to check.
     * @param maxRange the maximum range to check.
     * @return the natural (not quantized) position of the obstacle.
     */
    public Optional<Coordinates> look(Coordinates position, double heading, double maxRange) {
        int cordRange = fitRange(Math.round(maxRange / scale));

        for (int i = 0; i < cordRange; i++) {
            Coordinates pos = position.plus(Coordinates.fromAngle(heading, AngleUnits.RADIANS, i * scale));
            if (points.contains(new Coord(pos, ' '))) {
                return Optional.of(pos);
            }
        }
        return Optional.empty();
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

        public Point asPoint() {
            return new Point(x, y);
        }
    }
}
