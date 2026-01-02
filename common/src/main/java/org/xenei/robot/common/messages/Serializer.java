package org.xenei.robot.common.messages;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TProtocol;
import org.locationtech.jts.geom.CoordinateXY;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.Position;

public class Serializer {


    protected static void serialize(Location location, TProtocol protocol) throws TException {
        protocol.writeDouble(location.getX());
        protocol.writeDouble(location.getY());
    }

    protected static Location deserializeLocation(TProtocol protocol) throws TException {
        return Location.asLocation(new CoordinateXY(protocol.readDouble(), protocol.readDouble()));
    }

    protected static void serialize(Position position, TProtocol protocol) throws TException {
        serialize((Location)position, protocol);
        protocol.writeDouble(position.heading());
    }

    protected static Position deserializePosition(TProtocol protocol) throws TException {
        return Position.asPosition(deserializeLocation(protocol), protocol.readDouble());
    }

}
