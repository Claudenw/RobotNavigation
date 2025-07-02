package org.xenei.robot.mapper;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.locationtech.jts.geom.Coordinate;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.xenei.robot.common.DistanceSensor;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.NavigationSnapshot;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.ScaleInfo;
import org.xenei.robot.common.mapping.Map;
import org.xenei.robot.common.mapping.Mapper;
import org.xenei.robot.common.mapping.Obstacle;
import org.xenei.robot.common.planning.Step;
import org.xenei.robot.common.testUtils.CoordinateUtils;
import org.xenei.robot.common.testUtils.TestChassisInfo;
import org.xenei.robot.common.utils.AngleUtils;
import org.xenei.robot.common.utils.CoordUtils;
import org.xenei.robot.common.utils.RobutContext;

public class MapperImplTest {

    private final ArgumentCaptor<Coordinate> coordinateCaptor = ArgumentCaptor.forClass(Coordinate.class);
    private final ArgumentCaptor<Obstacle> obstacleCaptor = ArgumentCaptor.forClass(Obstacle.class);

    private RobutContext ctxt = new RobutContext(ScaleInfo.DEFAULT, TestChassisInfo.DEFAULT);

    @Test
    public void processSensorDataTest_TooClose() {

        Position currentPosition = Position.from(-1, -3, AngleUtils.RADIANS_90);
        Coordinate target = new Coordinate(-1, 1);
        Obstacle obstacle = Mockito.mock(Obstacle.class);
        Coordinate mapValue = new Coordinate(5, 5);

        Map map = Mockito.mock(Map.class);
        when(map.getContext()).thenReturn(ctxt);
//        when(map.createObstacle(any(), any())).thenReturn(obstacle);
//        when(map.addObstacle(any())).thenReturn(CompletableFuture.completedFuture(Set.of(obstacle)));
//        when(map.adopt(any())).thenReturn(mapValue);
        Mapper underTest = new MapperImpl(map, () -> Position.from(mapValue), () -> target);
        RelativeLocationDistanceSensorAdapter relativeLocationDistanceSensorAdapter =
                new RelativeLocationDistanceSensorAdapter(underTest.getRelativeObstacleConsumer());

        // an obstacle one unit away is too close so no target generated.
        relativeLocationDistanceSensorAdapter.accept(DistanceSensor.DistanceReading.from(CoordUtils.fromAngle(0, 1)));

        Location[] obstacles = { Location.from(CoordUtils.fromAngle(0, 1)) };
        NavigationSnapshot snapshot = new NavigationSnapshot(currentPosition, target);

        verify(map, times(0)).isObstacle(any(Coordinate.class));
        verify(map, times(0)).addCoord(any(Coordinate.class), any(Coordinate.class), anyBoolean());
    }


    private static Stream<Arguments> sensorData() {
        List<Arguments> lst = new ArrayList<>();
        Coordinate sensorReading = CoordUtils.fromAngle(0, 2);
        // results are the center of the cell.
        lst.add(Arguments.of(0, sensorReading, new Coordinate(1.5, 0))); // along x coord
        lst.add(Arguments.of(90, sensorReading, new Coordinate(0, 1.5))); // down Y ccoord
        lst.add(Arguments.of(180, sensorReading, new Coordinate(-1.5, 0))); // backwards on x coord
        lst.add(Arguments.of(270, sensorReading, new Coordinate(0, -1.5))); //up Y coord
        return lst.stream();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("sensorData")
    void processSensorDataTest(final double degrees, final Coordinate sensorReading, final Coordinate candidate) {

        Position currentPosition = Position.from(-0, 0, Math.toRadians(degrees));
        Coordinate target = new Coordinate(10, 10);
        Step step = Mockito.mock(Step.class);

        Obstacle obstacle = Mockito.mock(Obstacle.class);
        Map map = Mockito.mock(Map.class);
        when(map.getContext()).thenReturn(ctxt);
        when(map.createObstacle(any(Position.class), any(Location.class))).thenReturn(obstacle);
        when(map.addObstacle(any())).thenReturn(CompletableFuture.completedFuture(Set.of(obstacle)));
        when(map.adopt(any(Coordinate.class))).thenAnswer( context -> {
            return Map.adopt(context.getArgument(0, Coordinate.class), ctxt.scaleInfo);
        });
        when(map.isObstacle(any(Coordinate.class))).thenReturn(false);
        when(map.addCoord(any(Coordinate.class), any(Coordinate.class), anyBoolean()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(step)));
        when(map.isClearPath(any(Coordinate.class), any(Coordinate.class))).thenReturn(false);

        Mapper underTest = new MapperImpl(map, () -> currentPosition, () -> target);
        RelativeLocationDistanceSensorAdapter relativeLocationDistanceSensorAdapter =
                new RelativeLocationDistanceSensorAdapter(underTest.getRelativeObstacleConsumer());
        // process data
        relativeLocationDistanceSensorAdapter.accept(DistanceSensor.DistanceReading.from(CoordUtils.fromAngle(0, 2)));


        ArgumentCaptor<Boolean> one = ArgumentCaptor.forClass(Boolean.class);
        ArgumentCaptor<Coordinate> targetCaptor = ArgumentCaptor.forClass(Coordinate.class);
        Callable<Boolean> mockitoTest = () -> {
            try {
                verify(map).addCoord(coordinateCaptor.capture(), targetCaptor.capture(), one.capture());
                return true;
            }
            catch(AssertionError ae) {
                return false;
            }
        };
        await().atMost(5, SECONDS).until(mockitoTest);

        verify(map).isObstacle(coordinateCaptor.capture());
        CoordinateUtils.assertEquivalent(candidate, coordinateCaptor.getValue());

        // verify obstacle was added
        verify(map).addObstacle(obstacleCaptor.capture());
        assertEquals(obstacle, obstacleCaptor.getValue());

        // verify coord was added
        verify(map).addCoord(coordinateCaptor.capture(), targetCaptor.capture(), one.capture());
        CoordinateUtils.assertEquivalent(candidate, coordinateCaptor.getValue());
    }
}
