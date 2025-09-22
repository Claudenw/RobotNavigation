package org.xenei.robot.mapper;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.common.DistanceSensor;
import org.xenei.robot.common.FrontsCoordinate;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.mapping.Map;
import org.xenei.robot.common.mapping.Mapper;
import org.xenei.robot.common.utils.CoordUtils;
import org.xenei.robot.common.utils.DoubleUtils;

public class MapperImpl implements Mapper {
	private static final Logger LOG = LoggerFactory.getLogger(MapperImpl.class);
	private final Map map;
	private final Supplier<FrontsCoordinate> targetSupplier;

	/**
	 *
	 * @param map
	 *            The map to work with.
	 * @param targetSupplier
	 *            a target supplier scaled to the map.
	 */
	public MapperImpl(Map map, Supplier<FrontsCoordinate> targetSupplier) {
		this.map = map;
		this.targetSupplier = targetSupplier;
	}

	@Override
	public Consumer<DistanceSensor.Readings> getRelativeObstacleConsumer() {
		return readings -> {
			readings.readings().forEach(relativeObstacle -> {
				if (!DoubleUtils.inRange(relativeObstacle.range(), map.getContext().chassisInfo.radius)) {
					Location scaledObstacle = map.getContext().scaleInfo.round(relativeObstacle.getLocation());
					map.getContext().submit(new ObstacleMapper(map, readings.origin(), scaledObstacle));
				}
			});
		};
	}

	@Override
	public boolean isClearPath(Position currentPosition, FrontsCoordinate target) {
		return map.isClearPath(currentPosition, target);
	}

	/**
	 * Adds coordinates to the map that are near a registered obstacle.
	 */
	class ObstacleMapper implements Runnable {
		final Map map;
		final Position currentPosition;
		final Location relativeObstacle;

		ObstacleMapper(Map map, Position currentPosition, Location relativeObstacle) {
			this.map = map;
			this.currentPosition = currentPosition;
			this.relativeObstacle = relativeObstacle;
		}

		public void run() {
			// Optional<Coordinate> findCoordinateNear(Location relativeObstacle) {
			double distance = relativeObstacle.range() - map.getContext().scaledRadius;
			if (distance < map.getContext().scaledRadius) {
				return;
			}
			Location relativeCoord = Location.from(CoordUtils.fromAngle(relativeObstacle.theta(), distance));
			Location candidate = currentPosition.nextPosition(relativeCoord);
			// if it is not an obstacle add it.
			if (!map.isObstacle(candidate)) {
				map.addObstacle(map.createObstacle(currentPosition, relativeObstacle));
			}
			map.addCoord(candidate, targetSupplier.get(), false);
		}
	}
}
