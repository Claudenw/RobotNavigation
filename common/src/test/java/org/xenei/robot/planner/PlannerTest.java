package org.xenei.robot.planner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.xenei.robot.common.FrontsCoordinate;
import org.xenei.robot.common.FrontsCoordinateTest;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.ScaleInfo;
import org.xenei.robot.common.UnmodifiableCoordinate;
import org.xenei.robot.common.mapping.Map;
import org.xenei.robot.common.planning.Planner;
import org.xenei.robot.common.planning.Planner.Diff;
import org.xenei.robot.common.planning.Solution;
import org.xenei.robot.common.planning.Step;
import org.xenei.robot.common.testUtils.CoordinateUtils;
import org.xenei.robot.common.testUtils.TestChassisInfo;
import org.xenei.robot.common.testUtils.TestingPositionSupplier;
import org.xenei.robot.common.utils.AngleUtils;
import org.xenei.robot.common.utils.RobutContext;

public class PlannerTest {
    private RobutContext ctxt = new RobutContext(ScaleInfo.DEFAULT, TestChassisInfo.DEFAULT);
    private Planner underTest;


    private ArgumentCaptor<Coordinate> coordinateCaptor = ArgumentCaptor.forClass(Coordinate.class);
    // private ArgumentCaptor<Step> stepCaptor =
    // ArgumentCaptor.forClass(Step.class);
    private ArgumentCaptor<Double> doubleCaptor = ArgumentCaptor.forClass(Double.class);

    @Test
    public void setTargetTest() {
        FrontsCoordinate fc = FrontsCoordinateTest.make(1, 1);
        Map map = Mockito.mock(Map.class);

        TestingPositionSupplier supplier = new TestingPositionSupplier(Position.from(Location.ORIGIN));
        underTest = new PlannerImpl(map, supplier);
        
        assertEquals(AngleUtils.RADIANS_45, underTest.setTarget(fc));
        assertEquals(fc.getCoordinate(), underTest.getTarget());
        verify(map).recalculate(coordinateCaptor.capture());
        assertEquals(fc.getCoordinate(), coordinateCaptor.getValue());
        Solution solution = underTest.getSolution();
        assertEquals(0, solution.stepCount());
        assertEquals(Location.ORIGIN.getCoordinate(), solution.start());
    }

    @Test
    public void registerPositionChangeTest() {
        Step step = Mockito.mock(Step.class);
        when(step.getCoordinate()).thenReturn(UnmodifiableCoordinate.make(new Coordinate(1, 1)));
        Map map = Mockito.mock(Map.class);
        when(map.getContext()).thenReturn(ctxt);
        when(map.addCoord(any(Coordinate.class), anyDouble(), anyBoolean(), anyBoolean())).thenReturn(Optional.of(step));

        Location finalCoord = Location.from(-1, 1);
        Position initial = Position.from(-1, -3);
        TestingPositionSupplier supplier = new TestingPositionSupplier(initial);
        underTest = new PlannerImpl(map, supplier, finalCoord);
        Diff diff = underTest.getDiff();
        assertFalse(diff.didChange());
        Position second = Position.from(1, 1);
        supplier.position = second;
        
        // since there is only one target this will add a position to the target stack
        underTest.registerPositionChange();
        assertTrue(diff.didChange());
        assertTrue(diff.didPositionChange());

        // map.addTarget(p.getCoordinate(), p.distance(underTest.getTarget()));
        verify(map, times(2)).addCoord(coordinateCaptor.capture(), doubleCaptor.capture(), anyBoolean(), anyBoolean());
        List<Coordinate> lst = coordinateCaptor.getAllValues();
        assertTrue(initial.equals2D(lst.get(0)));
        assertTrue(second.equals2D(lst.get(1)));

        // verify solution has 2 items
        Solution solution = underTest.getSolution();
        List<Coordinate> sol = solution.stream().collect(Collectors.toList());
        assertEquals(2, sol.size());
        assertTrue(initial.equals2D(sol.get(0)));
        assertTrue(second.equals2D(lst.get(1)));
    }

    @Test
    public void constructorTest() {
        Step step = Mockito.mock(Step.class);
        Map map = Mockito.mock(Map.class);
        when(map.getContext()).thenReturn(ctxt);
        when(map.addCoord(any(Coordinate.class), anyDouble(), anyBoolean(), anyBoolean())).thenReturn(Optional.of(step));

        Location finalCoord = Location.from(-1, 1);
        TestingPositionSupplier supplier = new TestingPositionSupplier(Position.from(-1, -3));
        underTest = new PlannerImpl(map, supplier, finalCoord);
        Diff diff = underTest.getDiff();
        assertFalse(diff.didChange());

        // verify addTarget called
        verify(map, times(1)).addCoord(coordinateCaptor.capture(), doubleCaptor.capture(), anyBoolean(), anyBoolean());
        assertTrue(supplier.position.equals2D(coordinateCaptor.getValue()));

        // verify recalculate called
        verify(map).recalculate(coordinateCaptor.capture());
        assertTrue(finalCoord.equals2D(coordinateCaptor.getValue()));

        // verify solution has 1 item
        Solution solution = underTest.getSolution();
        List<Coordinate> sol = solution.stream().collect(Collectors.toList());
        assertEquals(1, sol.size());
        assertTrue(supplier.position.equals2D(sol.get(0)));
    }

