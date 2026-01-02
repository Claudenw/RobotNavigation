package org.xenei.robot.common.testUtils;

import org.locationtech.jts.geom.Geometry;
import org.slf4j.LoggerFactory;
import org.xenei.robot.common.sensor.distance.DistanceSensor;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.mapping.Map;
import org.xenei.robot.common.mapping.MapObstacle;
import org.xenei.robot.common.mapping.ThetaAndRange;

import java.util.List;
import java.util.stream.Collectors;

public interface FakeDistanceSensor extends DistanceSensor {
    Map map();

    default ThetaAndRange look(Position position, double heading) {
        if (LoggerFactory.getLogger(this.getClass()).isDebugEnabled()) {
            LoggerFactory.getLogger(this.getClass()).debug("Scanning heading: {} {}", heading, Math.toDegrees(heading));
        }

        Geometry boundingBox = map().getContext().geometryUtils.asPath(map().getContext().chassisInfo.radius, position,
                position.absoluteLocation(new ThetaAndRange(heading, maxRange())));
        Geometry positionGeometry = map().getContext().geometryUtils.asPoint(position);
        List<MapObstacle> lst = map().getObstacles(boundingBox).join().collect(Collectors.toList());
        double range = map().getObstacles(boundingBox).join().map(mapObstacle -> mapObstacle.getGeometry().distance(positionGeometry))
                .min(Double::compareTo).orElse(maxRange());

        return new ThetaAndRange(heading, range);
    }

}
