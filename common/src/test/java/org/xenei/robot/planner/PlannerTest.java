package org.xenei.robot.planner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.ScaleInfo;
import org.xenei.robot.common.mapping.CoordinateMap;
import org.xenei.robot.common.mapping.Map;
import org.xenei.robot.common.planning.Planner;
import org.xenei.robot.common.planning.Planner.Diff;
import org.xenei.robot.common.planning.Solution;
import org.xenei.robot.common.planning.Step;
import org.xenei.robot.common.testUtils.CoordinateUtils;
import org.xenei.robot.common.testUtils.MapLibrary;
import org.xenei.robot.mapper.MapImpl;

public class PlannerTest {

    private Planner underTest;
    private final double buffer = 0.5;

    private ArgumentCaptor<Coordinate> coordinateCaptor = ArgumentCaptor.forClass(Coordinate.class);
    //private ArgumentCaptor<Step> stepCaptor = ArgumentCaptor.forClass(Step.class);
    private ArgumentCaptor<Double> doubleCaptor = ArgumentCaptor.forClass(Double.class);

    @Test
    public void setTargetTest() {
        CoordinateMap cmap = MapLibrary.map2('#');
        Map map = new MapImpl(ScaleInfo.DEFAULT);
        Location origin = Location.from(0, 0);
        underTest = new PlannerImpl(map, origin, buffer);
        for (int x = 0; x <= 13; x++) {
            for (int y = 0; y <= 15; y++) {
                Location c = Location.from(x, y);
                if (!cmap.isEnabled(c)) {
                    underTest.setTarget(c);
                    verifyState(map, cmap);
                    assertEquals(0, underTest.getSolution().stepCount());
                }
            }
        }
    }

    private Function<Geometry, Coordinate> toCoord = g -> {
        Point p = g.getCentroid();
        return new Coordinate(p.getX(), p.getY());
    };

    private CoordinateMap verifyState(Map map, CoordinateMap cmap) {

        for (Geometry c : map.getObstacles()) {
            assertTrue(cmap.isEnabled(toCoord.apply(c)), () -> c + " should have been sensed.");
        }
        CoordinateMap sensedMap = new CoordinateMap(cmap.scale());
        sensedMap.enable(map.getObstacles().stream().map(toCoord).collect(Collectors.toList()), 'x');

        for (Step pr : map.getTargets()) {
            assertFalse(sensedMap.isEnabled(pr), () -> "Plan record " + pr + " should not have been sensed");
        }

        underTest.getSolution().stream().forEach(
                c -> assertFalse(sensedMap.isEnabled(c), () -> "Path record " + c + " should not have been enabled"));
        return sensedMap;
    }

    @Test
    public void changeCurrentPositionTest() {
        Map map = Mockito.mock(Map.class);
        when(map.getScale()).thenReturn(ScaleInfo.DEFAULT);

        Location finalCoord = Location.from(-1, 1);
        Location startCoord = Location.from(-1, -3);
        underTest = new PlannerImpl(map, startCoord, buffer, finalCoord);
        Diff diff = underTest.getDiff();
        assertFalse(diff.didChange());
        Position p = Position.from(1, 1);
        // since there is ony one target this will add a position to the target stack
        underTest.changeCurrentPosition(p);
        assertTrue(diff.didChange());
        assertTrue(diff.didPositionChange());

        //map.addTarget(p.getCoordinate(), p.distance(underTest.getTarget()));
        verify(map, times(2)).addCoord(coordinateCaptor.capture(), doubleCaptor.capture(), false, false);
        List<Coordinate> lst = coordinateCaptor.getAllValues();
        assertTrue(startCoord.equals2D(lst.get(0)));
        assertTrue(p.equals2D(lst.get(1)));

        // verify solution has 2 items
        Solution solution = underTest.getSolution();
        List<Coordinate> sol = solution.stream().collect(Collectors.toList());
        assertEquals(2, sol.size());
        assertTrue(startCoord.equals2D(sol.get(0)));
        assertTrue(p.equals2D(lst.get(1)));
    }