    @Test
    public void targetTest() {
        Map map = Mockito.mock(Map.class);
        when(map.getContext()).thenReturn(ctxt);

        Location finalCoord = Location.from(-1, 1);
        Coordinate newTarget = new Coordinate(4, 4);
        Position initial = Position.from(-1, -3);
        TestingPositionSupplier supplier = new TestingPositionSupplier(initial);
        underTest = new PlannerImpl(map, supplier, finalCoord);
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
        assertTrue(initial.equals2D(sol.get(0)));
    }

    @Test
    public void listenersTest() {
        int[] result = { 0 };
        Map map = Mockito.mock(Map.class);

        Location finalCoord = Location.from(-1, 1);
        TestingPositionSupplier supplier = new TestingPositionSupplier(Position.from(-1, -3));
        underTest = new PlannerImpl(map, supplier, finalCoord);
        underTest.addListener(() -> result[0]++);
        underTest.notifyListeners();
        assertEquals(1, result[0]);
        verify(map).recalculate(coordinateCaptor.capture());
        assertTrue(finalCoord.equals2D(coordinateCaptor.getValue()));
    }

    @Test
    public void recalculateCostsTest() {
        Map map = Mockito.mock(Map.class);

        Location finalCoord = Location.from(-1, 1);
        Position initial = Position.from(-1, -3);
        TestingPositionSupplier supplier = new TestingPositionSupplier(initial);
        underTest = new PlannerImpl(map, supplier, finalCoord);
        Coordinate newTarget = new Coordinate(4, 4);
        underTest.replaceTarget(newTarget);
        underTest.recalculateCosts();

        // verify recalculate was called once for each target
        verify(map, times(2)).recalculate(coordinateCaptor.capture());
        List<Coordinate> lst = coordinateCaptor.getAllValues();
        assertTrue(finalCoord.equals2D(lst.get(0)));
        assertTrue(newTarget.equals2D(lst.get(1)));

        // verify setTemporaryCost was called once
        verify(map, times(2)).recalculate(coordinateCaptor.capture());
        assertTrue(newTarget.equals2D(coordinateCaptor.getValue()));
        
        // verify solution has 1 item
        Solution solution = underTest.getSolution();
        List<Coordinate> sol = solution.stream().collect(Collectors.toList());
        assertEquals(1, sol.size());
        assertTrue(initial.equals2D(sol.get(0)));
    }

    @Test
    public void selectTargetTest() {
        Location finalCoord = Location.from(-1, 1);
        Location stepLocation = Location.from(3, 3);
        Step step = mock(Step.class);
        when(step.getCoordinate()).thenReturn(stepLocation.getCoordinate());
        when(step.cost()).thenReturn(Double.valueOf(5));

        Map map = Mockito.mock(Map.class);
        when(map.getContext()).thenReturn(ctxt);
        when(map.addCoord(any(Coordinate.class), anyDouble(), anyBoolean(), anyBoolean())).thenReturn(Optional.of(step));

        when(map.getBestStep(any())).thenReturn(Optional.of(step)).thenReturn(Optional.empty());
        Position initial = Position.from(-1, -3);
        TestingPositionSupplier supplier = new TestingPositionSupplier(initial);
        underTest = new PlannerImpl(map, supplier, finalCoord);;

        // first target (step)
        Diff diff = underTest.selectTarget();

        assertTrue(diff.didChange());
        assertTrue(diff.didTargetChange());

        CoordinateUtils.assertEquivalent(step, underTest.getTarget());
        Solution solution = underTest.getSolution();
        CoordinateUtils.assertEquivalent(initial, solution.start());
        CoordinateUtils.assertEquivalent(initial, solution.end());
        assertEquals(0, solution.stepCount());

        // second target (empty)
        diff.reset();
        diff = underTest.selectTarget();
        assertFalse(diff.didChange());
        CoordinateUtils.assertEquivalent(step, underTest.getTarget());
        solution = underTest.getSolution();
        CoordinateUtils.assertEquivalent(initial, solution.start());
        CoordinateUtils.assertEquivalent(initial, solution.end());
        assertEquals(0, solution.stepCount());

        // change the position to current target location.
        supplier.position = Position.from(step);
        underTest.registerPositionChange();
        diff.reset();
        diff = underTest.selectTarget();
        assertTrue(diff.didChange());
        CoordinateUtils.assertEquivalent(finalCoord, underTest.getTarget());
        solution = underTest.getSolution();
        CoordinateUtils.assertEquivalent(initial, solution.start());
        CoordinateUtils.assertEquivalent(stepLocation, solution.end());
        assertEquals(1, solution.stepCount());

        // chhange the position to the end location
        supplier.position = Position.from(finalCoord);
        underTest.registerPositionChange();
        diff.reset();
        diff = underTest.selectTarget();
        assertTrue(diff.didChange());
        assertNull(underTest.getTarget());
        solution = underTest.getSolution();
        CoordinateUtils.assertEquivalent(initial, solution.start());
        CoordinateUtils.assertEquivalent(finalCoord, solution.end());
        assertEquals(2, solution.stepCount());
    }
}
