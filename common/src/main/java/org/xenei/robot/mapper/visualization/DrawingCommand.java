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
public interface DrawingCommand {
        void doDrawing(Graphics g);
}

