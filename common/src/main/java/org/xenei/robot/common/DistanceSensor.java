package org.xenei.robot.common;

import org.locationtech.jts.geom.Coordinate;
import org.xenei.robot.common.utils.CoordUtils;

import java.util.Collection;

public interface DistanceSensor extends Runnable {

	/**
	 * The angle and range to some object.
	 * 
	 * @param theta
	 *            the angle to the object.
	 * @param range
	 *            the range to the object.
	 */
	record DistanceReading(double theta, double range) {
		/**
		 * Gets the location of the distance reading.
		 * 
		 * @return the Location from the distance reading.
		 */
		public Location getLocation() {
			return Location.from(CoordUtils.fromAngle(theta, range));
		}

		/**
		 * Creates a distance reading from a FrontsCoordinate object.
		 * 
		 * @param location
		 *            the location to create a distance reading to.
		 * @return the distance reading to the location.
		 */
		public static DistanceReading from(FrontsCoordinate location) {
			return new DistanceReading(location.theta(), location.range());
		}

		/**
		 * Creates a distance reading from a Coordinate.
		 * 
		 * @param coordinate
		 *            the coordinate to create a distance reading to.
		 * @return a new DistanceReading
		 */
		public static DistanceReading from(Coordinate coordinate) {
			return from(Location.from(coordinate));
		}

		/**
		 * Creates a distance reading from x and y location.
		 * 
		 * @param x
		 *            the x location.
		 * @param y
		 *            the y location.
		 * @return a new DistanceReading.
		 */
		public static DistanceReading from(double x, double y) {
			return from(Location.from(x, y));
		}
	}

	/**
	 * The origin and readings from that origin to objects.
	 * 
	 * @param origin
	 *            the origin position.
	 * @param readings
	 *            the collection of sensor readings from the origin.
	 */
	record Readings(Position origin, Collection<DistanceReading> readings) {
	}

	/**
	 * The maximum range the sensor can detect.
	 * 
	 * @return the maximum range the sensor can detect
	 */
	double maxRange();
}
