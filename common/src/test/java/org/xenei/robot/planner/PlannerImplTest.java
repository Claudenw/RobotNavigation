package org.xenei.robot.planner;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.function.Supplier;

import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.sparql.core.Var;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.mockito.ArgumentCaptor;
import org.xenei.robot.common.ChassisInfoTest;
import org.xenei.robot.common.FrontsCoordinate;
import org.xenei.robot.common.FrontsCoordinateTest;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.NavigationSnapshot;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.ScaleInfo;
import org.xenei.robot.common.UnmodifiableCoordinate;
import org.xenei.robot.common.planning.Planner;
import org.xenei.robot.common.planning.Solution;
import org.xenei.robot.common.planning.Segment;
import org.xenei.robot.common.testUtils.CoordinateUtils;
import org.xenei.robot.common.testUtils.TestingPositionSupplier;
import org.xenei.robot.common.utils.AngleUtils;
import org.xenei.robot.common.utils.RobutContext;
import org.xenei.robot.mapper.map.MapImpl;
import org.xenei.robot.mapper.rdf.Namespace;

public class PlannerImplTest {
    final private RobutContext ctxt = new RobutContext(ScaleInfo.DEFAULT, ChassisInfoTest.DEFAULT);
    private Planner underTest;
    private final MapImpl map = new MapImpl(ctxt);

    final private ArgumentCaptor<FrontsCoordinate> coordinateCaptor = ArgumentCaptor.forClass(FrontsCoordinate.class);
    final private ArgumentCaptor<FrontsCoordinate> targetCaptor = ArgumentCaptor.forClass(FrontsCoordinate.class);

    private Location makeLoc(double x, double y) {
        return new Location(new Coordinate(x, y));
    }

    private Position makePosition(double x, double y) {
        return new Position(new Coordinate(x, y), 0);
    }

    private Position makePosition(Location loc) {
        return new Position(loc.getCoordinate(), 0);
    }

    @Test
    void setTargetTest() {
        FrontsCoordinate fc = FrontsCoordinateTest.make(1, 1);

        TestingPositionSupplier supplier = new TestingPositionSupplier(Position.ORIGIN);
        underTest = new PlannerImpl(map, supplier);

        assertEquals(ctxt.scaleInfo.round(AngleUtils.RADIANS_45), underTest.setTarget(fc));
        assertEquals(fc.getCoordinate(), underTest.getTarget().getCoordinate());

        Solution solution = underTest.getSolution();
        assertEquals(0, solution.stepCount());
        assertEquals(Location.ORIGIN.getCoordinate(), solution.start().getCoordinate());
    }

    @Test
    void registerPositionChangeTest() {
        Location finalLocation = makeLoc(-1, 1);
        Position initial = makePosition(-1, -3);

        TestingPositionSupplier supplier = new TestingPositionSupplier(initial);
        underTest = new PlannerImpl(map, supplier, finalLocation);
        // verify solution has no steps
        assertEquals(0, underTest.getSolution().stepCount());
        NavigationSnapshot lastSnapshot = new NavigationSnapshot(initial, finalLocation);

        // set next position.
        Position second = makePosition(1, 1);
        NavigationSnapshot snapshot = new NavigationSnapshot(second, finalLocation);
        // since there is only one target this will add a position to the target stack
        underTest.registerPositionChange(snapshot);

        // verify that the snapshot should cause a change in position.
        assertTrue(lastSnapshot.didChange(snapshot));

        // verify solution has 2 items (1 step)
        await().atMost(2, SECONDS).untilAsserted(() -> assertEquals(1, underTest.getSolution().stepCount()));
        List<FrontsCoordinate> sol = underTest.getSolution().stream().toList();
        assertEquals(2, sol.size());
        assertEquals(initial.getCoordinate(), sol.get(0).getCoordinate());
        assertEquals(finalLocation.getCoordinate(), sol.get(1).getCoordinate());
    }

    @Test
    public void replaceTargetTest() {

        Location finalLocation = makeLoc(-1, 1);
        FrontsCoordinate newTarget = makeLoc(4, 4);
        Position initial = makePosition(-1, -3);
        TestingPositionSupplier supplier = new TestingPositionSupplier(initial);
        NavigationSnapshot initialSnapshot = new NavigationSnapshot(initial, finalLocation);

        underTest = new PlannerImpl(map, supplier, finalLocation);
        NavigationSnapshot snapshot = underTest.getSnapshot();
        assertFalse(initialSnapshot.didChange(snapshot));

        underTest.replaceTarget(newTarget);
        snapshot = underTest.getSnapshot();
        assertTrue(initialSnapshot.didTargetChange(snapshot));
        assertTrue(map.getContext().scaleInfo.areEquivalent(newTarget, underTest.getTarget()));
        assertEquals(2, underTest.getTargets().size());
        assertTrue(map.getContext().scaleInfo.areEquivalent(finalLocation, underTest.getFinalTarget()));

    }

