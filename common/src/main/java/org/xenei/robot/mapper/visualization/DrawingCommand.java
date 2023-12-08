package org.xenei.robot.mapper.visualization;

import java.awt.Color;
import java.awt.Graphics;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
/**
 * 
 * @see https://www.smartycoder.com
 *
 */
public abstract class DrawingCommand {
        int[] xler;
        int[] yler;
        private Color color;

        DrawingCommand(Geometry geom, Color color, double scale) {
            this.color = color;
            Coordinate[] coords = geom.getCoordinates();

            xler = new int[coords.length];
            yler = new int[coords.length];

            for (int i = 0; i < coords.length; i++) {
                xler[i] = (int) Math.round(coords[i].getX() * scale);
                yler[i] = (int) Math.round(coords[i].getY() * scale);
            }
        }

        public void doDrawing(Graphics g) {
            g.setColor(color);
            fillGeom(g, xler, yler);
        }

        abstract protected void fillGeom(Graphics g, int[] xler, int[] yler);
}

