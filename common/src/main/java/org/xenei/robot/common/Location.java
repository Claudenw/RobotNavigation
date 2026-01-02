package org.xenei.robot.common;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TByteBuffer;
import org.apache.thrift.transport.TMemoryBuffer;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateXY;
import org.xenei.robot.common.mapping.ThetaAndRange;

import org.xenei.robot.common.utils.CoordUtils;
import org.xenei.robot.common.serialization.SerializerDeserializer;
import org.xenei.robot.common.serialization.ThriftSerde;

import java.nio.ByteBuffer;
import java.util.Comparator;

public interface Location {

    int BYTES = Double.BYTES * 2;

    Comparator<Location> XYCompr = (one, two) -> CoordUtils.XYCompr.compare(one.getCoordinate(), two.getCoordinate());

    /**
     * A representation of the origin location (0,0)
     */
    Location ORIGIN = asLocation(UnmodifiableCoordinate.make(new Coordinate(0, 0)));

    /**
     * A representative infinite value.
     */
    Location INFINITE = asLocation(UnmodifiableCoordinate
            .make(new Coordinate(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY)));

    /**
     * Creates a simple Location implementation from a coordinate.
     * @param coord the coordinate for the location.
     * @return a simple Location on the coordinate.
     */
    static Location asLocation(Coordinate coord) {
        return new Location() {

            @Override
            public Coordinate getCoordinate() {
                return coord;
            }

            @Override
            public String toString() {
                return LocationUtils.toString(this);
            }

            @Override
            public int hashCode() {
                return getCoordinate().hashCode();
            }

            @Override
            public boolean equals(Object obj) {
                if (obj instanceof Location other) {
                    return getCoordinate().equals(other.getCoordinate());
                }
                return false;
            }
        };
    }

    /**
     * Gets the coordinate for the location.
     * @return the coordinate for the location.
     */
    Coordinate getCoordinate();

    default boolean isInfinite() {
        return CoordUtils.isInfinite(this.getCoordinate());
    }

    default double angleBetween(Location mapCoordinate) {
        return CoordUtils.angleBetween(this.getCoordinate(), mapCoordinate.getCoordinate());
    }
    /**
     * Return the angle in radians from the origin.
     *
     * @return the angle in radians from the origin to this coordinate.
     */
    default double theta() {
        return LocationUtils.theta(this);
    }

    default double range() {
        return LocationUtils.range(this);
    }

    default double getX() {
        return getCoordinate().getX();
    }

    default double getY() {
        return getCoordinate().getY();
    }

    default double distance(Location location) {
        return getCoordinate().distance(location.getCoordinate());
    }

    default boolean sameCoordinate(final Location location) {
        return compareTo(location) == 0;
    }

    default boolean sameCoordinate(final Coordinate coordinate) {
        return getCoordinate().equals2D(coordinate);
    }

    default boolean equals2D(Location location) {
        return compareTo(location) == 0;
    }

    default int compareTo(Location location) {
        return getCoordinate().compareTo(location.getCoordinate());
    }

    default boolean isNaN() {
        return CoordUtils.isNaN(getCoordinate());
    }

    default Location minus(Location location) {
        return Location.asLocation(CoordUtils.minus(this.getCoordinate(), location.getCoordinate()));
    }

    default Location plus(Location location) {
        return Location.asLocation(CoordUtils.plus(this.getCoordinate(), location.getCoordinate()));
    }

    default Location relativeLocation(Location absoluteLocation) {
        return LocationUtils.relativeLocation(this, absoluteLocation);
    }

    default Location absoluteLocation(Location relativeLocation) {
        return this.plus(relativeLocation);
    }

    final class LocationUtils {
        private LocationUtils() {
            // do not instantiate
        }

        public static double theta(Location location) {
            return CoordUtils.angleBetween(ORIGIN.getCoordinate(), location.getCoordinate());
        }

        public static double range(Location location) {
            return ORIGIN.getCoordinate().distance(location.getCoordinate());
        }

        static public String toString(Location location) {
            return String.format("%s[%s]", location.getClass().getSimpleName(), location.getCoordinate());
        }

        static public Location relativeLocation(Location location, Location absoluteLocation) {
            double range = location.getCoordinate().distance(absoluteLocation.getCoordinate());
            if (range == 0) {
                return location;
            }
            return new ThetaAndRange(location.angleBetween(absoluteLocation), range);
        }
    }

    class Serde implements SerializerDeserializer<Location>, ThriftSerde<Location> {

        public void serialize(Location location, TProtocol proto) throws TException {
            proto.writeDouble(location.getX());
            proto.writeDouble(location.getY());
        }

        public byte[] serialize(Location location)  {
            try {
                TMemoryBuffer result = new TMemoryBuffer(Location.BYTES);
                TProtocol proto = new TBinaryProtocol(result);
                serialize(location, proto);
                return result.getArray();
            } catch (TException e) {
                throw new RuntimeException(e);
            }
        }

        public Location deserialize(byte[] bytes)  {
            try {
                TByteBuffer buffer = new TByteBuffer(ByteBuffer.wrap(bytes));
                TProtocol proto = new TBinaryProtocol(buffer);
                return deserialize(proto);
            } catch (TException e) {
                throw new RuntimeException(e);
            }
        }

        public Location deserialize(TProtocol proto) throws TException {
            return Location.asLocation(new CoordinateXY(proto.readDouble(), proto.readDouble()));
        }
    }
}