    @Test
    public void constructorTest() {
        Map map = Mockito.mock(Map.class);
        when(map.getScale()).thenReturn(ScaleInfo.DEFAULT);

        Location finalCoord = Location.from(-1, 1);
        Location startCoord = Location.from(-1, -3);
        underTest = new PlannerImpl(map, startCoord, buffer, finalCoord);
        Diff diff = underTest.getDiff();
        assertFalse(diff.didChange());

        // verify addTarget called
        verify(map, times(1)).addCoord(coordinateCaptor.capture(), doubleCaptor.capture(), false, false);
        assertTrue(startCoord.equals2D(coordinateCaptor.getValue()));

        // verify recalculate called
        verify(map).recalculate(coordinateCaptor.capture(), buffer);
        assertTrue(finalCoord.equals2D(coordinateCaptor.getValue()));

        // verify solution has 1 item
        Solution solution = underTest.getSolution();
        List<Coordinate> sol = solution.stream().collect(Collectors.toList());
        assertEquals(1, sol.size());
        assertTrue(startCoord.equals2D(sol.get(0)));
    }

    @Test
    public void targetTest() {
        Map map = Mockito.mock(Map.class);
        when(map.getScale()).thenReturn(ScaleInfo.DEFAULT);

        Location finalCoord = Location.from(-1, 1);
        Location startCoord = Location.from(-1, -3);
        Coordinate newTarget = new Coordinate(4, 4);
        underTest = new PlannerImpl(map, startCoord, buffer, finalCoord);
        // this should make 2 targets
        underTest.replaceTarget(newTarget);
        Diff diff = underTest.getDiff();
        assertTrue(diff.didChange());
        assertTrue(diff.didTargetChange());
        assertTrue(newTarget.equals2D(underTest.getTarget()));
        assertEquals(2, underTest.getTargets().size());
        assertTrue(finalCoord.equals2D(underTest.getRootTarget()));

        // this should continue with 2 targets
        newTarget = new Coordinate(5, 5);
        // this should make 2 targets
        underTest.replaceTarget(newTarget);
        assertTrue(newTarget.equals2D(underTest.getTarget()));
        assertEquals(2, underTest.getTargets().size());
        assertTrue(finalCoord.equals2D(underTest.getRootTarget()));

        // verify solution has 1 item
        Solution solution = underTest.getSolution();
        List<Coordinate> sol = solution.stream().collect(Collectors.toList());
        assertEquals(1, sol.size());
        assertTrue(startCoord.equals2D(sol.get(0)));
    }

    @Test
    public void listenersTest() {
        int[] result = { 0 };
        Map map = Mockito.mock(Map.class);

        Location finalCoord = Location.from(-1, 1);
        Location startCoord = Location.from(-1, -3);
        underTest = new PlannerImpl(map, startCoord, buffer, finalCoord);
        underTest.addListener(() -> result[0]++);
        underTest.notifyListeners();
        assertEquals(1, result[0]);
        verify(map).recalculate(coordinateCaptor.capture(), buffer);
        assertTrue(finalCoord.equals2D(coordinateCaptor.getValue()));
    }

    @Test
    public void recalculateCostsTest() {
        Map map = Mockito.mock(Map.class);

        Location finalCoord = Location.from(-1, 1);
        Location startCoord = Location.from(-1, -3);
        underTest = new PlannerImpl(map, startCoord, buffer, finalCoord);
        Coordinate newTarget = new Coordinate(4, 4);
        underTest.replaceTarget(newTarget);
        underTest.recalculateCosts();

        // verify recalculate was called once for each target
        verify(map, times(2)).recalculate(coordinateCaptor.capture(), buffer);
        List<Coordinate> lst = coordinateCaptor.getAllValues();
        assertTrue(finalCoord.equals2D(lst.get(0)));
        assertTrue(newTarget.equals2D(lst.get(1)));

        // verify setTemporaryCost was called once
        verify(map).setTemporaryCost(coordinateCaptor.capture(), doubleCaptor.capture());
        assertTrue(startCoord.equals2D(coordinateCaptor.getValue()));
        assertEquals(Double.POSITIVE_INFINITY,doubleCaptor.getValue());

        // verify solution has 1 item
        Solution solution = underTest.getSolution();
        List<Coordinate> sol = solution.stream().collect(Collectors.toList());
        assertEquals(1, sol.size());
        assertTrue(startCoord.equals2D(sol.get(0)));
    }

