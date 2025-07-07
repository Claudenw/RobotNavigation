package org.xenei.robot.mapper.visualization;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.xenei.robot.common.Position;
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
            return drawPoint((Point)geom, color);
        }
        if (geom instanceof Polygon) {
            return drawPolygon((Polygon)geom, color);
        }
        if (geom instanceof LineString || geom instanceof MultiLineString) {
            return drawLine(geom, color);
        }
        return drawString(geom, color);
    }

    public java.util.List<T> draw(Map map, Supplier<Solution> solutionSupplier, Supplier<Position> positionSupplier,
                                  Supplier<Coordinate> targetSupplier) {
        java.util.List<T> cmds = new ArrayList<>();
        java.util.List<CompletableFuture<?>> futures = new ArrayList<>();
        futures.add(map.getObstacles().thenAccept( obs -> obs.forEach(obst ->
        {
            if (obst.geom() instanceof GeometryCollection gCollection) {
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

        List<Coordinate> lst = solutionSupplier.get().stream().toList();
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

        if (target != null) {
            cmds.add(getPoly(geometryUtils.asPath(map.getContext().chassisInfo.radius, position.getCoordinate(), target), Color.ORANGE));
        }

        for (CompletableFuture<?> f : futures) {
            f.join();
        }

        return cmds;
    }


    /**
     *
     * @see <a href="https://www.smartycoder.com">smartycpder</a>
     *
     */
    abstract static class AbstractDrawingCommand  {

        protected Color color;

        AbstractDrawingCommand(Geometry geom, Color color) {
            this.color = color;
        }
    }
}
