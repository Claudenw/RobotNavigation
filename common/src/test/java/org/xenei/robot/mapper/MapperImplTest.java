package org.xenei.robot.mapper;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.vocabulary.RDF;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.locationtech.jts.geom.Coordinate;
import org.xenei.robot.common.ChassisInfoTest;
import org.xenei.robot.common.DistanceSensor;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.ScaleInfo;
import org.xenei.robot.common.mapping.Mapper;
import org.xenei.robot.common.utils.AngleUtils;
import org.xenei.robot.common.utils.CoordUtils;
import org.xenei.robot.common.utils.RobutContext;
import org.xenei.robot.mapper.map.MapImpl;
import org.xenei.robot.mapper.rdf.Namespace;

public class MapperImplTest {
    private final RobutContext ctxt = new RobutContext(ScaleInfo.DEFAULT, ChassisInfoTest.DEFAULT);
    private final MapImpl map = new MapImpl(ctxt);

    @BeforeEach
    public void setup() {
        map.clear(Namespace.PlanningModel.getURI());
    }

    @Test
    public void processSensorDataTest_TooClose() throws InterruptedException {

        Position currentPosition = new Position(new Coordinate(-1, -3), AngleUtils.RADIANS_90);
        Location target = new Location(new Coordinate(-1, 1));
        Coordinate mapValue = new Coordinate(5, 5);

        Mapper underTest = new MapperImpl(map, () -> target);

        Coordinate expectedObstacle = CoordUtils.fromAngle(0, 2 * map.getContext().scaleInfo.getResolution());
        Coordinate unexpectedObstacle = CoordUtils.fromAngle(0, map.getContext().scaleInfo.getResolution());
        // an obstacle within one radius away is too close so no target generated.
        underTest.getRelativeObstacleConsumer()
                .accept(new DistanceSensor.Readings(currentPosition,
                        List.of(DistanceSensor.DistanceReading.from(unexpectedObstacle),
                                DistanceSensor.DistanceReading.from(expectedObstacle))));

        Thread.sleep(2000);
        System.out.println(MapReports.dumpModel(map));

        await().atMost(2, SECONDS).untilAsserted(() -> assertEquals(1, map.getObstacles().join().size()));
        assertFalse(map.isObstacle(new Location(new Coordinate(-1, 1 + map.getContext().scaleInfo.getResolution()))));
        assertTrue(map.isObstacle(new Location(new Coordinate(-1, -2))));
    }

    private static Stream<Arguments> sensorData() {
        List<Arguments> lst = new ArrayList<>();
        Coordinate sensorReading = CoordUtils.fromAngle(0, 2);
        // results are the center of the cell.
        lst.add(Arguments.of(0, sensorReading, new Coordinate(2, 0), new Coordinate(1.5, 0))); // along x coord
        lst.add(Arguments.of(90, sensorReading, new Coordinate(0, 2), new Coordinate(0, 1.5))); // down Y coord
        lst.add(Arguments.of(180, sensorReading, new Coordinate(-2, 0), new Coordinate(-1.5, 0))); // backwards on x
                                                                                                    // coord
        lst.add(Arguments.of(270, sensorReading, new Coordinate(0, -2), new Coordinate(0, -1.5))); // up Y coord
        return lst.stream();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("sensorData")
    void processSensorDataTest(final double heading, final Coordinate sensorReading, final Coordinate expectedObstacle,
            final Coordinate expectedCoord) {

        Position currentPosition = new Position(new Coordinate(-0, 0), Math.toRadians(heading));
        Location target = new Location(new Coordinate(10, 10));

        Mapper underTest = new MapperImpl(map, () -> target);

        // process data
        underTest.getRelativeObstacleConsumer().accept(new DistanceSensor.Readings(currentPosition,
                List.of(DistanceSensor.DistanceReading.from(sensorReading))));

        await().atMost(2, SECONDS).untilAsserted(() -> assertTrue(map.isObstacle(new Location(expectedObstacle))));

        AskBuilder ask = new AskBuilder().addGraph(Namespace.PlanningModel, new WhereBuilder()
                .addWhere(Namespace.s, Namespace.x, expectedCoord.x).addWhere(Namespace.s, Namespace.y, expectedCoord.y)
                .addWhere(Namespace.s, Namespace.isIndirect, false).addWhere(Namespace.s, RDF.type, Namespace.Coord));
        await().atMost(2, SECONDS).untilAsserted(() -> assertTrue(map.ask(ask)));

    }
}
