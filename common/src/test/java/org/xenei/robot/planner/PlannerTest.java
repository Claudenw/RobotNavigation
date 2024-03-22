package org.xenei.robot.planner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.jena.rdf.model.Resource;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.xenei.robot.common.FrontsCoordinate;
import org.xenei.robot.common.FrontsCoordinateTest;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.NavigationSnapshot;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.ScaleInfo;
import org.xenei.robot.common.UnmodifiableCoordinate;
import org.xenei.robot.common.mapping.Map;
import org.xenei.robot.common.mapping.MapCoord;
import org.xenei.robot.common.mapping.Obstacle;
import org.xenei.robot.common.planning.Planner;
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
        when(map.addCoord(any(Coordinate.class), anyDouble(), anyBoolean(), anyBoolean()))
                .thenReturn(Optional.of(step));

        Location finalLocation = Location.from(-1, 1);
        Position initial = Position.from(-1, -3);

        TestingPositionSupplier supplier = new TestingPositionSupplier(initial);
        underTest = new PlannerImpl(map, supplier, finalLocation);
        NavigationSnapshot lastSnapshot = new NavigationSnapshot(initial, finalLocation.getCoordinate());
        Position second = Position.from(1, 1);

        NavigationSnapshot snapshot = new NavigationSnapshot(second, finalLocation.getCoordinate());
        // since there is only one target this will add a position to the target stack
        underTest.registerPositionChange(snapshot);
        assertTrue(lastSnapshot.didChange(snapshot));

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
        when(map.addCoord(any(Coordinate.class), anyDouble(), anyBoolean(), anyBoolean()))
                .thenReturn(Optional.of(step));

        Location finalLocation = Location.from(-1, 1);
        Position initial = Position.from(-1, -3);
        TestingPositionSupplier supplier = new TestingPositionSupplier(initial);
        NavigationSnapshot initialSnapshot = new NavigationSnapshot(initial, finalLocation.getCoordinate());

        underTest = new PlannerImpl(map, supplier, finalLocation);
        NavigationSnapshot snapshot = underTest.getSnapshot();
        assertFalse(snapshot.didChange(initialSnapshot));

        // verify addTarget called
        verify(map, times(1)).addCoord(coordinateCaptor.capture(), doubleCaptor.capture(), anyBoolean(), anyBoolean());
        assertTrue(supplier.position.equals2D(coordinateCaptor.getValue()));

        // verify recalculate called
        verify(map).recalculate(coordinateCaptor.capture());
        assertTrue(finalLocation.equals2D(coordinateCaptor.getValue()));

        // verify solution has 1 item
        Solution solution = underTest.getSolution();
        List<Coordinate> sol = solution.stream().collect(Collectors.toList());
        assertEquals(1, sol.size());
        assertTrue(supplier.position.equals2D(sol.get(0)));
    }

    @Test
    public void replaceTargetTest() {
        Map map = Mockito.mock(Map.class);
        when(map.getContext()).thenReturn(ctxt);

        Location finalLocation = Location.from(-1, 1);
        Coordinate newTarget = new Coordinate(4, 4);
        Position initial = Position.from(-1, -3);
        TestingPositionSupplier supplier = new TestingPositionSupplier(initial);
        NavigationSnapshot initialSnapshot = new NavigationSnapshot(initial, finalLocation.getCoordinate());

        underTest = new PlannerImpl(map, supplier, finalLocation);
        NavigationSnapshot snapshot = underTest.getSnapshot();
        assertFalse(initialSnapshot.didChange(snapshot));

        underTest.replaceTarget(newTarget);
        snapshot = underTest.getSnapshot();
        assertTrue(initialSnapshot.didTargetChange(snapshot));
        assertTrue(newTarget.equals2D(underTest.getTarget()));
        assertEquals(2, underTest.getTargets().size());
        assertTrue(finalLocation.equals2D(underTest.getFinalTarget()));

        // this should continue with 2 targets
        newTarget = new Coordinate(5, 5);
        initialSnapshot = snapshot;
        // this should make 2 targets
        underTest.replaceTarget(newTarget);
        snapshot = underTest.getSnapshot();
        /// END OF EDIT

        assertTrue(newTarget.equals2D(underTest.getTarget()));
        assertEquals(2, underTest.getTargets().size());
        assertTrue(finalLocation.equals2D(underTest.getFinalTarget()));

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

    /*
     * public Optional<Step> selectTarget() {
        Position pos = positionSupplier.get();
        if (pos.equals2D(getTarget(), map.getContext().scaleInfo.getResolution())) {
            LOG.debug("Reached intermediate target");
            map.setVisited(getFinalTarget(), target.pop());
            if (target.isEmpty()) {
                LOG.debug("Reached final target");
                return Optional.empty();
            }
        } 
        Optional<Step> selected = map.getBestStep(pos.getCoordinate());
        if (selected.isPresent()) {
            if (!map.areEquivalent(selected.get().getCoordinate(), getTarget())) {
                target.push(selected.get().getCoordinate());
                if (LOG.isDebugEnabled()) {
                    LOG.debug("New target registered: " + selected.get());
                }
            }
        }
        return selected;
    }
     */

    @Test
    public void selectTargetTest() {
        Location finalLocation = Location.from(-1, 1);
        Location stepLocation = Location.from(3, 3);

        StepSupplier stepSupplier = new StepSupplier();
        stepSupplier.setup(new TestingStep(3, 2, 1, 1), null, new TestingStep(-1, 1, 1, 1));

        StepSupplier coordStepSupplier = new StepSupplier();

        final Coordinate visitedTarget[] = { null };

        Map map = new TestingMap() {
            @Override
            public Optional<Step> addCoord(Coordinate target, Double distance, boolean visited, Boolean isIndirect) {
                return Optional.ofNullable(coordStepSupplier.get());
            }

            @Override
            public Optional<Step> getBestStep(Coordinate currentCoords) {
                return Optional.ofNullable(stepSupplier.get());
            }

            @Override
            public void setVisited(Coordinate finalTarget, Coordinate coord) {
                if (underTest.getFinalTarget() == null) {
                    assertEquals(finalTarget, coord);
                } else {
                    assertEquals(underTest.getFinalTarget(), finalTarget);
                }
                visitedTarget[0] = coord;
            }
        };

        Position initial = Position.from(-1, -3);

        coordStepSupplier.setup(new TestingStep(-1, -3, 0, 0));
        TestingPositionSupplier positionSupplier = new TestingPositionSupplier(initial);
        underTest = new PlannerImpl(map, positionSupplier, finalLocation);

        // first target (step)
        Optional<Step> opStep = underTest.selectTarget();
        assertTrue(opStep.isPresent());
        Step step = opStep.get();
        CoordinateUtils.assertEquivalent(step, underTest.getTarget());
        assertNull(visitedTarget[0]);

        // second target (empty)
        opStep = underTest.selectTarget();
        assertFalse(opStep.isPresent());
        CoordinateUtils.assertEquivalent(step, underTest.getTarget());
        assertNull(visitedTarget[0]);

        // change the position to current target location.
        positionSupplier.position = Position.from(underTest.getTarget());
        opStep = underTest.selectTarget();
        assertTrue(opStep.isPresent());
        Step step2 = opStep.get();
        CoordinateUtils.assertEquivalent(step2, underTest.getTarget());
        CoordinateUtils.assertNotEquivalent(step2, step);
        // verify that visited target is set
        CoordinateUtils.assertEquivalent(positionSupplier.position, visitedTarget[0]);

        visitedTarget[0] = null;

        // change the position to the end location
        // should not change snapshot
        // solution should have one more entry
        // target should be null
        positionSupplier.position = Position.from(finalLocation);
        opStep = underTest.selectTarget();
        assertFalse(opStep.isPresent());
        CoordinateUtils.assertEquivalent(positionSupplier.position, visitedTarget[0]);
    }

    private class StepSupplier implements Supplier<Step> {
        Queue<Step> queue = new LinkedList<Step>();

        StepSupplier() {
        }

        void setup(Step... steps) {
            queue.clear();
            for (Step s : steps) {
                queue.add(s);
            }
        }

        @Override
        public Step get() {
            return queue.remove();
        }
    }

    private class TestingStep implements Step {
        UnmodifiableCoordinate coord;
        double cost;
        double distance;

        TestingStep(double x, double y, double cost, double distance) {
            coord = UnmodifiableCoordinate.make(new Coordinate(x, y));
            this.cost = cost;
            this.distance = distance;
        }

        @Override
        public UnmodifiableCoordinate getCoordinate() {
            return coord;
        }

        @Override
        public int compareTo(Step o) {
            return Step.compare.compare(this, o);
        }

        @Override
        public double cost() {
            return cost;
        }

        @Override
        public double distance() {
            return distance;
        }

        @Override
        public Geometry getGeometry() {
            return null;
        }

    }

    private class TestingMap implements Map {

        @Override
        public RobutContext getContext() {
            return ctxt;
        }

        @Override
        public void clear(String mapLayer) {
        }

        @Override
        public boolean isClearPath(Coordinate source, Coordinate dest) {
            return true;
        }

        @Override
        public Optional<Step> addCoord(Coordinate target, Double distance, boolean visited, Boolean isIndirect) {
            return null;
        }

        @Override
        public Collection<Step> getSteps(Coordinate position) {
            return null;
        }

        @Override
        public Collection<MapCoord> getCoords() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Coordinate[] addPath(Coordinate... coords) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Coordinate[] addPath(Resource model, Coordinate... coords) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Coordinate recalculate(Coordinate target) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Optional<Step> getBestStep(Coordinate currentCoords) {
            return null;
        }

        @Override
        public boolean isObstacle(Coordinate coord) {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public Set<Obstacle> addObstacle(Obstacle obstacle) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Set<Obstacle> getObstacles() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void cutPath(Coordinate a, Coordinate b) {
            // TODO Auto-generated method stub

        }

        @Override
        public void recordSolution(Solution solution) {
            // TODO Auto-generated method stub

        }

        @Override
        public boolean areEquivalent(Coordinate a, Coordinate b) {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public Coordinate adopt(Coordinate a) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void updateIsIndirect(Coordinate finalTarget, Set<Obstacle> newObstacles) {
            // TODO Auto-generated method stub

        }

        @Override
        public Obstacle createObstacle(Position startPosition, Location relativeLocation) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void setVisited(Coordinate finalTarget, Coordinate coord) {
            // TODO Auto-generated method stub

        }

        @Override
        public Optional<Location> look(Position position, double heading, int maxRange) {
            // TODO Auto-generated method stub
            return Optional.empty();
        }
    }

}
