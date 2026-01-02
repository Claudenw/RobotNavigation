package org.xenei.robot.mapper.visualization;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.mapping.MapCoordinate;
import org.xenei.robot.common.mapping.MapLocation;
import org.xenei.robot.common.mapping.MapPosition;
import org.xenei.robot.common.planning.Solution;
import org.xenei.robot.common.utils.GeometryUtils;
import org.xenei.robot.common.mapping.Map;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

abstract class VisualizationLibrary<T extends VisualizationLibrary.AbstractDrawingCommand> {

    private final GeometryUtils geometryUtils;
    protected VisualizationLibrary(GeometryUtils geometryUtils) {
        this.geometryUtils = geometryUtils;
    }

    abstract protected T drawPoint(Point geom, Color color);
    abstract protected T drawPolygon(Polygon geom, Color color);
    abstract protected T drawLine(Geometry geom, Color color);
    abstract protected T drawString(Geometry geom, Color color);

    T getPoly(Geometry geom, Color color) {
        if (geom instanceof Point) {
            return drawPoint((Point) geom, color);
        }
        if (geom instanceof Polygon) {
            return drawPolygon((Polygon) geom, color);
        }
        if (geom instanceof LineString || geom instanceof MultiLineString) {
            return drawLine(geom, color);
        }
        return drawString(geom, color);
    }

    public T convert(RemoteVisualization.DrawingCommand cmd) {
        return switch (cmd.type()) {
            case Location -> getPoly(cmd.geometry(), Color.BLUE);
            case IndirectLocation -> getPoly(cmd.geometry(), Color.CYAN);
            case Obstacle -> getPoly(cmd.geometry(), Color.RED);
            case Solution -> getPoly(cmd.geometry(), Color.WHITE);
            case Target -> getPoly(geometryUtils.asPolygon(cmd.geometry().getCoordinate(), 0.25), Color.GREEN);
            case Position -> getPoly(geometryUtils.asPolygon(cmd.geometry().getCoordinate(), 0.25), Color.ORANGE);
            case Path -> getPoly(cmd.geometry(), Color.GRAY);
        };
    }

    /**
     *
     * @see <a href="https://www.smartycoder.com">smartycpder</a>
     *
     */
    abstract static class AbstractDrawingCommand {

        protected Color color;

        AbstractDrawingCommand(Geometry geom, Color color) {
            this.color = color;
        }
    }
}
