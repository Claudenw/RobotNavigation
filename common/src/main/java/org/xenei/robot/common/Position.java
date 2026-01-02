package org.xenei.robot.common;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TByteBuffer;
import org.apache.thrift.transport.TMemoryBuffer;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateXY;
import org.xenei.robot.common.mapping.ThetaAndRange;
import org.xenei.robot.common.utils.AngleUtils;
import org.xenei.robot.common.utils.CoordUtils;
import org.xenei.robot.common.utils.SerializerDeserializer;
import org.xenei.robot.common.utils.ThriftSerde;

import java.nio.ByteBuffer;

/**
 * A position is a location and a heading.
 */
public interface Position extends Location, Compass  {

    int BYTES = Position.BYTES + Double.BYTES;

    /**
     * A representation of the origin location (0,0)
     */
    Position ORIGIN = asPosition(Location.ORIGIN, 0.0);

    static Position asPosition(Coordinate coord, double heading) {
        return new Position() {

            @Override
            public Coordinate getCoordinate() {
                return coord;
            }

            @Override
            public double heading() {
                return heading;
            }

            @Override
            public String toString() {
                return PositionUtils.toString(this);
            }
        };
    }

    static Position asPosition(Location loc, double heading) {
        return new Position() {

            @Override
            public Coordinate getCoordinate() {
                return loc.getCoordinate();
            }

            @Override
            public double heading() {
                return AngleUtils.normalize(heading);
            }
        };
    }

    /**
     * Calculates the heading required to move from the current absolute position to
     * another absolute coordinate.
     *
     * @param mapCoordinate
     *            the coordinate to calculate the heading to.
     * @return the heading in radians.
     */
    default double headingTo(Location mapCoordinate) {
        return PositionUtils.headingTo(this, mapCoordinate);
    }

    default Position nextPosition(Location relativeLocation) {
        return Position.PositionUtils.nextPosition(this, relativeLocation);
    }

    /**
     * Calculates the next position.
     * <p>
     * The heading will be the theta from the relative coordinates.
     * </p>
     *
     * @param relativeCoordinates
     *            The coordinates relative to this position to move to.
     * @return the new Position centered on the new position with the proper
     *         heading.
     */
    default Position nextPosition(Coordinate relativeCoordinates) {
        return PositionUtils.nextPosition(this, relativeCoordinates);
    }

    /**
     * Calculates the next position by moving the specified distance.
     * <p>
     * The heading does not change
     * </p>
     *
     * @param scaledRange
     *            The distance to travel.
     * @return the new Position centered on the new position with the proper
     *         heading.
     */
    default Position nextPosition(double scaledRange) {
        return PositionUtils.nextPosition(this, scaledRange);
    }

    class Serde implements SerializerDeserializer<Position>, ThriftSerde<Position> {
        Location.Serde lSerde = new Location.Serde();

        public void serialize(Position position, TProtocol proto) throws TException {
            lSerde.serialize(position, proto);
            proto.writeDouble(position.heading());
        }

        public byte[] serialize(Position position)  {
            try {
                TMemoryBuffer result = new TMemoryBuffer(Position.BYTES);
                TProtocol proto = new TBinaryProtocol(result);
                serialize(position, proto);
                return result.getBuffer();
            } catch (TException e) {
                throw new RuntimeException(e);
            }
        }

        public Position deserialize(byte[] bytes)  {
            try {
                TByteBuffer buffer = new TByteBuffer(ByteBuffer.wrap(bytes));
                TProtocol proto = new TBinaryProtocol(buffer);
                return deserialize(proto);
            } catch (TException e) {
                throw new RuntimeException(e);
            }
        }

        public Position deserialize(TProtocol proto) throws TException {
            return Position.asPosition(lSerde.deserialize(proto), proto.readDouble());
        }
    }


    final class PositionUtils {

        private PositionUtils() {
            // do not instantiate.
        }
        /**
         * Calculates the next position.
         * <p>
         * The heading will be the theta from the relative coordinates.
         * </p>
         *
         * @param relativeCoordinate
         *            The coordinates relative to this position to move to.
         * @return the new Position centered on the new position with the proper
         *         heading.
         */
        static public Position nextPosition(Position position, Coordinate relativeCoordinate) {
           return nextPosition(position, Location.asLocation(relativeCoordinate));
        }

        /**
         * Calculates the next position.
         * <p>
         * The heading will be the theta from the relative coordinates.
         * </p>
         *
         * @param relativeLocation
         *            The coordinates relative to this position to move to.
         * @return the new Position centered on the new position with the proper
         *         heading.
         */
        static public Position nextPosition(Position position, Location relativeLocation) {
            ThetaAndRange thetaAndRange = new ThetaAndRange(position.heading() + relativeLocation.theta(),
                    relativeLocation.range());
            return Position.asPosition(position.plus(thetaAndRange), position.heading() + relativeLocation.theta());
        }

        static public Position nextPosition(Position position, double range) {
            double heading = position.heading();
            return asPosition(CoordUtils.add(position.getCoordinate(), CoordUtils.fromAngle(heading, range)), heading);
        }

        /**
         * Calculates the heading required to move from the current absolute position to
         * another absolute coordinate.
         *
         * @param position the position to start at.
         * @param location
         *            the coordinate to calculate the heading to.
         * @return the heading in radians.
         */
        static public double headingTo(Position position, Location location) {
            if (position.getCoordinate().equals2D(location.getCoordinate())) {
                return position.heading();
            }
            Coordinate pCoordinate = position.getCoordinate();
            Coordinate lCoordinate = location.getCoordinate();
            return AngleUtils.normalize(Math.atan2(lCoordinate.getY() - pCoordinate.getY(), lCoordinate.getX() - pCoordinate.getX()));
        }

//        static public Location relativeLocation(Position position, Location absolute) {
//            return absolute.minus(position);
//        }

        static public String toString(Position position) {
            String name = position.getClass().isAnonymousClass() ? "Position" : position.getClass().getSimpleName();
            return String.format("%s[%s, h: %s]", name, position.getCoordinate(), position.heading());
        }
    }
}
