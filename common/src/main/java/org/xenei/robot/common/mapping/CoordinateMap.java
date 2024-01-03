package org.xenei.robot.common.mapping;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.xenei.robot.common.FrontsCoordinate;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.ScaleInfo;
import org.xenei.robot.common.UnmodifiableCoordinate;
import org.xenei.robot.common.utils.CoordUtils;
import org.xenei.robot.common.utils.DoubleUtils;
import org.xenei.robot.common.utils.GeometryUtils;

public class CoordinateMap {
    GeometryFactory geometryFactory = new GeometryFactory();

    SortedSet<Coord> points;

    final double scale;
    final ScaleInfo scaleInfo = ScaleInfo.builder().setResolution(0.5).build();

    private static double fitRange(double x) {
        return x > Integer.MAX_VALUE ? Integer.MAX_VALUE : (x < Integer.MIN_VALUE ? Integer.MIN_VALUE : x);
    }

    public CoordinateMap(double scale) {
        this.points = new TreeSet<Coord>();
        this.scale = scale;
    }

    public double scale() {
        return scale;
    }
    
    public ScaleInfo scaleInfo() {
        return scaleInfo;
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

    int asInt(double d) { return (int)Math.round(d); }
    /**
     * Generates the map for display.
     * 
     * @param c the character to use for enabled items.
     * @return the StringBuilder.
     */
    public StringBuilder stringBuilder() {
        StringBuilder sb = new StringBuilder();
        int minX = points.stream().map(c -> asInt(c.getX())).min(Integer::compare).get();
        Coord row = points.first();
        int rowY = asInt(row.getY());
        StringBuilder rowBuilder = new StringBuilder();
        for (Coord point : points) {
            if (rowY != asInt(point.getY())) {
                sb.append(rowBuilder.append("\n"));
                rowBuilder = new StringBuilder();
                row = point;
                rowY = asInt(row.getY());
            }
            int x = asInt(point.getX()) - minX;
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
     * The positions on the map are calculated as x+.5 y+.5 to center the position into a square at grid
     * location x,y
     * 
     * @param position the position to start from
     * @param heading the heading to check.
     * @param maxRange the maximum range to check.
     * @return the natural (not quantized) position of the obstacle.
     */
    public Optional<Location> look(Location position, double heading, double maxRange) {
        Location pos = Location.from(position.plus(CoordUtils.fromAngle(heading, maxRange / scale)));
        Coordinate found[] = { null };
        
        Consumer<Coordinate> filter = new Consumer<Coordinate>() {
            double distance = Double.MAX_VALUE;
            @Override
            public void accept(Coordinate t) {
                double d = position.distance(t);
                if (d < distance) {
                    distance = d;
                    found[0] = t;
                }
            }
        };

        
        LineString path = GeometryUtils.asLine(position.getCoordinate(), pos.getCoordinate());
        
        for (Coord point : points) {
            if (path.intersects(point.polygon)) {
                // find face
                Geometry g = path.intersection(point.polygon);
                Arrays.stream(g.getCoordinates()).forEach(filter::accept);
            }
        }
        if (found[0] != null) {
            return Optional.of(Location.from(scaleInfo.scale(found[0].getX()), scaleInfo.scale(found[0].getY())));
        }

        return Optional.empty();
    }

    private double lineDistance(Location position, double heading, Coordinate c) {
        return Math.abs(
                Math.cos(heading) * (position.getY() - c.getY()) - Math.sin(heading) * (position.getX() - c.getX()));
    }

    // a location in the map
    public class Coord implements Comparable<Coord>, FrontsCoordinate {
        public final UnmodifiableCoordinate coordinate;
        public char c;
        public final Polygon polygon;

        Coord(double x, double y, char c) {
            coordinate = UnmodifiableCoordinate.make(new Coordinate(fitRange(x / scale),fitRange(y / scale)));
            this.c = c;
            polygon = GeometryUtils.asPolygon( coordinate, scale/2, 4 );
        }

        public Coord(FrontsCoordinate coords, char c) {
            this(coords.getCoordinate(), c);
        }

        public Coord(Coordinate coords, char c) {
            this(coords.getX(), coords.getY(), c);
        }

        @Override
        public String toString() {
            return String.format("Coord[%s,%s]", getX(), getY());
        }

        @Override
        public int compareTo(Coord other) {
            int result = -1 * Double.compare(getY(), other.getY());
            return result == 0 ? Double.compare(getX(), other.getX()) : result;
        }

        public Polygon getPolygon() {
            return polygon;
        }

        public Location asLocation() {
            return Location.from(coordinate);
        }

        @Override
        public UnmodifiableCoordinate getCoordinate() {
            return coordinate;
        }
    }
}
