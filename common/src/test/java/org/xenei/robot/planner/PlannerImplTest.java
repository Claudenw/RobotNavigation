package org.xenei.robot.planner;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.rdf.model.Resource;
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
import org.xenei.robot.common.mapping.Map;
import org.xenei.robot.common.mapping.MapCoord;
import org.xenei.robot.common.mapping.Obstacle;
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

	@Test
	public void setTargetTest() {
		FrontsCoordinate fc = FrontsCoordinateTest.make(1, 1);

		TestingPositionSupplier supplier = new TestingPositionSupplier(Position.from(Location.ORIGIN));
		underTest = new PlannerImpl(map, supplier);

		assertEquals(ctxt.scaleInfo.round(AngleUtils.RADIANS_45), underTest.setTarget(fc));
		assertEquals(fc.getCoordinate(), underTest.getTarget().getCoordinate());

		Solution solution = underTest.getSolution();
		assertEquals(0, solution.stepCount());
		assertEquals(Location.ORIGIN.getCoordinate(), solution.start().getCoordinate());
	}

	@Test
	public void registerPositionChangeTest() {
		Location finalLocation = Location.from(-1, 1);
		Position initial = Position.from(-1, -3);

		TestingPositionSupplier supplier = new TestingPositionSupplier(initial);
		underTest = new PlannerImpl(map, supplier, finalLocation);
		// verify solution has no steps
		assertEquals(0, underTest.getSolution().stepCount());
		NavigationSnapshot lastSnapshot = new NavigationSnapshot(initial, finalLocation);

		// set next position.
		Position second = Position.from(1, 1);
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

		Location finalLocation = Location.from(-1, 1);
		FrontsCoordinate newTarget = Location.from(4, 4);
		Position initial = Position.from(-1, -3);
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
		Location finalCoord = Location.from(-1, 1);
		Position initial = Position.from(-1, -3);
		TestingPositionSupplier supplier = new TestingPositionSupplier(initial);
		underTest = new PlannerImpl(map, supplier, finalCoord);

		Var dist = Var.alloc("?dist");
		final AskBuilder ask = new AskBuilder().addGraph(Namespace.PlanningModel,
				new WhereBuilder().addWhere(Namespace.s, Namespace.distance, dist)
						.addWhere(Namespace.s, Namespace.x, -1.0).addWhere(Namespace.s, Namespace.y, -3.0));
		ask.setVar(dist, 4.0);
		await().atMost(2, SECONDS).untilAsserted(() -> map.ask(ask));

		FrontsCoordinate newTarget = Location.from(4, 4);
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
		Location finalLocation = Location.from(-1, 1);
		Location stepLocation = Location.from(0, -2);
		Position initial = Position.from(-1, -3);

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
		positionSupplier.position = Position.from(stepLocation);
		optionalSegment = underTest.selectSegment();
		assertTrue(optionalSegment.isPresent());
		CoordinateUtils.assertEquivalent(optionalSegment.get(), underTest.getTarget());

		// change the position to the end location
		// target should be null
		positionSupplier.position = Position.from(finalLocation);
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

	private class TestingMap implements Map {

		@Override
		public MapCoordinate asMapCoordinate(FrontsCoordinate coord) {
			return null;
		}

		@Override
		public RobutContext getContext() {
			return ctxt;
		}

		@Override
		public CompletableFuture<Void> updateIsIndirect(FrontsCoordinate finalTarget, Set<Obstacle> newObstacles) {
			return null;
		}

		@Override
		public Obstacle createObstacle(Position startPosition, FrontsCoordinate relativeLocation) {
			return null;
		}

		@Override
		public Obstacle createObstacle(Position startPosition, FrontsCoordinate relativeStart,
				FrontsCoordinate relativeEnd) {
			return null;
		}

		@Override
		public CompletableFuture<? extends MapCoordinate> setVisited(FrontsCoordinate coord) {
			return null;
		}

		@Override
		public CompletableFuture<Optional<FrontsCoordinate>> look(Position position, double heading, int maxRange) {
			return null;
		}

		@Override
		public void clear(String mapLayer) {
		}

		@Override
		public boolean isClearPath(FrontsCoordinate source, FrontsCoordinate dest) {
			return true;
		}

		@Override
		public CompletableFuture<Optional<Segment>> addCoord(FrontsCoordinate coord, FrontsCoordinate target,
				boolean visited) {
			return null;
		}

		@Override
		public Collection<Segment> getSegments(FrontsCoordinate position) {
			return null;
		}

		@Override
		public CompletableFuture<Collection<MapCoord>> getCoords() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public CompletableFuture<? extends Path> addPath(Resource model, FrontsCoordinate... coords) {
			return CompletableFuture.completedFuture(null);
		}

		@Override
		public MapCoordinate recalculate(FrontsCoordinate target) {
			return null;
		}

		@Override
		public Optional<Segment> getBestSegment(FrontsCoordinate currentCoords) {
			return Optional.empty();
		}

		@Override
		public boolean isObstacle(FrontsCoordinate coord) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public Set<Obstacle> addObstacle(Obstacle obstacle) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public CompletableFuture<Set<Obstacle>> getObstacles() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public CompletableFuture<?> cutPath(FrontsCoordinate a, FrontsCoordinate b) {
			// TODO Auto-generated method stub

			return null;
		}

		@Override
		public CompletableFuture<? extends Path> recordSolution(Solution solution) {
			// TODO Auto-generated method stub
			return null;
		}
	}

}
