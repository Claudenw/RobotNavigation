package org.xenei.robot.mapper.visualization;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.swing.JFrame;
import javax.swing.WindowConstants;

import org.apache.jena.rdf.model.Literal;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.mapping.Map;
import org.xenei.robot.common.mapping.Mapper;
import org.xenei.robot.common.planning.Solution;
import org.xenei.robot.common.utils.GeometryUtils;

public class MapViz implements Mapper.Visualization, Runnable {
    private final Supplier<Solution> solutionSupplier;
    private final Supplier<Position> positionSupplier;
    private final Supplier<Coordinate> targetSupplier;
    private final Map map;
    private final JTSPanel panel;
    private final int scale;
    private final int buffer;

    public MapViz(int scale, Map map, Supplier<Solution> solutionSupplier, Supplier<Position> positionSupplier,
                   Supplier<Coordinate> targetSupplier) {
        this.map = map;
        this.panel = new JTSPanel();
        this.solutionSupplier = solutionSupplier;
        this.positionSupplier = positionSupplier;
        this.targetSupplier = targetSupplier;
        this.scale = scale;
        this.buffer = (int) (map.getContext().scaleInfo.getResolution() * scale) / 2;

        JFrame frame = new JFrame("Map Visualization");
        frame.setLayout(new BorderLayout());
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        frame.add(panel, BorderLayout.CENTER);

        frame.pack();
        frame.setSize(1000, 1000);
        frame.setVisible(true);
    }

    @Override
    public void run() {
        redraw();
    }

    private AbstractDrawingCommand getPoly(Geometry geom, Color color) {
        if (geom instanceof Point) {
            return new AbstractDrawingCommand(geom, color) {
                @Override
                protected void fillGeom(Graphics g, int[] xler, int[] yler) {
                    g.fillOval(xler[0] - buffer, yler[0] - buffer, buffer * 2, buffer * 2);
                }
            };
        }
        if (geom instanceof Polygon) {
            return new AbstractDrawingCommand(geom, color) {
                @Override
                protected void fillGeom(Graphics g, int[] xler, int[] yler) {
                    g.fillPolygon(xler, yler, xler.length);
                }
            };
        }

        if (geom instanceof LineString || geom instanceof MultiLineString) {
            return new AbstractDrawingCommand(geom, color) {
                @Override
                protected void fillGeom(Graphics g, int[] xler, int[] yler) {
                    g.drawPolyline(xler, yler, xler.length);
                }
            };
        }

        return new AbstractDrawingCommand(geom, color) {
            @Override
            protected void fillGeom(Graphics g, int[] xler, int[] yler) {
                g.drawString(geom.getClass().getSimpleName(), xler[0], yler[0]);
            }
        };
    }

    @Override
    public void redraw() {
        GeometryUtils geometryUtils = map.getContext().geometryUtils;
        List<AbstractDrawingCommand> cmds = new ArrayList<>();
        List<CompletableFuture<?>> futures = new ArrayList<>();
        futures.add(map.getObstacles().thenAccept( obs -> obs.forEach(obst ->
        {
            if (obst.geom() instanceof GeometryCollection) {
                GeometryCollection gCollection = (GeometryCollection) obst.geom();

                for (int i = 0; i < gCollection.getNumGeometries(); i++) {
                    cmds.add(getPoly(gCollection.getGeometryN(i), Color.RED));
                }
            } else {
                cmds.add(getPoly(obst.geom(), Color.RED));
            }
        })));

        futures.add(map.getCoords().thenAccept( coords -> coords.forEach( mapCoord -> {
            cmds.add(getPoly(mapCoord.geometry, mapCoord.isIndirect ? Color.CYAN : Color.BLUE));
        })));

        List<Coordinate> lst = solutionSupplier.get().stream().collect(Collectors.toList());
        if (lst.size() > 1) {
            cmds.add(getPoly(geometryUtils.asPath(0.25, lst.toArray(new Coordinate[lst.size()])), Color.WHITE));
        } else if (lst.size() == 1) {
            cmds.add(getPoly(geometryUtils.asPolygon(lst.get(0), .25), Color.WHITE));
        }

        Coordinate target = targetSupplier.get();
        if (target != null) {
            cmds.add(getPoly(geometryUtils.asPolygon(target, 0.25), Color.GREEN));
        }

        Position position = positionSupplier.get();
        if (position != null) {
            cmds.add(getPoly(geometryUtils.asPolygon(position, 0.25), Color.ORANGE));
        }

        cmds.add(getPoly(geometryUtils.asPath(map.getContext().chassisInfo.radius, position.getCoordinate(), target), Color.ORANGE));

        for (CompletableFuture<?> f : futures) {
            f.join();
        }
        map.getContext().awaitQuiescence(2, TimeUnit.SECONDS);
        rescale(cmds);

        panel.clear();
        cmds.forEach(panel::addDrawCommand);
        panel.repaint();
    }

    private void rescale(List<AbstractDrawingCommand> lst) {

        double max = Integer.MIN_VALUE;
        for (AbstractDrawingCommand cmd : lst) {
            for (int i : cmd.xler) {
                double ii = Math.abs(i);
                max = ii < max ? max : ii;
            }
            for (int i : cmd.yler) {
                double ii = Math.abs(i);
                max = ii < max ? max : ii;
            }
        }
        max += buffer;
        int offset = (int) Math.max(2 * max / 700, 1);
        for (AbstractDrawingCommand cmd : lst) {
            for (int i = 0; i < cmd.xler.length; i++) {
                cmd.xler[i] += max;
                cmd.xler[i] /= offset;
            }
            for (int i = 0; i < cmd.yler.length; i++) {
                cmd.yler[i] += max;
                cmd.yler[i] /= offset;
            }
        }
    }

    /**
     * 
     * @see <a href="https://www.smartycoder.com">smartycpder</a>
     *
     */
    public abstract class AbstractDrawingCommand implements DrawingCommand {
        int[] xler;
        int[] yler;
        private Color color;

        AbstractDrawingCommand(Geometry geom, Color color) {
            this.color = color;
            Coordinate[] coords = geom.getCoordinates();
            xler = new int[coords.length];
            yler = new int[coords.length];

            for (int i = 0; i < coords.length; i++) {
                xler[i] = (int) Math.round(coords[i].getX() * scale);
                yler[i] = -1 * (int) Math.round(coords[i].getY() * scale);
            }
        }

        @Override
        public void doDrawing(Graphics g) {
            g.setColor(color);
            fillGeom(g, xler, yler);
        }

        abstract protected void fillGeom(Graphics g, int[] xler, int[] yler);
    }

    public class DrawingCommandCollection implements DrawingCommand {
        List<DrawingCommand> cmds = new ArrayList<>();

        @Override
        public void doDrawing(Graphics g) {
            cmds.forEach(dc -> dc.doDrawing(g));
        }

    }
}
