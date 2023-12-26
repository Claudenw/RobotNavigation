package org.xenei.robot.common.mapping;

import java.util.Collection;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Stream;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Polygon;
import org.xenei.robot.common.FrontsCoordinate;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.ScaleInfo;
import org.xenei.robot.common.utils.CoordUtils;
import org.xenei.robot.common.utils.GeometryUtils;

public class CoordinateMap {
    GeometryFactory geometryFactory = new GeometryFactory();

    SortedSet<Coord> points;

    final double scale;
    final ScaleInfo scaleInfo = ScaleInfo.builder().setResolution(0.5).build();

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

    public void enable(FrontsCoordinate coord, char c) {
        enable(coord.getCoordinate(), c);
    }

    public void enable(Coordinate coord, char c) {
        enable(new Coord(coord, c));
    }

    public void enable(Collection<Coordinate> location, char c) {
        location.stream().forEach(loc -> enable(loc, c));
    }

    public void enable(Coord coord) {
        points.add(coord);
    }

    public Stream<Polygon> getObstacles() {
        return points.stream().map(Coord::getPolygon);
    }

    public void disable(Coordinate location) {
        points.remove(new Coord(location, ' '));
    }

    public void disable(Collection<Coordinate> location) {
        location.stream().forEach(loc -> disable(loc));
    }

    public boolean isEnabled(FrontsCoordinate location) {
        return points.contains(new Coord(location, ' '));
    }

    public boolean isEnabled(Coordinate location) {
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
    public boolean clearView(Coordinate from, Coordinate target) {
        LineString path = geometryFactory.createLineString(new Coordinate[] { from, target });
        return getObstacles().filter(obstacle -> obstacle.isWithinDistance(path, 0.49)).findFirst().isEmpty();
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
    public Optional<Location> look(Location position, double heading, double maxRange) {
        Location pos = Location.from(position.plus(CoordUtils.fromAngle(heading, maxRange / scale)));
        // Polygon path = GeometryUtils.asPath(position.getCoordinate(),
        // pos.getCoordinate());
        LineString path = GeometryUtils.asLine(position.getCoordinate(), pos.getCoordinate());
        double distance = Double.MAX_VALUE;
        Coordinate found = null;
        for (Coord point : points) {
            if (path.intersects(point.polygon)) {
                for (Coordinate p2 : calcCoords(point)) {
                    double d = position.distance(p2);
                    if (d < distance) {
                        distance = d;
                        found = p2;
                    }
                }
            }
        }
        if (found != null) {
            return Optional.of(Location.from(scaleInfo.scale(found.getX()), scaleInfo.scale(found.getY())));
        }

        return Optional.empty();
    }

    private Coordinate[] calcCoords(Coord point) {
        int parts = (int) (scale / scaleInfo.getResolution());
        double incr = scale * scaleInfo.getResolution();
        Coordinate[] result = new Coordinate[parts * parts];
        double x = point.x + incr / 2;
        for (int i = 0; i < parts; i++) {
            double y = point.y + incr / 2;
            for (int j = 0; j < parts; j++) {
                result[i * parts + j] = new Coordinate(x, y);
                y += incr;
            }
            x += incr;
        }
        return result;
    }

    private double lineDistance(Location position, double heading, Coordinate c) {
        return Math.abs(
                Math.cos(heading) * (position.getY() - c.getY()) - Math.sin(heading) * (position.getX() - c.getX()));
    }

    // a location in the map
    public class Coord implements Comparable<Coord> {
        public final int x;
        public final int y;
        public char c;
        public final Polygon polygon;

        Coord(double x, double y, char c) {
            this.x = fitRange(Math.round(x / scale));
            this.y = fitRange(Math.round(y / scale));
            this.c = c;

            double xStart = x;
            double yStart = y;
            double xFini = x + 1;
            double yFini = y + 1;

            Coordinate[] lst = { new Coordinate(xStart, yStart), new Coordinate(xStart, yFini),
                    new Coordinate(xFini, yFini), new Coordinate(xFini, yStart), new Coordinate(xStart, yStart) };
            polygon = geometryFactory.createPolygon(lst);
        }

        public Coord(FrontsCoordinate coords, char c) {
            this(coords.getCoordinate(), c);
        }

        public Coord(Coordinate coords, char c) {
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

        public Coordinate asCoordinate() {
            return new Coordinate(x, y);
        }

        public Polygon getPolygon() {
            return polygon;
        }

        public Location asLocation() {
            return Location.from(x, y);
        }
    }
}
