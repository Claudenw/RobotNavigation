package org.xenei.robot.mapper.visualization;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.xenei.robot.common.FrontsCoordinate;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.UnmodifiableCoordinate;
import org.xenei.robot.common.mapping.Map;
import org.xenei.robot.common.planning.Solution;
import org.xenei.robot.common.utils.GeometryUtils;

public class TextViz implements Map.Visualization {
    final Map map;
    final double scale;
    final Supplier<Solution> solutionSupplier;
    final Supplier<Position> positionSupplier;
    final Supplier<Coordinate> targetSupplier;

    private static final char OBSTACLE = '#';
    private static final char TARGET = 't';
    private static final char COORD_INDIRECT = 0xA7;
    private static final char COORD_DIRECT = '*';
    private static final char PATH = '+';
    private static final char POSITION = '@';

    private static double fitRange(double x) {
        return x > Integer.MAX_VALUE ? Integer.MAX_VALUE : (x < Integer.MIN_VALUE ? Integer.MIN_VALUE : x);
    }

    public TextViz(double scale, Map map, Supplier<Solution> solutionSupplier, Supplier<Position> positionSupplier,
                   Supplier<Coordinate> targetSupplier) {
        this.scale = scale;
        this.map = map;
        this.positionSupplier = positionSupplier;
        this.solutionSupplier = solutionSupplier;
        this.targetSupplier = targetSupplier;
    }

    public double scale() {
        return scale;
    }

    private int asInt(double d) {
        return (int) Math.round(fitRange(d));
    }

    /**
     * Generates the map for display.
     * 
     * @param points the set of points.
     * @return the StringBuilder.
     */
    private StringBuilder stringBuilder(SortedSet<Coord> points) {
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

    public void addGeom(SortedSet<Coord> points, Geometry geom, char c) {
        if (geom instanceof GeometryCollection) {
            GeometryCollection gCollection = (GeometryCollection) geom;

            for (int i = 0; i < gCollection.getNumGeometries(); i++) {
                addGeom(points, gCollection.getGeometryN(i), c);
            }
        } else {
            Arrays.stream(geom.getCoordinates()).forEach(coord -> {
                Coord newCoord = new Coord(coord, c);

                if (points.contains(newCoord)) {
                    points.tailSet(newCoord).first().c = c;
                } else {
                    points.add(newCoord);
                }
            });
        }
    }

    @Override
    public void redraw() {
        GeometryUtils geometryUtils = map.getContext().geometryUtils;
        SortedSet<Coord> points = new TreeSet<>();
        List<CompletableFuture<?>> futures = new ArrayList<>();
        futures.add(map.getObstacles().thenAccept(s -> s.forEach(o -> addGeom(points, o.geom(), OBSTACLE))));
        futures.add(map.getCoords().thenAccept( mc -> mc.forEach(coord -> addGeom(points, coord.geometry, coord.isIndirect ? COORD_INDIRECT : COORD_DIRECT))));
        List<Coordinate> lst = solutionSupplier.get().stream().toList();
        for (CompletableFuture<?> future : futures) {
            future.join();
        }
        if (lst.size() > 1) {
            addGeom(points, geometryUtils.asPath(0.25, lst.toArray(new Coordinate[lst.size()])), PATH);
        } else if (lst.size() == 1) {
            addGeom(points, geometryUtils.asPoint(lst.get(0)), PATH);
        }

        Coordinate target = targetSupplier.get();
        if (target != null) {
            addGeom(points, geometryUtils.asPoint(target), TARGET);
        }

        Position position = positionSupplier.get();
        if (position != null) {
            addGeom(points, geometryUtils.asPoint(position), POSITION);
        }
        output(stringBuilder(points));
    }

    protected void output(StringBuilder sb) {
        System.out.println();
        System.out.println(sb);
    }

    // a location in the map
    class Coord implements Comparable<Coord>, FrontsCoordinate {
        public final UnmodifiableCoordinate coordinate;
        public char c;

        Coord(double x, double y, char c) {
            coordinate = UnmodifiableCoordinate.make(new Coordinate(asInt(x / scale), asInt(y / scale)));
            this.c = c;
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

        @Override
        public UnmodifiableCoordinate getCoordinate() {
            return coordinate;
        }
    }
}
