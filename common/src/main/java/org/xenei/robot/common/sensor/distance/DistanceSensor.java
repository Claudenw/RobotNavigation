package org.xenei.robot.common.sensor.distance;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TByteBuffer;
import org.apache.thrift.transport.TMemoryBuffer;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.serialization.SerializationException;
import org.xenei.robot.common.serialization.SerializerDeserializer;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public interface DistanceSensor extends Runnable {

    /**
     * The origin and readings from that origin to objects.
     *
     * @param origin
     *            the origin position.
     * @param readings
     *            the collection of sensor readings from the origin.
     */
    record Readings(Position origin, Collection<? extends Location> readings) {
    }

    /**
     * The maximum range the sensor can detect.
     *
     * @return the maximum range the sensor can detect
     */
    double maxRange();

    class Serde implements SerializerDeserializer<Readings> {
        Location.Serde lSerde = new Location.Serde();
        Position.Serde pSerde = new Position.Serde();

        @Override
        public byte[] serialize(DistanceSensor.Readings readings) throws SerializationException {
            System.out.println("Serializing " + readings);
            int size = Short.BYTES + Position.BYTES + readings.readings().size() * Location.BYTES;
            if (size > Short.MAX_VALUE) {
                throw new SerializationException("Too large distance sensor readings");
            }
            try {
                TMemoryBuffer result = new TMemoryBuffer(size);
                TProtocol proto = new TBinaryProtocol(result);
                pSerde.serialize(readings.origin(), proto);
                proto.writeI16((short) readings.readings().size());
                for (Location loc : readings.readings()) {
                    lSerde.serialize(loc, proto);
                }
                return result.getArray();
            } catch (TException e) {
                throw new SerializationException(e.getMessage(), e);
            }
        }

        public DistanceSensor.Readings deserialize(byte[] buff) throws SerializationException {
            System.out.println("Deserializing " + buff.length + " bytes");
            try {
                TByteBuffer buffer = new TByteBuffer(ByteBuffer.wrap(buff));
                TProtocol proto = new TBinaryProtocol(buffer);
                Position pos = pSerde.deserialize(proto);
                int count = proto.readI16();
                List<Location> readings = new ArrayList<>(count);
                for (int i = 0; i < count; i++) {
                    readings.add(lSerde.deserialize(proto));
                }
                return new DistanceSensor.Readings(pos, readings);
            } catch (TException e) {
                throw new SerializationException(e.getMessage(), e);
            }
        }
    }
}
