package org.xenei.robot.common.testUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.common.CoordinateMap;
import org.xenei.robot.common.CoordinateMapBuilder;
import org.xenei.robot.common.Map;
import org.xenei.robot.common.Planner;
import org.xenei.robot.common.Position;

public class CoordinateMapUtils {

    private static final Logger LOG = LoggerFactory.getLogger(CoordinateMapUtils.class);

    private CoordinateMapUtils() {
    }

    public static void logMap(Map map, Planner planner, CoordinateMap initialMap, Position p) {
        CoordinateMap cmap = new CoordinateMapBuilder(initialMap.scale()).merge(initialMap).build();
        cmap.enable(map.getObstacles(), '@');
        planner.getTargets().stream().forEach(c -> cmap.enable(c, '*'));
        planner.getSolution().stream().forEach(r -> cmap.enable(r, '='));
        cmap.enable(p.coordinates(), 'p');
        cmap.enable(planner.getTarget(), 't');
        LOG.info("\n{}", cmap.toString());
        LOG.info(p.toString());
    }

}
