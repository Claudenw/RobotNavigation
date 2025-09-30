package org.xenei.robot.mapper.visualization;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import javax.swing.JFrame;
import javax.swing.WindowConstants;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.xenei.robot.common.FrontsCoordinate;
import org.xenei.robot.common.PositionI;
import org.xenei.robot.common.mapping.Map;
import org.xenei.robot.common.planning.Solution;

public class MapViz implements Map.Visualization {
    private final Supplier<Solution> solutionSupplier;
    private final Supplier<PositionI<?, ?>> positionSupplier;
    private final Supplier<FrontsCoordinate> targetSupplier;
    private final Map<?, ?, ?> map;
    private final JTSPanel panel;
    private final int scale;
    private final int buffer;
    private final VizLib vizLib;

    public MapViz(int scale, Map.VisualizationInitializer initializer) {
        this(scale, initializer.map(), initializer.solutionSupplier(), initializer.positionSupplier(),
                initializer.targetSupplier());
    }

    public MapViz(int scale, Map<?, ?, ?> map, Supplier<Solution> solutionSupplier,
            Supplier<PositionI<?, ?>> positionSupplier, Supplier<FrontsCoordinate> targetSupplier) {
        this.map = map;
        this.panel = new JTSPanel();
        this.solutionSupplier = solutionSupplier;
        this.positionSupplier = positionSupplier;
        this.targetSupplier = targetSupplier;
        this.scale = scale;
        this.buffer = (int) (map.getContext().scaleInfo.getResolution() * scale) / 2;
        this.vizLib = new VizLib();

        JFrame frame = new JFrame("Map Visualization");
        frame.setLayout(new BorderLayout());
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        frame.add(panel, BorderLayout.CENTER);

        frame.pack();
        frame.setSize(1000, 1000);
        frame.setVisible(true);
    }

    @Override
    public void redraw() {
        List<MapVizDrawingCommand> cmds = vizLib.draw(map, solutionSupplier, positionSupplier, targetSupplier);
        map.getContext().awaitQuiescence(2, TimeUnit.SECONDS);
        rescale(cmds);

        panel.clear();
        cmds.forEach(panel::addDrawCommand);
        panel.repaint();
    }

    private void rescale(List<MapVizDrawingCommand> lst) {
        double max = Integer.MIN_VALUE;
        for (MapVizDrawingCommand cmd : lst) {
            for (int i : cmd.xler) {
                double ii = Math.abs(i);
                max = Math.max(ii, max);
            }
            for (int i : cmd.yler) {
                double ii = Math.abs(i);
                max = Math.max(ii, max);
            }
        }
        max += buffer;
        int offset = (int) Math.max(2 * max / 700, 1);
        for (MapVizDrawingCommand cmd : lst) {
            for (int i = 0; i < cmd.xler.length; i++) {
                cmd.xler[i] += (int) max;
                cmd.xler[i] /= offset;
            }
            for (int i = 0; i < cmd.yler.length; i++) {
                cmd.yler[i] += (int) max;
                cmd.yler[i] /= offset;
            }
        }
    }

    private class VizLib extends VisualizationLibrary<MapVizDrawingCommand> {

        public VizLib() {
            super(map.getContext().geometryUtils);
        }

        protected MapVizDrawingCommand drawPoint(Point geom, Color color) {
            return new MapVizDrawingCommand(geom, color) {
                @Override
                protected void fillGeom(Graphics g, int[] xler, int[] yler) {
                    g.fillOval(xler[0] - buffer, yler[0] - buffer, buffer * 2, buffer * 2);
                }
            };
        }

        protected MapVizDrawingCommand drawPolygon(Polygon geom, Color color) {
            return new MapVizDrawingCommand(geom, color) {
                @Override
                protected void fillGeom(Graphics g, int[] xler, int[] yler) {
                    g.fillPolygon(xler, yler, xler.length);
                }
            };
        }

        protected MapVizDrawingCommand drawLine(Geometry geom, Color color) {
            return new MapVizDrawingCommand(geom, color) {
                @Override
                protected void fillGeom(Graphics g, int[] xler, int[] yler) {
                    g.drawPolyline(xler, yler, xler.length);
                }
            };
        }

        protected MapVizDrawingCommand drawString(Geometry geom, Color color) {
            return new MapVizDrawingCommand(geom, color) {
                @Override
                protected void fillGeom(Graphics g, int[] xler, int[] yler) {
                    g.drawString(geom.getClass().getSimpleName(), xler[0], yler[0]);
                }
            };
        }
    }

    /**
     *
     * @see <a href="https://www.smartycoder.com">smartycpder</a>
     *
     */
    public abstract class MapVizDrawingCommand extends VisualizationLibrary.AbstractDrawingCommand
            implements
                DrawingCommand {
        protected final int[] xler;
        protected final int[] yler;

        MapVizDrawingCommand(Geometry geom, Color color) {
            super(geom, color);
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
}
