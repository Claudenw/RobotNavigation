package org.xenei.robot.mapper.visualization;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import io.nats.client.Options;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.UnmodifiableCoordinate;

public class TextViz extends AbstractRemoteVisualization {
    private static final Logger LOG = LoggerFactory.getLogger(TextViz.class);
    private Duration delay;
    private final double scale;
    private final Appendable output;
    private long lastExecution = 0;

    private static double fitRange(double x) {
        return x > Integer.MAX_VALUE ? Integer.MAX_VALUE : (x < Integer.MIN_VALUE ? Integer.MIN_VALUE : x);
    }

    public TextViz(double scale, Options connectionOptions, final String remoteTopic, Appendable output) {
        super(connectionOptions, remoteTopic);
        this.scale = scale;
        this.output = output;
        this.delay = Duration.ofSeconds(1);
    }

    public void setDelay(Duration delay) {
        this.delay = delay;
    }

    private char mapChar(RemoteVisualization.ObjectType objType) {
        return switch (objType) {
            case Location -> '*';
            case IndirectLocation -> 0xA7;
            case Obstacle -> '#';
            case Solution -> '+';
            case Target -> 't';
            case Position -> '@';
            case Path -> '+';
        };
    }

    private int sortOrder(RemoteVisualization.ObjectType objType) {
        return switch (objType) {
            case Location -> 2;
            case IndirectLocation -> 3;
            case Obstacle -> 1;
            case Solution -> 5;
            case Target -> 6;
            case Position -> 7;
            case Path -> 4;
        };
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
     * @param points
     *            the set of points.
     */
    private void buildOutput(SortedSet<Coord> points) {
        int minX = points.stream().map(c -> asInt(c.getX())).min(Integer::compare).orElse(1);
        Coord row = points.first();
        int rowY = asInt(row.getY());
        try {
            StringBuilder rowBuilder = new StringBuilder();
            for (Coord point : points) {
                if (rowY != asInt(point.getY())) {
                    output.append(rowBuilder.append(System.lineSeparator()));
                    rowBuilder = new StringBuilder();
                    row = point;
                    rowY = asInt(row.getY());
                }
                int x = asInt(point.getX()) - minX;
                if (x > -rowBuilder.length()) {
                    rowBuilder.append(" ".repeat(Math.max(0, x - rowBuilder.length())));
                }
                rowBuilder.append(point.c);
            }
            output.append(rowBuilder.append(System.lineSeparator()));
        } catch (IOException e) {
            LOG.error("Unable to append to output: {}", e.getMessage(), e);
        }
    }

    void addGeom(SortedSet<Coord> points, RemoteVisualization.DrawingCommand cmd) {
        if (cmd.geometry() instanceof GeometryCollection gCollection) {
            for (int i = 0; i < gCollection.getNumGeometries(); i++) {
                addGeom(points, new RemoteVisualization.DrawingCommand(gCollection.getGeometryN(i), cmd.type()));
            }
        } else {
            Arrays.stream(cmd.geometry().getCoordinates()).forEach(coord -> {
                Coord newCoord = new Coord(coord, mapChar(cmd.type()));
                if (points.contains(newCoord)) {
                    points.tailSet(newCoord).first().c = newCoord.c;
                } else {
                    points.add(newCoord);
                }
            });
        }
    }


    protected void draw(final List<RemoteVisualization.DrawingCommand> cmds) {
        if (lastExecution < System.currentTimeMillis() - delay.toMillis()) {
            final SortedSet<Coord> points = new TreeSet<>();
            cmds.sort((o1, o2) -> Integer.compare(sortOrder(o1.type()), sortOrder(o2.type())));
            for (RemoteVisualization.DrawingCommand cmd : cmds) {
                addGeom(points, cmd);
            }
            buildOutput(points);
            lastExecution = System.currentTimeMillis();
        }
    }

    // a location in the map
    class Coord implements Comparable<Coord>, Location {
        public final UnmodifiableCoordinate coordinate;
        public char c;

        Coord(double x, double y, char c) {
            coordinate = UnmodifiableCoordinate.make(new Coordinate(asInt(x / scale), asInt(y / scale)));
            this.c = c;
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
