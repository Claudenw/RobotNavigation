package org.xenei.robot.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
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

    private ArgumentCaptor<Coordinate> coordinateCaptor = ArgumentCaptor.forClass(Coordinate.class);
    private ArgumentCaptor<Step> stepCaptor = ArgumentCaptor.forClass(Step.class);
    private ArgumentCaptor<Set> setCaptor = ArgumentCaptor.forClass(Set.class);
    private ArgumentCaptor<Double> doubleCaptor = ArgumentCaptor.forClass(Double.class);
    private ArgumentCaptor<Set<Obstacle>> obstacleSetCaptor = ArgumentCaptor.forClass(Set.class);
    private ArgumentCaptor<Obstacle> obstacleCaptor = ArgumentCaptor.forClass(Obstacle.class);

    private RobutContext ctxt = new RobutContext(ScaleInfo.DEFAULT, TestChassisInfo.DEFAULT);

    @Test
    public void processSensorDataTest_TooClose() {

        Position currentPosition = Position.from(-1, -3, AngleUtils.RADIANS_90);
        Coordinate target = new Coordinate(-1, 1);
        Obstacle obstacle = Mockito.mock(Obstacle.class);
        Coordinate mapValue = new Coordinate(5, 5);
        Map map = Mockito.mock(Map.class);
        when(map.getContext()).thenReturn(ctxt);
        when(map.createObstacle(any(), any())).thenReturn(obstacle);
        when(map.addObstacle(any())).thenReturn(Set.of(obstacle));
        when(map.adopt(any())).thenReturn(mapValue);
        Mapper underTest = new MapperImpl(map);

        // an obstacle one unit away is too close so no target generated.
        Location[] obstacles = { Location.from(CoordUtils.fromAngle(0, 1)) };
        NavigationSnapshot snapshot = new NavigationSnapshot(currentPosition, target);
        Collection<Step> result = underTest.processSensorData(target, snapshot, obstacles);
        // should not look at result here.
        assertTrue(result.isEmpty());

        verify(map).updateIsIndirect(coordinateCaptor.capture(), setCaptor.capture());
        assertEquals(1, setCaptor.getValue().size());
        assertEquals(obstacle, setCaptor.getValue().iterator().next());

        // verify obstacle was added
        verify(map).addObstacle(obstacleCaptor.capture());
        assertEquals(obstacle, obstacleCaptor.getValue());

    }

    @Test
    public void processSensorDataTest_IntermediateTarget() {

        Position currentPosition = Position.from(-1, -3, AngleUtils.RADIANS_90);
        Coordinate target = new Coordinate(-1, 1);
        Step step = Mockito.mock(Step.class);

        Obstacle obstacle = Mockito.mock(Obstacle.class);
        Map map = Mockito.mock(Map.class);
        when(map.getContext()).thenReturn(ctxt);
        when(map.createObstacle(any(), any())).thenReturn(obstacle);
        when(map.addObstacle(any())).thenReturn(Set.of(obstacle));
        when(map.adopt(any())).thenReturn(new Coordinate(-1, -2));
        when(map.isObstacle(any())).thenReturn(false);
        when(map.addCoord(any(), anyDouble(), anyBoolean(), anyBoolean())).thenReturn(Optional.of(step));
        Mapper underTest = new MapperImpl(map);

        Location[] obstacles = { Location.from(CoordUtils.fromAngle(0, 2)) };
        NavigationSnapshot snapshot = new NavigationSnapshot(currentPosition, target);
        Collection<Step> result = underTest.processSensorData(target, snapshot, obstacles);
        assertFalse(result.isEmpty());

        // verify indirects updated
        verify(map).updateIsIndirect(coordinateCaptor.capture(), setCaptor.capture());
        assertEquals(1, setCaptor.getValue().size());
        assertEquals(obstacle, setCaptor.getValue().iterator().next());

        // verify obstacle was added
        verify(map).addObstacle(obstacleCaptor.capture());
        assertEquals(obstacle, obstacleCaptor.getValue());

        // verify coord was added
        ArgumentCaptor<Boolean> one = ArgumentCaptor.forClass(Boolean.class);
        ArgumentCaptor<Boolean> two = ArgumentCaptor.forClass(Boolean.class);
        verify(map).addCoord(coordinateCaptor.capture(), doubleCaptor.capture(), one.capture(), two.capture());
        CoordinateUtils.assertEquivalent(new Coordinate(-1, -2), coordinateCaptor.getValue());
    }

    @Test
    public void processSensorDataTest_NoTarget() {

        Position currentPosition = Position.from(-1, -3, AngleUtils.RADIANS_90);
        Step step = Mockito.mock(Step.class);

        Obstacle obstacle = Mockito.mock(Obstacle.class);
        Map map = Mockito.mock(Map.class);
        when(map.getContext()).thenReturn(ctxt);
        when(map.createObstacle(any(), any())).thenReturn(obstacle);
        when(map.addObstacle(any())).thenReturn(Set.of(obstacle));
        when(map.adopt(any())).thenReturn(new Coordinate(-1, -2));
        when(map.isObstacle(any())).thenReturn(false);
        when(map.addCoord(any(), anyDouble(), anyBoolean(), anyBoolean())).thenReturn(Optional.of(step));
        Mapper underTest = new MapperImpl(map);

        Location[] obstacles = { Location.from(CoordUtils.fromAngle(0, 2)) };
        NavigationSnapshot snapshot = new NavigationSnapshot(currentPosition, null);
        Collection<Step> result = underTest.processSensorData(null, snapshot, obstacles);
        assertTrue(result.isEmpty());

        // verify no indirects updated
        verify(map, times(0)).updateIsIndirect(coordinateCaptor.capture(), setCaptor.capture());

        // verify obstacle was added
        verify(map).addObstacle(obstacleCaptor.capture());
        assertEquals(obstacle, obstacleCaptor.getValue());

        // verify coord was added
        ArgumentCaptor<Boolean> one = ArgumentCaptor.forClass(Boolean.class);
        ArgumentCaptor<Boolean> two = ArgumentCaptor.forClass(Boolean.class);
        verify(map).addCoord(coordinateCaptor.capture(), doubleCaptor.capture(), one.capture(), two.capture());
        CoordinateUtils.assertEquivalent(new Coordinate(-1, -2), coordinateCaptor.getValue());
    }
    
    @Test
    public void processSensorDataTest_noSensorReadings() {

        Position currentPosition = Position.from(-1, -3, AngleUtils.RADIANS_90);
        Coordinate target = new Coordinate(-1, 1);
        Step step = Mockito.mock(Step.class);

        Obstacle obstacle = Mockito.mock(Obstacle.class);
        Map map = Mockito.mock(Map.class);
        when(map.getContext()).thenReturn(ctxt);
        when(map.createObstacle(any(), any())).thenReturn(obstacle);
        when(map.addObstacle(any())).thenReturn(Set.of(obstacle));
        when(map.adopt(any())).thenReturn(new Coordinate(-1, -2));
        when(map.isObstacle(any())).thenReturn(false);
        when(map.addCoord(any(), anyDouble(), anyBoolean(), anyBoolean())).thenReturn(Optional.of(step));
        Mapper underTest = new MapperImpl(map);

        NavigationSnapshot snapshot = new NavigationSnapshot(currentPosition, target);
        Collection<Step> result = underTest.processSensorData(null, snapshot, new Location[] {});
        assertTrue(result.isEmpty());

        // verify no indirects updated
        verify(map, times(0)).updateIsIndirect(coordinateCaptor.capture(), setCaptor.capture());

        // verify no obstacle was added
        verify(map, times(0)).addObstacle(obstacleCaptor.capture());


        // verify not coord was added
        ArgumentCaptor<Boolean> one = ArgumentCaptor.forClass(Boolean.class);
        ArgumentCaptor<Boolean> two = ArgumentCaptor.forClass(Boolean.class);
        verify(map, times(0)).addCoord(coordinateCaptor.capture(), doubleCaptor.capture(), one.capture(), two.capture());
    }
}
