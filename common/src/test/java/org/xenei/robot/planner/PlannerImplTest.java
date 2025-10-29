package org.xenei.robot.planner;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.mockito.ArgumentCaptor;
import org.xenei.robot.common.ChassisInfoTest;
import org.xenei.robot.common.mapping.Map;
import org.xenei.robot.common.mapping.MapCoordinate;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.mapping.MapLocation;
import org.xenei.robot.common.mapping.MapPosition;
import org.xenei.robot.common.mapping.MapTargetData;
import org.xenei.robot.common.mapping.MapTest;
import org.xenei.robot.common.mapping.NavigationSnapshot;
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
import org.xenei.robot.mapper.rdf.Namespace;

public class PlannerImplTest {
    final private RobutContext ctxt = new RobutContext(ScaleInfo.DEFAULT, ChassisInfoTest.DEFAULT);
    private Planner underTest;
    private Map map;
    private MapTest.TestingStorage testingStorage;


    final private ArgumentCaptor<MapCoordinate> coordinateCaptor = ArgumentCaptor.forClass(MapCoordinate.class);
    final private ArgumentCaptor<MapCoordinate> targetCaptor = ArgumentCaptor.forClass(MapCoordinate.class);

    private Location makeLoc(double x, double y) {
        return Location.asLocation(new Coordinate(x, y));
    }

    private MapPosition makePosition(double x, double y) {
        return map.asMapPosition(new Coordinate(x, y), 0);
    }

    private Position makePosition(Location loc) {
        return Position.asPosition(loc, 0);
    }

    @BeforeEach
    void setup() {
        testingStorage = new MapTest.TestingStorage();
        map = new Map(ctxt, new MapTest.TestingStorage());
    }

    @Test
    void setTargetTest() {
        MapCoordinate fc = map.asMapCoordinate(new Coordinate(1, 1));

        TestingPositionSupplier supplier = new TestingPositionSupplier(Position.asPosition(Location.ORIGIN, 0));
        underTest = new PlannerImpl(map, supplier);

        assertEquals(ctxt.scaleInfo.round(AngleUtils.RADIANS_45), underTest.setTarget(fc));
        assertEquals(fc.getCoordinate(), underTest.getTarget().getCoordinate());

        Solution solution = underTest.getSolution();
        assertEquals(0, solution.stepCount());
        assertEquals(Location.ORIGIN.getCoordinate(), solution.start().getCoordinate());
    }

    @Test
    void registerPositionChangeTest() {
        MapLocation finalLocation = map.asMapLocation(new Coordinate(-1, 1));
        MapPosition initial = map.asMapPosition(new Coordinate(-1, -3), 0);

        TestingPositionSupplier supplier = new TestingPositionSupplier(initial);
        underTest = new PlannerImpl(map, supplier, finalLocation);
        // verify solution has no steps
        assertEquals(0, underTest.getSolution().stepCount());
        NavigationSnapshot lastSnapshot = new NavigationSnapshot(initial, finalLocation);

        // set next position.
        MapPosition second = map.asMapPosition(new Coordinate(1, 1), 0);
        NavigationSnapshot snapshot = new NavigationSnapshot(second, finalLocation);
        // since there is only one target this will add a position to the target stack
        underTest.registerPositionChange(snapshot);

        // verify that the snapshot should cause a change in position.
        assertTrue(lastSnapshot.didChange(snapshot));

        // verify solution has 2 items (1 step)
        await().atMost(2, SECONDS).untilAsserted(() -> assertEquals(1, underTest.getSolution().stepCount()));
        List<MapCoordinate> sol = underTest.getSolution().stream().toList();
        assertEquals(2, sol.size());
        assertEquals(initial.getCoordinate(), sol.get(0).getCoordinate());
        assertEquals(finalLocation.getCoordinate(), sol.get(1).getCoordinate());
    }

    @Test
    public void replaceTargetTest() {
        MapLocation finalLocation = map.asMapLocation(new Coordinate(-1, 1));
        MapCoordinate newTarget = map.asMapCoordinate(new Coordinate(4, 4));
        MapPosition initial = map.asMapPosition(new Coordinate(-1, -3), 0);
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
        Location finalCoord = Location.asLocation(new Coordinate(-1, 1));
        Position initial = Position.asPosition(new Coordinate(-1, -3), 0);
        TestingPositionSupplier supplier = new TestingPositionSupplier(initial);
        underTest = new PlannerImpl(map, supplier, finalCoord);

        MapLocation location = map.asMapLocation(new Coordinate(-1, -3));
        MapTargetData targetData = location.getTargetData(map.asMapLocation(finalCoord));
        assertThat(targetData.distance()).isEqualTo(4.0); // ctxt.scaleInfo.scale(4.0)?

        MapCoordinate newTarget = map.asMapCoordinate(new Coordinate(4, 4));
        underTest.replaceTarget(newTarget);
        targetData = location.getTargetData(map.asMapLocation(newTarget));
        assertThat(targetData.distance()).isEqualTo(ctxt.scaleInfo.scale(Math.sqrt(74)));

        // verify solution has 1 item
        Solution solution = underTest.getSolution();
        List<MapCoordinate> sol = solution.stream().toList();
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
        map.asMapLocation(stepLocation).getTargetData(map.asMapLocation(finalLocation));
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

//    private static class TestingStep implements Segment {
//        UnmodifiableCoordinate coord;
//        double cost;
//        double distance;
//
//        TestingStep(double x, double y, double cost, double distance) {
//            coord = UnmodifiableCoordinate.make(new Coordinate(x, y));
//            this.cost = cost;
//            this.distance = distance;
//        }
//
//        @Override
//        public UnmodifiableCoordinate getCoordinate() {
//            return coord;
//        }
//
//        @Override
//        public int compareTo(Segment o) {
//            return Segment.COMPARATOR.compare(this, o);
//        }
//
//        @Override
//        public double cost() {
//            return cost;
//        }
//
//        @Override
//        public double distance() {
//            return distance;
//        }
//
//        @Override
//        public Geometry getWkt() {
//            return null;
//        }
//
//    }
}
