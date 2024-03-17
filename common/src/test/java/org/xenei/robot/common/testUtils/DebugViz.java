package org.xenei.robot.common.testUtils;

import java.util.function.Supplier;

import org.locationtech.jts.geom.Coordinate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.mapping.Map;
import org.xenei.robot.common.planning.Solution;
import org.xenei.robot.mapper.visualization.TextViz;

public class DebugViz extends TextViz {
    private static final Logger LOG = LoggerFactory.getLogger(DebugViz.class);

    public DebugViz(double scale, Map map, Supplier<Solution> solutionSupplier, Supplier<Position> positionSupplier) {
        super(scale, map, solutionSupplier, positionSupplier);
    }

    @Override
    public void redraw(Coordinate target) {
        if (LOG.isDebugEnabled()) {
            super.redraw(target);
        }
    }

    @Override
    protected void output(StringBuilder sb) {
        LOG.debug(sb.insert(0, "\n").toString());
    }

    
}
