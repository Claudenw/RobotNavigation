package org.xenei.robot.mapper.visualization;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.JFrame;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.xenei.robot.common.mapping.Map;
import org.xenei.robot.common.mapping.Mapper;
import org.xenei.robot.common.planning.Solution;
import org.xenei.robot.common.planning.Step;
import org.xenei.robot.common.utils.DoubleUtils;
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
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        frame.add(panel, BorderLayout.CENTER);

        frame.pack();
        frame.setSize(1000,1000);
        frame.setVisible(true);
    }
    
    public void redraw() {
        List<AbstractDrawingCommand> cmds = new ArrayList<>();
        for (Geometry obst : map.getObstacles()) {
            cmds.add(new Poly(obst, Color.RED));
        }

        for (Step targ : map.getTargets()) {
            cmds.add(new Poly(GeometryUtils.asPolygon(targ.getGeometry().getCoordinate(), 0.25), Color.CYAN));
        }

        List<Coordinate> lst = solution.stream().collect(Collectors.toList());
        if (lst.size() > 1) {
            cmds.add( new Poly(GeometryUtils.asPath(lst.toArray(new Coordinate[lst.size()])),
                    Color.WHITE));
        } else {
            cmds.add( new Poly(GeometryUtils.asPolygon(lst.get(0), 0.25),
                    Color.WHITE));
        }
        
        rescale(cmds);
        
        EventQueue.invokeLater( () -> {
            panel.clear();
            cmds.forEach(panel::addDrawCommand);
        } );
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
        int offset = (int) (2*max / 700);
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

    private abstract class AbstractDrawingCommand implements DrawingCommand {
        private int[] xler;
        private int[] yler;
        private Color color;
        
        AbstractDrawingCommand(Geometry geom, Color color) {
            this.color = color;
            Coordinate[] coords = geom.getCoordinates();

            xler = new int[coords.length];
            yler = new int[coords.length];

            for (int i = 0; i < coords.length; i++) {
                xler[i] = (int) Math.round( coords[i].getX()*scale );
                yler[i] = (int) Math.round( coords[i].getY()*scale );
            }
        }

        @Override
        public void doDrawing(Graphics g) {
            g.setColor(color);
            fillGeom(g, xler, yler);
        }
        
        abstract protected void fillGeom(Graphics g, int[] xler, int[] yler);
    }
    
    private class Poly extends AbstractDrawingCommand {
       Poly(Geometry geom, Color color) {
           super( geom, color );
        }

        @Override
        protected void fillGeom(Graphics g, int[] xler, int[] yler) {
            g.fillPolygon(xler, yler, xler.length);
        }
    }

    private class Line extends AbstractDrawingCommand {
       Line(Geometry geom, Color color) {
           super( geom, color );
       }

       @Override
       protected void fillGeom(Graphics g, int[] xler, int[] yler) {
            g.drawPolyline(xler, yler, xler.length);
        }
    }
}
