package org.xenei.robot.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

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
import org.xenei.robot.common.mapping.Map;
import org.xenei.robot.common.mapping.MapLocation;
import org.xenei.robot.common.mapping.MapTest;
import org.xenei.robot.common.mapping.Mapper;
import org.xenei.robot.common.mapping.ThetaAndRange;
import org.xenei.robot.common.utils.AngleUtils;
import org.xenei.robot.common.utils.RobutContext;

public class MapperImplTest {
    private final RobutContext ctxt = new RobutContext(ScaleInfo.DEFAULT, ChassisInfoTest.DEFAULT);
    private MapTest.TestingStorage testingStorage;
    private Map map;

    @BeforeEach
    public void setup() {
        testingStorage = new MapTest.TestingStorage();
        map = new Map(ctxt, testingStorage);
    }

    @Test
    public void processSensorDataTest_TooClose() throws InterruptedException {

        Position currentPosition = Position.asPosition(new Coordinate(-1, -3), AngleUtils.RADIANS_90);
        Location target = Location.asLocation(new Coordinate(-1, 1));
        Coordinate mapValue = new Coordinate(5, 5);

        Mapper underTest = new MapperImpl(map, () -> target);

        ThetaAndRange expectedObstacle = new ThetaAndRange(0, 2 * map.getContext().scaleInfo.getResolution());
        ThetaAndRange unexpectedObstacle = new ThetaAndRange(0, map.getContext().scaleInfo.getResolution());
        // an obstacle within one radius away is too close so no target generated.
        underTest.getRelativeObstacleConsumer()
                .accept(new DistanceSensor.Readings(currentPosition, List.of(unexpectedObstacle, expectedObstacle)));

        //System.out.println(MapReports.dumpModel(map));

        assertEquals(1, map.getObstacles().join().count());
        assertFalse(map.isObstacle(map.asMapCoordinate(new Coordinate(-1, 1 + map.getContext().scaleInfo.getResolution()))));
        assertTrue(map.isObstacle(map.asMapCoordinate((new Coordinate(-1, -2)))));
    }

    private static Stream<Arguments> sensorData() {
        List<Arguments> lst = new ArrayList<>();
        Location sensorReading = new ThetaAndRange(0, 2);
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

        Position currentPosition = Position.asPosition(new Coordinate(-0, 0), Math.toRadians(heading));
        Location target = Location.asLocation(new Coordinate(10, 10));

        Mapper underTest = new MapperImpl(map, () -> target);

        // process data
        underTest.getRelativeObstacleConsumer().accept(new DistanceSensor.Readings(currentPosition,
                List.of(Location.asLocation(sensorReading))));

        assertTrue(map.isObstacle(map.asMapCoordinate(expectedObstacle)));

        Optional<MapLocation> optional = map.getLocations().join().filter(loc -> loc.sameCoordinates(expectedCoord)).findFirst();
        assertTrue(optional.isPresent());
        MapLocation mapLocation = optional.get();
        assertTrue(mapLocation.isIndirect(map.asMapLocation(target)));
    }
}