    @Test
    public void restartTest() {
        Map map = Mockito.mock(Map.class);
        when(map.getScale()).thenReturn(ScaleInfo.DEFAULT);

        Location finalCoord = Location.from(-1, 1);
        Location startCoord = Location.from(-1, -3);
        Location nextStart = Location.from(4, 4);
        underTest = new PlannerImpl(map, startCoord, buffer, finalCoord);
        underTest.restart(nextStart);
        Diff diff = underTest.getDiff();
        assertFalse(diff.didChange());

        // verify current position changed
        assertTrue(nextStart.equals2D(underTest.getCurrentPosition()));

        // verify addTarget called
        verify(map, times(2)).addCoord(coordinateCaptor.capture(), doubleCaptor.capture(), false, false);
        List<Coordinate> coords = coordinateCaptor.getAllValues();
        assertTrue(startCoord.equals2D(coords.get(0)));
        assertTrue(nextStart.equals2D(coords.get(1)));

        // verify solution has 1 item
        Solution solution = underTest.getSolution();
        List<Coordinate> sol = solution.stream().collect(Collectors.toList());
        assertEquals(1, sol.size());
        assertTrue(nextStart.equals2D(sol.get(0)));
    }

    @Test
    public void getPlanRecordsTest() {
        Map map = Mockito.mock(Map.class);
        when(map.getScale()).thenReturn(ScaleInfo.DEFAULT);
        when(map.getTargets()).thenReturn(Collections.emptyList());

        Location finalCoord = Location.from(-1, 1);
        Location startCoord = Location.from(-1, -3);
        underTest = new PlannerImpl(map, startCoord, buffer, finalCoord);
        underTest.getPlanRecords();

        // verify we got them from the map
        verify(map).getTargets();
    }

    @Test
    public void selectTargetTest() {
        Location finalCoord = Location.from(-1, 1);
        Location startCoord = Location.from(-1, -3);
        Location stepLocation = Location.from(3, 3);
        Step step = mock(Step.class);
        when(step.getCoordinate()).thenReturn(stepLocation.getCoordinate());
        when(step.cost()).thenReturn(Double.valueOf(5));


        Map map = Mockito.mock(Map.class);
        when(map.getScale()).thenReturn(ScaleInfo.DEFAULT);
        when(map.getBestStep(any(), any())).thenReturn(Optional.of(step)).thenReturn(Optional.empty());
        underTest = new PlannerImpl(map, startCoord, buffer, finalCoord);

        // first target (step)
        Diff diff = underTest.selectTarget();

        assertTrue(diff.didChange());
        assertTrue(diff.didTargetChange());

        CoordinateUtils.assertEquivalent(step, underTest.getTarget());
        Solution solution = underTest.getSolution();
        CoordinateUtils.assertEquivalent(startCoord, solution.start());
        CoordinateUtils.assertEquivalent(startCoord, solution.end());
        assertEquals(0, solution.stepCount());

        // second target (empty)
        diff.reset();
        diff = underTest.selectTarget();
        assertFalse(diff.didChange());
        CoordinateUtils.assertEquivalent(step, underTest.getTarget());
        solution = underTest.getSolution();
        CoordinateUtils.assertEquivalent(startCoord, solution.start());
        CoordinateUtils.assertEquivalent(startCoord, solution.end());
        assertEquals(0, solution.stepCount());

        // change the position to current target location.
        underTest.changeCurrentPosition(Position.from(step));
        diff.reset();
        diff = underTest.selectTarget();
        assertTrue(diff.didChange());
        CoordinateUtils.assertEquivalent(finalCoord, underTest.getTarget());
        solution = underTest.getSolution();
        CoordinateUtils.assertEquivalent(startCoord, solution.start());
        CoordinateUtils.assertEquivalent(stepLocation, solution.end());
        assertEquals(1, solution.stepCount());

        // chhange the position to the end location
        underTest.changeCurrentPosition(Position.from(finalCoord));
        diff.reset();
        diff = underTest.selectTarget();
        assertTrue(diff.didChange());
        assertNull(underTest.getTarget());
        solution = underTest.getSolution();
        CoordinateUtils.assertEquivalent(startCoord, solution.start());
        CoordinateUtils.assertEquivalent(finalCoord, solution.end());
        assertEquals(2, solution.stepCount());
    }

}