package org.xenei.robot.mapper.visualization;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TByteBuffer;
import org.apache.thrift.transport.TEndpointTransport;
import org.apache.thrift.transport.TMemoryBuffer;
import org.apache.thrift.transport.TTransportException;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.io.WKTWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.mapping.Map;
import org.xenei.robot.common.mapping.MapCoordinate;
import org.xenei.robot.common.mapping.MapLocation;
import org.xenei.robot.common.planning.Solution;
import org.xenei.robot.common.serialization.SerializationException;
import org.xenei.robot.common.utils.RobutContext;
import org.xenei.robot.common.serialization.SerializerDeserializer;
import org.xenei.robot.common.serialization.ThriftSerde;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class RemoteVisualization {
    private static final Logger LOG = LoggerFactory.getLogger(RemoteVisualization.class);
    private static final Serde SERDE = new Serde();

    public RemoteVisualization() {
    }

    private void addCommand(final TProtocol protocol, DrawingCommand cmds) {
        addCommands(protocol, Collections.singletonList(cmds));
    }

    private synchronized void addCommands(final TProtocol protocol, Collection<DrawingCommand> cmds) {
        for (DrawingCommand cmd : cmds) {
            try {
                SERDE.serialize(cmd, protocol);
            } catch (TException e) {
                LOG.error("Unable to serialize {}", cmd);
            }
        }
    }

    private TProtocol getProtocol(TEndpointTransport buffer) {
        return new TBinaryProtocol(buffer);
    }

    public void draw(Map map, Supplier<Solution> solutionSupplier,
                                  Supplier<? extends Position> positionSupplier, Supplier<? extends Location> targetSupplier) {
        final RobutContext ctxt = map.getContext();
        try {
            TMemoryBuffer result = new TMemoryBuffer(1000);
            final TProtocol proto = getProtocol(result);

            java.util.List<CompletableFuture<?>> futures = new ArrayList<>();

            MapLocation target = map.asMapLocation(targetSupplier.get());
            futures.add(map.getObstacles().thenAccept(obs ->
                    obs.forEach(obst -> {
                        List<DrawingCommand> cmds = new ArrayList<>();
                        if (obst.getGeometry() instanceof GeometryCollection gCollection) {
                            for (int i = 0; i < gCollection.getNumGeometries(); i++) {
                                cmds.add(new DrawingCommand(gCollection.getGeometryN(i), ObjectType.Obstacle));
                            }

                        } else {
                            cmds.add(new DrawingCommand(obst.getGeometry(), ObjectType.Obstacle));
                        }
                        addCommands(proto, cmds);
                    })));


            futures.add(map.getLocations().thenAccept(coords -> {
                List<DrawingCommand> cmds = new ArrayList<>();
                coords.forEach(mapCoord ->
                        cmds.add(new DrawingCommand(mapCoord.getGeometry(), target != null && mapCoord.isIndirect(target) ? ObjectType.IndirectLocation : ObjectType.Location))
                );
                addCommands(proto, cmds);
            }));

            Solution solution = solutionSupplier.get();
            if (solution != null) {
                List<MapLocation> lst = solution.stream().toList();
                if (lst.size() > 1) {
                    addCommand(proto, new DrawingCommand(ctxt.geometryUtils.asPath(0.25, lst.toArray(new MapCoordinate[0])), ObjectType.Solution));
                } else if (lst.size() == 1) {
                    addCommand(proto, new DrawingCommand(ctxt.geometryUtils.asPolygon(lst.get(0), .25), ObjectType.Solution));
                }
            }
            if (target != null) {
                addCommand(proto, new DrawingCommand(ctxt.geometryUtils.asPolygon(target, 0.25), ObjectType.Target));
            }

            for (CompletableFuture<?> f : futures) {
                f.join();
            }

            Position position = positionSupplier.get();
            if (position != null) {
                addCommand(proto, new DrawingCommand(ctxt.geometryUtils.asPolygon(position.getCoordinate(), 0.25), ObjectType.Position));

                if (target != null) {
                    addCommand(proto, new DrawingCommand(ctxt.geometryUtils.asPath(ctxt.chassisInfo.radius, position.getCoordinate(),
                            target.getCoordinate()), ObjectType.Path));
                }
            }
            ctxt.sendRemoteVizCommand(result.getArray());
        } catch (TException e) {
            LOG.error("Unable to draw map: {}", e.getMessage(), e);
        }
    }

    public List<DrawingCommand> deserialize(byte[] bytes) throws TTransportException {
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        TByteBuffer buffer = new TByteBuffer(byteBuffer);
        final TProtocol proto = getProtocol(buffer);
        final List<DrawingCommand> cmds = new ArrayList<>();
        try {
            while (byteBuffer.hasRemaining()) {
                cmds.add(SERDE.deserialize(proto));
            }
        } catch (TException e) {
            LOG.error("Unable to deserialize. Aborting: {}", e.getMessage(), e);
        }
        return cmds;
    }

    /**
     * Enum in order of default priority in visualization with highest ordinal being most important.
     */
    public enum ObjectType {Location, IndirectLocation, Obstacle, Solution, Path, Target, Position}

    public record DrawingCommand(Geometry geometry, ObjectType type) {}

    public static class Serde implements ThriftSerde<DrawingCommand>, SerializerDeserializer<DrawingCommand> {
        private final WKTWriter writer = new WKTWriter();
        private final WKTReader reader = new WKTReader();

        @Override
        public void serialize(DrawingCommand command, TProtocol proto) throws TException {
                proto.writeString(writer.write(command.geometry));
                proto.writeString(command.type.toString());
        }

        @Override
        public byte[] serialize(DrawingCommand command) throws SerializationException {
            try {
                TMemoryBuffer buffer = new TMemoryBuffer(1000);
                TProtocol proto = new TBinaryProtocol(buffer);
                serialize(command, proto);
                return buffer.getArray();
            } catch (TException e) {
                throw new SerializationException(String.format("Unable to serialize %s, Aborting.", command), e);
            }
        }

        @Override
        public DrawingCommand deserialize(TProtocol proto) throws TException {
            try {
                return new DrawingCommand(reader.read(proto.readString()), ObjectType.valueOf(proto.readString()));
            } catch (ParseException | IllegalArgumentException e) {
                throw new TException("Unable to parse drawing command: " + e.getMessage(), e);
            }
        }

        @Override
        public DrawingCommand deserialize(byte[] bytes) throws SerializationException {
            ByteBuffer buff = ByteBuffer.wrap(bytes);
            try {
                TByteBuffer buffer = new TByteBuffer(buff);
                TProtocol proto = new TBinaryProtocol(buffer);
                return deserialize(proto);
            } catch (TException e) {
                throw new SerializationException("Unable to deserialize. Aborting.", e);
            }
        }
    }
}
