package org.xenei.robot.testUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.navigation.Position;
import org.xenei.robot.planner.PlanRecord;
import org.xenei.robot.planner.Planner;
import org.xenei.robot.utils.CoordinateMap;
import org.xenei.robot.utils.CoordinateMapBuilder;

public class CoordinateMapUtils {

    private static final Logger LOG = LoggerFactory.getLogger(CoordinateMapUtils.class);

    private CoordinateMapUtils() {
    }

    public static void logMap(Planner planner, CoordinateMap initialMap, Position p) {
        CoordinateMap map = new CoordinateMapBuilder(initialMap.scale()).merge(initialMap).build();
        map.enable(planner.getMap().getObstacles(), '@');
        planner.getPlanRecords().stream().map(PlanRecord::coordinates).forEach(c -> map.enable(c, '*'));
        planner.getSolution().stream().forEach(r -> map.enable(r, '='));
        map.enable(p.coordinates(), 'p');
        map.enable(planner.getTarget(), 't');
        LOG.info("\n{}", map.toString());
        LOG.info(p.toString());
    }

}
