package org.xenei.robot.utils;

import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;


import org.xenei.robot.navigation.Coordinates;

public class CoordinateGraph {
    SortedSet<Arc> points;

    
    public static final Coordinates MAX_COORD = Coordinates.fromXY(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
    public static final Coordinates MIN_COORD = Coordinates.fromXY(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY);

    public CoordinateGraph() {
        this.points = new TreeSet<Arc>();

    }

    public boolean isEmpty() {
        return points.isEmpty();
    }
    
    public void add(Coordinates locationA, Coordinates locationB, double weight) {
        Arc arc = new Arc(locationA, locationB, weight);
        points.add(arc);
    }

    
    public SortedSet<Arc> search(Coordinates location) {
        SortedSet<Arc> result = new TreeSet<>(); 
        points.subSet(new Arc(location, MIN_COORD, 0), new Arc(location, MAX_COORD, 0)).forEach(result::add);
        heads().stream().map( c-> get(c, location)).filter(Optional::isPresent).forEach(o -> result.add(o.get()));
        return result;
    }
    
    public Optional<Arc> get(Coordinates a, Coordinates b) {
        Arc arc = new Arc(a, b, 0);
        SortedSet<Arc> tail = points.tailSet(arc);
        if (tail.first().equals(arc)) {
            return Optional.of(tail.first());
        }
        return Optional.empty();
    }
    
    private SortedSet<Coordinates> heads() {
        TreeSet<Coordinates> result = new TreeSet<>(Coordinates.XYCompr);
        SortedSet<Arc> tailSet = points;
        while (!tailSet.isEmpty()) {
            result.add(tailSet.first().locationA);
            tailSet = points.tailSet(new Arc( points.first().locationA, MAX_COORD, 0));
            while (tailSet.first().locationA.equals( result.last())) {
                Iterator<Arc> iter = tailSet.iterator();
                iter.next();
                tailSet = points.tailSet(iter.next());
            }
        }
        return result;
    }

    public void remove(Coordinates location) {
        search(location).forEach(points::remove);
    }

    public boolean exists(Coordinates location) {
        return points.tailSet(new Arc(location, MIN_COORD, 0)).first().locationA.equals(location)
                    || heads().stream().map( c-> get(c, location)).filter(Optional::isPresent).findAny().isPresent();
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
        points.stream().forEach( arc -> sb.append(arc).append("\n"));
        return sb;
    }

    // a location in the map
    public class Arc implements Comparable<Arc> {
        Coordinates locationA;
        Coordinates locationB;
        double weight;

        Arc(Coordinates a, Coordinates b, double weight) {
            Coordinates qa = a.quantize();
            Coordinates qb = b.quantize();
            if (Coordinates.XYCompr.compare(a,b) > 0) {
                Coordinates tmp = qa;
                qa = qb;
                qb = tmp;
            }
            this.locationA = qa;
            this.locationB = qb;
            this.weight = weight;       
        }

        @Override
        public String toString() {
            return String.format("Arc[%s,%s : %.4f]", locationA, locationB, weight);
        }

        @Override
        public int compareTo(Arc other) {
            int result = Coordinates.XYCompr.compare(this.locationA, other.locationA);
            return result == 0 ? Coordinates.XYCompr.compare(this.locationB, other.locationB) : result;
        }
    }
}
