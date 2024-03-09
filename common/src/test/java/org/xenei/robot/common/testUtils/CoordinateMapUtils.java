package org.xenei.robot.common.testUtils;

import java.util.List;
import java.util.stream.Collectors;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.mapping.CoordinateMap;
import org.xenei.robot.common.mapping.CoordinateMapBuilder;
import org.xenei.robot.common.mapping.Map;
import org.xenei.robot.common.planning.Planner;

public class CoordinateMapUtils {

    private static final Logger LOG = LoggerFactory.getLogger(CoordinateMapUtils.class);

    private CoordinateMapUtils() {
    }

    public static void logMap(Map map, Planner planner, CoordinateMap initialMap, Position position) {
        CoordinateMap cmap = new CoordinateMapBuilder(initialMap.scale(), initialMap.getContext().chassisInfo).merge(initialMap).build();
        List<Coordinate> lst = map.getObstacles().stream().map(g -> {
            Point p = g.geom().getCentroid();
            return new Coordinate(p.getX(), p.getY());
        }).collect(Collectors.toList());

        cmap.enable(lst, '@');

        planner.getTargets().stream().forEach(c -> cmap.enable(c, '*'));
        planner.getSolution().stream().forEach(r -> cmap.enable(r, '='));
        cmap.enable(position, 'p');
        cmap.enable(planner.getTarget(), 't');
        LOG.info("\n{}", cmap.toString());
        LOG.info(position.toString());
    }

}