    @Test
    public void recalculateCostsTest() throws InterruptedException {
        Location finalCoord = makeLoc(-1, 1);
        Position initial = makePosition(-1, -3);
        TestingPositionSupplier supplier = new TestingPositionSupplier(initial);
        underTest = new PlannerImpl(map, supplier, finalCoord);

        Var dist = Var.alloc("?dist");
        final AskBuilder ask = new AskBuilder().addGraph(Namespace.PlanningModel,
                new WhereBuilder().addWhere(Namespace.s, Namespace.distance, dist)
                        .addWhere(Namespace.s, Namespace.x, -1.0).addWhere(Namespace.s, Namespace.y, -3.0));
        ask.setVar(dist, 4.0);
        await().atMost(2, SECONDS).untilAsserted(() -> map.ask(ask));

        FrontsCoordinate newTarget = makeLoc(4, 4);
        underTest.replaceTarget(newTarget);
        underTest.recalculateCosts();

        ask.setVar(dist, Math.sqrt(74));
        await().atMost(2, SECONDS).untilAsserted(() -> map.ask(ask));

        // verify solution has 1 item
        Solution solution = underTest.getSolution();
        List<FrontsCoordinate> sol = solution.stream().toList();
        assertEquals(1, sol.size());
        assertEquals(initial.getCoordinate(), sol.get(0).getCoordinate());
        assertEquals(0.0, solution.cost());
    }

    /*
     * public Optional<Step> selectTarget() { Position pos = positionSupplier.get();
     * if (pos.equals2D(getTarget(), map.getContext().scaleInfo.getResolution())) {
     * LOG.debug("Reached intermediate target"); map.setVisited(getFinalTarget(),
     * target.pop()); if (target.isEmpty()) { LOG.debug("Reached final target");
     * return Optional.empty(); } } Optional<Step> selected =
     * map.getBestStep(pos.getCoordinate()); if (selected.isPresent()) { if
     * (!map.areEquivalent(selected.get().getCoordinate(), getTarget())) {
     * target.push(selected.get().getCoordinate()); if (LOG.isDebugEnabled()) {
     * LOG.debug("New target registered: " + selected.get()); } } } return selected;
     * }
     */

    @Test
    public void selectSegmentTest() {
        Location finalLocation = makeLoc(-1, 1);
        Location stepLocation = makeLoc(0, -2);
        Position initial = makePosition(-1, -3);

        TestingPositionSupplier positionSupplier = new TestingPositionSupplier(initial);
        underTest = new PlannerImpl(map, positionSupplier, finalLocation);

        // first target (segment = target)
        Optional<Segment> optionalSegment = underTest.selectSegment();
        assertTrue(optionalSegment.isPresent());
        CoordinateUtils.assertEquivalent(optionalSegment.get(), underTest.getTarget());

        // second target (segment = stepLocation)
        map.addCoord(stepLocation, finalLocation, false);
        optionalSegment = underTest.selectSegment();
        assertTrue(optionalSegment.isPresent());
        CoordinateUtils.assertEquivalent(optionalSegment.get(), stepLocation);

        // change the position to step location.
        positionSupplier.position = makePosition(stepLocation);
        optionalSegment = underTest.selectSegment();
        assertTrue(optionalSegment.isPresent());
        CoordinateUtils.assertEquivalent(optionalSegment.get(), underTest.getTarget());

        // change the position to the end location
        // target should be null
        positionSupplier.position = makePosition(finalLocation);
        optionalSegment = underTest.selectSegment();
        assertFalse(optionalSegment.isPresent());
    }

    private static class StepSupplier implements Supplier<Segment> {
        Queue<Segment> queue = new LinkedList<>();

        StepSupplier() {
        }

        void setup(Segment... steps) {
            queue.clear();
            Collections.addAll(queue, steps);
        }

        @Override
        public Segment get() {
            return queue.remove();
        }
    }

    private static class TestingStep implements Segment {
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
        public int compareTo(Segment o) {
            return Segment.compare.compare(this, o);
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
}
