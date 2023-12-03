package org.xenei.robot.utils;

import java.util.Iterator;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;

import org.xenei.robot.common.Location;

public class CoordinateGraph {
    SortedSet<Arc> points;

    public static final Location MAX_COORD = new Location(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
    public static final Location MIN_COORD = new Location(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY);

    public CoordinateGraph() {
        this.points = new TreeSet<Arc>();

    }

    public boolean isEmpty() {
        return points.isEmpty();
    }

    public void add(Location locationA, Location locationB, double weight) {
        Arc arc = new Arc(locationA, locationB, weight);
        points.add(arc);
    }

    public SortedSet<Arc> search(Location location) {
        SortedSet<Arc> result = new TreeSet<>();
        points.subSet(new Arc(location, MIN_COORD, 0), new Arc(location, MAX_COORD, 0)).forEach(result::add);
        heads().stream().map(c -> get(c, location)).filter(Optional::isPresent).forEach(o -> result.add(o.get()));
        return result;
    }

    public Optional<Arc> get(Location a, Location b) {
        Arc arc = new Arc(a, b, 0);
        SortedSet<Arc> tail = points.tailSet(arc);
        if (tail.first().equals(arc)) {
            return Optional.of(tail.first());
        }
        return Optional.empty();
    }

    private SortedSet<Location> heads() {
        TreeSet<Location> result = new TreeSet<>(Location.XYCompr);
        SortedSet<Arc> tailSet = points;
        while (!tailSet.isEmpty()) {
            result.add(tailSet.first().locationA);
            tailSet = points.tailSet(new Arc(points.first().locationA, MAX_COORD, 0));
            while (tailSet.first().locationA.equals(result.last())) {
                Iterator<Arc> iter = tailSet.iterator();
                iter.next();
                tailSet = points.tailSet(iter.next());
            }
        }
        return result;
    }

    public void remove(Location location) {
        search(location).forEach(points::remove);
    }

    public boolean exists(Location location) {
        return points.tailSet(new Arc(location, MIN_COORD, 0)).first().locationA.equals(location)
                || heads().stream().map(c -> get(c, location)).filter(Optional::isPresent).findAny().isPresent();
    }

    @Override
    public String toString() {
        return stringBuilder().toString();

    }

    /**
     * Generates the map for display.
     * 
     * @param c the character to use for enabled items.
     * @return the StringBuilder.
     */
    public StringBuilder stringBuilder() {
        StringBuilder sb = new StringBuilder();
        points.stream().forEach(arc -> sb.append(arc).append("\n"));
        return sb;
    }

    // a location in the map
    public class Arc implements Comparable<Arc> {
        Location locationA;
        Location locationB;
        double weight;

        Arc(Location a, Location b, double weight) {
            if (Location.XYCompr.compare(a, b) > 0) {
                Location tmp = a;
                a = b;
                b = tmp;
            }
            this.locationA = a;
            this.locationB = b;
            this.weight = weight;
        }

        @Override
        public String toString() {
            return String.format("Arc[%s,%s : %.4f]", locationA, locationB, weight);
        }

        @Override
        public int compareTo(Arc other) {
            int result = Location.XYCompr.compare(this.locationA, other.locationA);
            return result == 0 ? Location.XYCompr.compare(this.locationB, other.locationB) : result;
        }
    }
}
