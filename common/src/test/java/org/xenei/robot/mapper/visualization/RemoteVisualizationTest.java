package org.xenei.robot.mapper.visualization;


import org.apache.thrift.TException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.xenei.robot.common.ScaleInfo;
import org.xenei.robot.common.serialization.SerializationException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class RemoteVisualizationTest {
    private static final GeometryFactory geometryFactory = new GeometryFactory(ScaleInfo.DEFAULT.getPrecisionModel());

    @ParameterizedTest
    @MethodSource("commands")
    void drawingCommandSerdeTest(RemoteVisualization.DrawingCommand command) throws SerializationException {
        RemoteVisualization.Serde serde = new RemoteVisualization.Serde();

        byte[] buffer = serde.serialize(command);
        RemoteVisualization.DrawingCommand command2 = serde.deserialize(buffer);
        assertThat(command2).isEqualTo(command);
    }

    //     public enum ObjectType {Location, IndirectLocation, Obstacle, Solution, Target, Position, Path}
    static Stream<RemoteVisualization.DrawingCommand> commands() {
        List<RemoteVisualization.DrawingCommand> commands = new ArrayList<>();
        Geometry geo = geometryFactory.createPoint(new Coordinate(0, 0));
        commands.add(new RemoteVisualization.DrawingCommand(geo, RemoteVisualization.ObjectType.Location));
        geo = geometryFactory.createPoint(new Coordinate(100, 100));
        commands.add(new RemoteVisualization.DrawingCommand(geo, RemoteVisualization.ObjectType.IndirectLocation));
        geo = geometryFactory.createMultiLineString(new LineString[] {
                geometryFactory.createLineString(new Coordinate[] {new Coordinate(10,10), new Coordinate(20, 20)}),
                geometryFactory.createLineString(new Coordinate[] {new Coordinate(10, 15), new Coordinate(20,15), new Coordinate(20, 18 )})});
        commands.add(new RemoteVisualization.DrawingCommand(geo, RemoteVisualization.ObjectType.Obstacle));
        geo = geometryFactory.createLineString(new Coordinate[]{new Coordinate(0,0), new Coordinate(9,9), new Coordinate(100, 100), new Coordinate(100, 300)});
        commands.add(new RemoteVisualization.DrawingCommand(geo, RemoteVisualization.ObjectType.Solution));
        geo = geometryFactory.createPoint(new Coordinate(100, 300));
        commands.add(new RemoteVisualization.DrawingCommand(geo, RemoteVisualization.ObjectType.Target));
        geo = geometryFactory.createPoint(new Coordinate(1, 1));
        commands.add(new RemoteVisualization.DrawingCommand(geo, RemoteVisualization.ObjectType.Position));
        geo = geometryFactory.createLineString(new Coordinate[] {new Coordinate(100, 100), new Coordinate(100,295)});
        commands.add(new RemoteVisualization.DrawingCommand(geo, RemoteVisualization.ObjectType.Position));
        return commands.stream();
    }
}
