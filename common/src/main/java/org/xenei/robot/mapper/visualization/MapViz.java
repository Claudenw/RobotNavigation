package org.xenei.robot.mapper.visualization;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.JFrame;
import javax.swing.WindowConstants;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.xenei.robot.common.mapping.Map;
import org.xenei.robot.common.planning.Solution;
import org.xenei.robot.common.planning.Step;
import org.xenei.robot.common.utils.GeometryUtils;

public class MapViz {
    private Solution solution;
    private Map map;
    private JTSPanel panel;
    private int scale;

    public MapViz(int scale, Map map, Solution solution) {
        this.map = map;
        this.panel = new JTSPanel();
        this.solution = solution;
        this.scale = scale;

        JFrame frame = new JFrame("Map Visualization");
        frame.setLayout(new BorderLayout());
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        frame.add(panel, BorderLayout.CENTER);

        frame.pack();
        frame.setSize(1000, 1000);
        frame.setVisible(true);
    }

    public DrawingCommand getPoly(Geometry geom, Color color) {
        if (geom instanceof Point) {
            return new DrawingCommand(geom, color, scale) {
                @Override
                protected void fillGeom(Graphics g, int[] xler, int[] yler) {
                    g.fillOval(xler[0]-2, yler[0]-2, 4, 4);
                }
            };
        }
        if (geom instanceof Polygon) {
            return new DrawingCommand(geom, color, scale) {
                @Override
                protected void fillGeom(Graphics g, int[] xler, int[] yler) {
                    g.fillPolygon(xler, yler, xler.length);
                }
            };
        }

        if (geom instanceof LineString) {
            return new DrawingCommand(geom, color, scale) {
                @Override
                protected void fillGeom(Graphics g, int[] xler, int[] yler) {
                    g.drawPolyline(xler, yler, xler.length);
                }
            };
        }
        
        return new DrawingCommand(geom, color, scale) {
            @Override
            protected void fillGeom(Graphics g, int[] xler, int[] yler) {
                g.drawString( geom.getClass().getSimpleName(), xler[0], yler[0]);
            }
        };
    }

    public void redraw(Coordinate target) {
        List<DrawingCommand> cmds = new ArrayList<>();
        for (Geometry obst : map.getObstacles()) {
            cmds.add(getPoly(obst, Color.RED));
        }

        for (Step targ : map.getTargets()) {
            cmds.add(getPoly(targ.getGeometry(), Color.CYAN));
        }

        List<Coordinate> lst = solution.stream().collect(Collectors.toList());
        if (lst.size() > 1) {
            cmds.add(getPoly(GeometryUtils.asPath(lst.toArray(new Coordinate[lst.size()])), Color.WHITE));
        } else {
            cmds.add(getPoly(GeometryUtils.asPolygon(lst.get(0), 0.25), Color.WHITE));
        }
        
        if (target != null) {
            cmds.add( getPoly(GeometryUtils.asPolygon(target, 0.1), Color.GREEN));
        }

        rescale(cmds);

        EventQueue.invokeLater(() -> {
            panel.clear();
            cmds.forEach(panel::addDrawCommand);
        });
    }

    private void rescale(List<DrawingCommand> lst) {
        double max = Integer.MIN_VALUE;
        for (DrawingCommand cmd : lst) {
            for (int i : cmd.xler) {
                double ii = Math.abs(i);
                max = ii < max ? max : ii;
            }
            for (int i : cmd.yler) {
                double ii = Math.abs(i);
                max = ii < max ? max : ii;
            }
        }
        int offset = (int) Math.max(2 * max / 700, 1);
        for (DrawingCommand cmd : lst) {
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
}
