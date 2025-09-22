package org.xenei.robot.mapper;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.arq.querybuilder.ExprFactory;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.geosparql.implementation.vocabulary.Geo;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.vocabulary.RDF;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.common.ChassisInfoTest;
import org.xenei.robot.common.FrontsCoordinate;
import org.xenei.robot.common.FrontsCoordinateTest;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.ScaleInfo;
import org.xenei.robot.common.mapping.Obstacle;
import org.xenei.robot.common.planning.Solution;
import org.xenei.robot.common.planning.Segment;
import org.xenei.robot.common.testUtils.CoordinateUtils;
import org.xenei.robot.common.testUtils.DebugViz;
import org.xenei.robot.common.testUtils.MapLibrary;
import org.xenei.robot.common.utils.AngleUtils;
import org.xenei.robot.common.utils.CoordUtils;
import org.xenei.robot.common.utils.RobutContext;
import org.xenei.robot.mapper.map.MapImpl;
import org.xenei.robot.mapper.rdf.Namespace;
import org.xenei.robot.mapper.visualization.MapViz;

public class MapImplTest {

	private static final Logger LOG = LoggerFactory.getLogger(MapImplTest.class);
	private static final ScaleInfo scaleInfo = ScaleInfo.DEFAULT;
	private static final RobutContext ctxt = new RobutContext(scaleInfo, ChassisInfoTest.DEFAULT);

	private MapImpl underTest;

	public static final Location[] coordinates = {Location.from(-4, -4), Location.from(-4, -3), Location.from(-4, -1),
			Location.from(-2, -4), Location.from(-2, -2), Location.from(-1, -4), Location.from(-1, -2),
			Location.from(0, -4), Location.from(0, -2), Location.from(2, -4), Location.from(2, -3),
			Location.from(2, -1)};

	public static final Location[] obstacles = {Location.from(-5, -4), Location.from(-5, -3), Location.from(-5, -1),
			Location.from(-3, -5), Location.from(-3, -1), Location.from(-2, -5), Location.from(-2, -1),
			Location.from(-1, -5), Location.from(-1, -1), Location.from(0, -5), Location.from(0, -1),
			Location.from(1, -5), Location.from(1, -1), Location.from(3, -4), Location.from(3, -3),
			Location.from(3, -1)};

	static List<FrontsCoordinate[]> paths = new ArrayList<>();

	static final Position position = Position.from(-1, -3);

	static Location target = Location.from(-1, 1);

	DebugViz cMap;

	Solution solution;

	public static List<FrontsCoordinate> obstacleList() {
		return Arrays.asList(obstacles);
	}

	@BeforeAll
	public static void setupPaths() {
		for (FrontsCoordinate e : coordinates) {
			paths.add(new FrontsCoordinate[]{position, e});
		}
	}

	private void setup() {
		underTest = new MapImpl(ctxt);
		MapLibrary.map2(underTest);
		solution = new Solution();
		solution.add(position);
		target = Location.from(-1, 1);
		cMap = new DebugViz(1, underTest, () -> solution, () -> position, () -> target);

		List<CompletableFuture<?>> futures = new ArrayList<>();
		futures.add(underTest.addCoord(position, target, false));
		Arrays.stream(coordinates).forEach(c -> futures.add(underTest.addCoord(c, target, false)));
		for (CompletableFuture<?> f : futures) {
			f.join();
		}
		ctxt.awaitQuiescence(1, SECONDS);
	}

	@Test
	void addCoordTest_buildup() {
		underTest = new MapImpl(ctxt);
		target = Location.from(-1, 1);

		Optional<Segment> result = underTest.addCoord(position, null, false).join();
		assertFalse(result.isPresent());

		AskBuilder askResult = new AskBuilder().addWhere(Namespace.s, RDF.type, Namespace.Coord)
				.addWhere(Namespace.s, Namespace.x, -1d).addWhere(Namespace.s, Namespace.y, -3d);
		assertFalse(underTest.ask(askResult));

		AskBuilder askFalse = askResult.clone();
		askFalse.addWhere(Namespace.s, Namespace.isIndirect, Namespace.o);
		assertFalse(underTest.ask(askFalse));

		askFalse = askResult.clone();
		askFalse.addWhere(Namespace.s, Namespace.visited, Namespace.o);
		assertFalse(underTest.ask(askFalse));

		askFalse = askResult.clone();
		askFalse.addWhere(Namespace.s, Namespace.distance, Namespace.o);
		assertFalse(underTest.ask(askFalse));

		result = underTest.addCoord(position, target, false).join();

		assertTrue(result.isPresent());
		Segment segment = result.get();
		assertEquals(4.0d, segment.distance());
		FrontsCoordinateTest.assertEquals(target, segment, scaleInfo);
		assertEquals(4.0d, segment.cost());

		askResult.addWhere(Namespace.s, RDF.type, Namespace.Coord).addWhere(Namespace.s, Namespace.x, -1d)
				.addWhere(Namespace.s, Namespace.y, -3d).addWhere(Namespace.s, Namespace.isIndirect, false)
				.addWhere(Namespace.s, Namespace.distance, 4.0);

		await().atMost(Duration.ofSeconds(5)).pollInterval(Duration.ofMillis(500))
				.untilAsserted(() -> underTest.ask(askResult));

		askFalse = askResult.clone();
		askFalse.addWhere(Namespace.s, Namespace.visited, Namespace.o);
		assertFalse(underTest.ask(askFalse));

		result = underTest.addCoord(position, null, true).join();
		assertFalse(result.isPresent());

		// System.out.println(MapReports.dumpModel(underTest, Namespace.PlanningModel));
		askResult.addWhere(Namespace.s, Namespace.visited, Namespace.o);
		await().atMost(Duration.ofSeconds(5)).pollInterval(Duration.ofMillis(500))
				.untilAsserted(() -> underTest.ask(askResult));
	}

	@Test
	void addCoordTest_complete() {
		underTest = new MapImpl(ctxt);
		target = Location.from(-1, 1);

		Optional<Segment> result = underTest.addCoord(position, target, true).join();
		assertTrue(result.isPresent());
		Segment segment = result.get();
		assertEquals(4.0d, segment.distance());
		FrontsCoordinateTest.assertEquals(target, segment, scaleInfo);
		assertEquals(4.0d, segment.cost());

		AskBuilder askResult = new AskBuilder().addWhere(Namespace.s, RDF.type, Namespace.Coord)
				.addWhere(Namespace.s, Namespace.x, -1d).addWhere(Namespace.s, Namespace.y, -3d)
				.addWhere(Namespace.s, Namespace.isIndirect, false).addWhere(Namespace.s, Namespace.distance, 4.0)
				.addWhere(Namespace.s, Namespace.visited, true);

		await().atMost(Duration.ofSeconds(5)).pollInterval(Duration.ofMillis(500))
				.untilAsserted(() -> underTest.ask(askResult));
	}

	@Test
	void addCoordTest_completeWithObstacle() {
		underTest = new MapImpl(ctxt);
		target = Location.from(-1, 1);

		underTest.addObstacle(new ObstacleImpl(-1, -2));

		Optional<Segment> result = underTest.addCoord(position, target, true).join();
		ctxt.awaitQuiescence(5, SECONDS);

		assertTrue(result.isPresent());
		Segment segment = result.get();
		assertEquals(4.0d, segment.distance());
		assertEquals(target.getCoordinate(), segment.getCoordinate());
		assertEquals(8.0d, segment.cost());

		AskBuilder askResult = new AskBuilder().addWhere(Namespace.s, RDF.type, Namespace.Coord)
				.addWhere(Namespace.s, Namespace.x, -1d).addWhere(Namespace.s, Namespace.y, -3d)
				.addWhere(Namespace.s, Namespace.isIndirect, true).addWhere(Namespace.s, Namespace.distance, 4.0)
				.addWhere(Namespace.s, Namespace.visited, true);

		await().atMost(Duration.ofSeconds(5)).pollInterval(Duration.ofMillis(500))
				.untilAsserted(() -> underTest.ask(askResult));
	}

	@Test
	public void getBestTargetTest() {
		setup();
		TreeSet<FrontsCoordinate> solutions = new TreeSet<>(FrontsCoordinate.XYCompr);
		solutions.add(Location.from(2, -1));
		solutions.add(Location.from(-4, -1));
		MapViz vMap = new MapViz(100, underTest, () -> solution, () -> Position.from(position), () -> target);
		vMap.redraw();
		Optional<Segment> pr = underTest.getBestSegment(position);
		assertTrue(pr.isPresent());
		Segment segment = pr.get();
		assertTrue(solutions.contains(segment));

		// remove the 2 possible solutions.
		solutions.forEach(c -> underTest.setVisited(target, c));

		pr = underTest.getBestSegment(position);
		assertTrue(pr.isPresent());
		segment = pr.get();
		assertEquals(0, Location.from(-1, -2).compareTo(segment.getCoordinate()));

		// remove all the solutions
		underTest.getCoords().join().forEach(c -> underTest.setVisited(target, c.location));
		pr = underTest.getBestSegment(position);
		assertFalse(pr.isPresent());
	}

	/**
	 * Checks that at least one of the geometries (obsts) contains the coordinate.
	 * 
	 * @param obsts
	 *            the list of geometries.
	 * @param c
	 *            he coorindate to contain.
	 */
	static void assertCoordinateInObstacles(Collection<? extends Geometry> obsts, Coordinate c) {
		boolean found = false;
		Geometry cGeom = ctxt.geometryUtils.asPoint(c);
		for (Geometry geom : obsts) {
			if (geom.intersects(cGeom)) {
				found = true;
				break;
			}
		}
		assertTrue(found, () -> "Missing coordinate " + c);
	}

	@Test
	public void getStepTest() throws InterruptedException {
		setup();
		System.out.println(MapReports.dumpModel(underTest, Namespace.PlanningModel));

		Optional<Segment> pr = underTest.getStep(0.0, Location.from(position)).join();
		assertTrue(pr.isPresent());
		assertEquals(0, FrontsCoordinate.XYCompr.compare(position, pr.get()));
		assertEquals(position.distance(target), pr.get().distance());
		// p can not see t so cost should be 2x distance
		assertEquals(pr.get().distance() * 2, pr.get().cost());

		pr = underTest.getStep(0.0, Location.from(target)).join();
		assertTrue(pr.isEmpty());

		for (FrontsCoordinate e : coordinates) {
			pr = underTest.getStep(0.0, Location.from(e)).join();
			assertTrue(pr.isPresent());
			assertEquals(0, FrontsCoordinate.XYCompr.compare(e, pr.get()));
			assertEquals(e.distance(target), pr.get().distance());
		}
		for (FrontsCoordinate o : obstacles) {
			pr = underTest.getStep(0.0, Location.from(o)).join();
			assertTrue(pr.isEmpty());
		}
	}

	@Test
	public void getStepsTest() {
		setup();

		Solution solution = new Solution();
		solution.add(position);
		// Supplier<Position> positionSupplier = () -> Position.from( p );
		cMap.redraw();
		// looking from the target we should only see -4,-1 and 2,-1
		Collection<Segment> records = underTest.getSegments(position);
		cMap.redraw();
		assertEquals(12, records.size());

		FrontsCoordinate nxt = records.iterator().next();
		underTest.setVisited(target, nxt);
		records = underTest.getSegments(position);
		cMap.redraw();
		assertEquals(11, records.size());
	}

	@Test
	public void getCoordsTest() {
		setup();
		List<FrontsCoordinate> expected = new ArrayList<>(Arrays.asList(coordinates));
		expected.add(position);
		expected.sort(FrontsCoordinate.XYCompr);

		List<FrontsCoordinate> records = underTest.getCoords().join().stream().map(m -> m.location)
				.collect(Collectors.toList());
		records.sort(FrontsCoordinate.XYCompr);
		assertEquals(expected.size(), records.size());

		for (int i = 0; i < records.size(); i++) {
			assertEquals(0, FrontsCoordinate.XYCompr.compare(records.get(i), expected.get(i)));
		}
	}

	@Test
	public void isEmptyTest() {
		assertTrue(new MapImpl(ctxt).isEmpty());
	}

	@Test
	public void addPathTest() {
		setup();
		Location a = Location.from(coordinates[0]);
		Location b = Location.from(coordinates[1]);

		assertFalse(underTest.hasPath(a, b), "Should not have path");

		Location c = Location.from(5, 5);
		underTest.addCoord(c, a, false);

		underTest.addPath(a, c);

		ctxt.awaitQuiescence(5, SECONDS);
		assertTrue(underTest.hasPath(a, c), "Should have path");
	}

	@Test
	public void hasPathTest() {
		setup();
		for (FrontsCoordinate[] l : paths) {
			underTest.addPath(l[0], l[1]);
		}
		Location a = Location.from(position);
		Location b = Location.from(coordinates[0]);
		Location c = Location.from(target);
		underTest.addCoord(target, null, false);

		assertTrue(underTest.hasPath(a, b));
		assertFalse(underTest.hasPath(b, c));
		assertFalse(underTest.hasPath(a, c));

		underTest.addPath(b, c);

		assertTrue(underTest.hasPath(a, b));
		assertTrue(underTest.hasPath(b, c));
		// assertTrue(underTest.hasPath(a, c));// does this one actually work
	}

	@Test
	public void recalculateTest() {
		setup();
		SelectBuilder select = new SelectBuilder();
		ExprFactory exprF = select.getExprFactory();
		select.addVar("Count(*)", "?count").from(Namespace.UnionModel.getURI()) //
				.addWhere(Namespace.s, RDF.type, Namespace.Coord) //
				.addOptional(Namespace.s, Namespace.isIndirect, "?indFlg") //
				.addBind(exprF.cond(exprF.bound("?indFlg"), exprF.asExpr("?indFlg"), exprF.asExpr(false)),
						"?isIndirect")
				.addFilter(exprF.not("?isIndirect"));

		SelectBuilder report = new SelectBuilder();
		if (LOG.isDebugEnabled()) {
			report.addVar("?wkt").from(Namespace.UnionModel.getURI()) //
					.addWhere(Namespace.s, RDF.type, Namespace.Coord) //
					.addWhere(Namespace.s, Geo.AS_WKT_PROP, "?wkt")
					.addOptional(Namespace.s, Namespace.isIndirect, "?indFlg") //
					.addBind(exprF.cond(exprF.bound("?indFlg"), exprF.asExpr("?indFlg"), exprF.asExpr(false)),
							"?isIndirect")
					.addFilter(exprF.not("?isIndirect"));

			LOG.debug("\n{}", MapReports.dumpQuery(underTest, report));
		}

		int count = underTest.exec(select).thenApply(resultSet -> resultSet.next().getLiteral("count").getInt()).join();
		ctxt.awaitQuiescence(5, SECONDS);
		cMap.redraw();

		// assertEquals(3, count, "Should have 3 direct points");

		cMap.redraw();

		Location c = Location.from(coordinates[0]);

		Segment before = underTest.getStep(0.0, c).join().orElseThrow();
		target = Location.from(-4, 1);
		underTest.recalculate(target);

		solution.add(target);
		if (LOG.isDebugEnabled()) {
			LOG.debug("\n{}", MapReports.dumpQuery(underTest, report));
		}
		cMap.redraw();

		Segment after = underTest.getStep(0.0, c).join().orElseThrow();
		assertNotEquals(before.cost(), after.cost());

		System.out.println(MapReports.dumpModel(underTest));
		cMap.redraw();

		System.out.println(position);

		// await().atMost(5, SECONDS).untilAsserted(() -> assertEquals(5,
		// underTest.exec(select).thenApply(resultSet -> {
		// return resultSet.next().getLiteral("count").getInt();
		// }).join(), () -> "Should have 5 direct points"));
	}

	@Test
	public void updateTest() {
		setup();
		Location c = Location.from(5, 4);
		// Resource r = Namespace.urlOf(c);

		// check not there, update then verify that it is.
		AskBuilder ask = new AskBuilder().from(Namespace.UnionModel.getURI()) //
				.addWhere(Namespace.s, Namespace.distance, 5) //
				.addWhere(Namespace.s, Namespace.x, c.getX()) //
				.addWhere(Namespace.s, Namespace.y, c.getY());
		assertFalse(underTest.ask(ask));

		// no coordinate so update should not do anything.
		underTest.updateCoordinate(Namespace.PlanningModel, c, Namespace.distance, 5).join();
		assertFalse(underTest.ask(ask));

		// add the coordinate with a distance of 1.
		Location target = Position.from(c).relativeLocation(Location.from(0, 1));
		underTest.addCoord(c, target, false);
		assertFalse(underTest.ask(ask));
		// now update it to 5 and verify that it is there.
		underTest.updateCoordinate(Namespace.PlanningModel, c, Namespace.distance, 5).join();
		assertTrue(underTest.ask(ask));

		ExprFactory exprF = new ExprFactory();

		ask = new AskBuilder().from(Namespace.UnionModel.getURI()) //
				.addWhere(Namespace.s, Namespace.distance, Namespace.o) //
				.addFilter(exprF.eq(Namespace.o, 5.0));
		assertTrue(underTest.ask(ask));

		c = Location.from(coordinates[0]);
		assertTrue(underTest.ask(ask));

		Segment before = underTest.getStep(0.0, c).join().orElseThrow();
		underTest.updateCoordinate(Namespace.PlanningModel, c, Namespace.distance, before.distance() + 5).join();

		SelectBuilder sb = new SelectBuilder().from(Namespace.UnionModel.getURI()) //
				.addWhere(Namespace.s, Namespace.distance, before.distance() + 5) //
				.addWhere(Namespace.s, Namespace.x, c.getX()) //
				.addWhere(Namespace.s, Namespace.y, c.getY());
		int count = underTest.exec(sb).thenApply(resultSet -> {
			int cnt = 0;
			while (resultSet.hasNext()) {
				cnt++;
				resultSet.next();
			}
			return cnt;
		}).join();
		assertEquals(1, count);

		Segment after = underTest.getStep(0.0, c).join().orElseThrow();
		assertEquals(before.distance() + 5, after.distance());
	}

	@Test
	public void clearViewTest() {
		underTest = new MapImpl(ctxt);
		Position pos = Position.from(position, 0);

		underTest.addObstacle(underTest.createObstacle(pos, pos.relativeLocation(Location.from(-3, -3))));
		FrontsCoordinate a = Location.from(-3, -4);
		FrontsCoordinate b = Location.from(-3, -2);

		assertFalse(underTest.isClearPath(Location.from(a), Location.from(b)), "Did not expect clear path");
		b = Location.from(-4, -4);
		assertTrue(underTest.isClearPath(Location.from(a), Location.from(b)), "Expected clear path");
	}

	@Test
	public void verifyMapRounding() {
		// verify inserting a node near map coord shows up at map coord
		underTest = new MapImpl(ctxt);
		Location target = Position.from(position).nextPosition(Location.from(0, 11));
		Segment step = underTest.addCoord(position, target, false).join().orElseThrow();
		assertEquals(11, step.cost());
		String result = MapReports.dumpModel(underTest, Namespace.PlanningModel);
		AskBuilder ask = new AskBuilder().addGraph(Namespace.PlanningModel, new WhereBuilder() //
				.addWhere(Namespace.s, Namespace.x, position.getX()) //
				.addWhere(Namespace.s, Namespace.y, position.getY()) //
				.addWhere(Namespace.s, Namespace.distance, 11.0) //
				.addWhere(Namespace.s, RDF.type, Namespace.Coord) //
				.addWhere(Namespace.s, Geo.AS_WKT_PROP,
						ctxt.graphGeomFactory.asWKT(ctxt.geometryUtils.asPoint(position))));
		assertTrue(underTest.ask(ask));

		// create a point not "p" and within p +/- the resolution of the map.
		double incr = ctxt.scaleInfo.getResolution() / 2;
		FrontsCoordinate p2 = Location.from(position.getX() + incr, position.getY() + incr);
		assertNotEquals(position, p2);
		step = underTest.addCoord(p2, target, false).join().orElseThrow();
		assertTrue(underTest.ask(ask));
		assertEquals(11, step.cost());
		SelectBuilder sb = new SelectBuilder().from(Namespace.PlanningModel).addVar("count(*)", "?x")
				.addWhere(Namespace.s, RDF.type, Namespace.Coord);
		int count = underTest.exec(sb).thenApply(resultSet -> resultSet.next().getLiteral("x").getInt()).join();
		assertEquals(1, count);
	}

	@Test
	public void testAddPath() {
		underTest = new MapImpl(ctxt);
		assertTrue(underTest.addCoord(position, target, false).join().isPresent());
		assertTrue(underTest.addCoord(coordinates[0], target, false).join().isPresent());
		underTest.addPath(position, coordinates[0]);
		ctxt.awaitQuiescence(30, TimeUnit.SECONDS);

		ExprFactory exprF = new ExprFactory(MapImpl.getPrefixMapping());
		Var wkt = Var.alloc("wkt");
		Literal pathWkt = ctxt.graphGeomFactory.asWKTString(position.getCoordinate(), coordinates[0].getCoordinate());
		AskBuilder ask = new AskBuilder().addPrefixes(MapImpl.getPrefixMapping()).from(Namespace.PlanningModel.getURI())
				.addWhere(Namespace.s, RDF.type, Namespace.Coord).addWhere(Namespace.s, Geo.AS_WKT_PROP, wkt)
				.addFilter(ctxt.graphGeomFactory.isNearby(exprF, wkt, pathWkt, ctxt.scaleInfo.getResolution()));

		assertTrue(underTest.ask(ask));
	}

	@Test
	public void createObstacleTest() {
		underTest = new MapImpl(ctxt);
		Position pos = Position.from(position, 0);
		Location relative = Location.from(1, 0);
		Obstacle obst = underTest.createObstacle(pos, relative);
		Coordinate[] lst = obst.geom().getCoordinates();
		assertEquals(1, lst.length);
		CoordinateUtils.assertEquivalent(Location.from(0, -3), lst[0], ctxt.scaleInfo.getResolution());
		assertEquals(ctxt.graphGeomFactory.asWKT(obst.geom()), obst.wkt());
	}

	@Test
	public void addObstacleTest() {
		underTest = new MapImpl(ctxt);
		Position pos = Position.from(position, 0);
		Location relative = Location.from(ctxt.scaleInfo.getResolution(), 0);
		Obstacle obst = underTest.createObstacle(pos, relative);
		Set<Obstacle> result = underTest.addObstacle(obst);
		assertEquals(1, result.size());
		assertEquals(obst, result.iterator().next());

		relative = Location.from(0, ctxt.scaleInfo.getResolution());
		Obstacle obst2 = underTest.createObstacle(pos, relative);
		result = underTest.addObstacle(obst2);
		double halfResolution = ctxt.scaleInfo.getResolution() / 2;
		relative = Location.from(halfResolution, halfResolution);
		Obstacle obst3 = underTest.createObstacle(pos, relative);
		result = underTest.addObstacle(obst3);

		assertEquals(1, underTest.getObstacles().join().size());
	}

	@Test
	public void isObstacleTest() {
		underTest = new MapImpl(ctxt);
		Position pos = Position.from(position, 0);
		Location relative = Location.from(1, 0);
		Position pos1 = position.nextPosition(relative);
		Obstacle obst = underTest.createObstacle(pos, relative);
		underTest.addObstacle(obst);
		await().atMost(Duration.ofSeconds(1))
				.untilAsserted(() -> assertTrue(underTest.isObstacle(pos1), "Did not find 1 " + pos1));

		relative = Location.from(1, 1);
		Position pos2 = position.nextPosition(relative);
		Obstacle obst2 = underTest.createObstacle(pos, relative);
		Set<Obstacle> obstacles = underTest.addObstacle(obst2);

		await().atMost(Duration.ofSeconds(1))
				.untilAsserted(() -> assertTrue(underTest.isObstacle(pos2), "Did not find 2 " + pos2));

		relative = Location.from(CoordUtils.fromAngle(AngleUtils.RADIANS_45 / 2, 1));
		Position pos3 = position.nextPosition(relative);
		Obstacle obst3 = underTest.createObstacle(pos, relative);
		underTest.addObstacle(obst3);
		await().atMost(Duration.ofSeconds(1))
				.untilAsserted(() -> assertTrue(underTest.isObstacle(pos3), "Did not find 3 " + pos3));

		for (Coordinate c : obst.geom().getCoordinates())
			assertTrue(underTest.isObstacle(Location.from(c)), "Did not find 1 coordinate " + c);
		for (Coordinate c : obst2.geom().getCoordinates())
			assertTrue(underTest.isObstacle(Location.from(c)), "Did not find 2 coordinate " + c);
		System.out.println(MapReports.dumpModel(underTest.getModel()));
		for (Coordinate c : obst3.geom().getCoordinates())
			assertTrue(underTest.isObstacle(Location.from(c)), "Did not find 3 coordinate " + c);
	}

	@Test
	public void isClearPathTest() {
		setup();
		cMap.redraw();

		assertFalse(underTest.isClearPath(Location.from(position), Location.from(target)));
		assertFalse(underTest.isClearPath(Location.from(-2, -2), Location.from(target)));
		assertTrue(underTest.isClearPath(Location.from(-2, -2), Location.from(position)));
		assertFalse(underTest.isClearPath(Location.from(2, -1), Location.from(-4, -1)));
	}

	@Test
	public void lookTest() {
		double delta = 0.0001;
		setup();
		cMap.redraw();
		Position pos = Position.from(position);

		Optional<FrontsCoordinate> result = underTest.look(pos, 0, 250).join();
		assertTrue(result.isPresent());
		FrontsCoordinate loc = result.get();
		assertEquals(4, loc.getX(), delta);

		assertEquals(0, loc.getY(), delta);
		cMap.redraw();
		result = underTest.look(pos, AngleUtils.RADIANS_45, 250).join();
		assertTrue(result.isPresent());
		loc = result.get();
		assertEquals(4, loc.getX(), delta);
		assertEquals(4, loc.getY(), delta);

		result = underTest.look(pos, AngleUtils.RADIANS_90, 250).join();
		assertTrue(result.isPresent());
		loc = result.get();
		assertEquals(0, loc.getX(), delta);
		assertEquals(2, loc.getY(), delta);

		result = underTest.look(pos, AngleUtils.RADIANS_135, 250).join();
		assertTrue(result.isPresent());
		loc = result.get();
		assertEquals(-2, loc.getX(), delta);
		assertEquals(2, loc.getY(), delta);

		result = underTest.look(pos, AngleUtils.RADIANS_180, 250).join();
		assertTrue(result.isPresent());
		loc = result.get();
		assertEquals(-4, loc.getX(), delta);
		assertEquals(0, loc.getY(), delta);

		result = underTest.look(pos, AngleUtils.RADIANS_225, 250).join();
		assertTrue(result.isPresent());
		loc = result.get();
		assertEquals(-2, loc.getX(), delta);
		assertEquals(-2, loc.getY(), delta);

		result = underTest.look(pos, AngleUtils.RADIANS_270, 250).join();
		assertTrue(result.isPresent());
		loc = result.get();
		assertEquals(0, loc.getX(), delta);
		assertEquals(-2, loc.getY(), delta);

		result = underTest.look(pos, AngleUtils.RADIANS_315, 250).join();
		assertTrue(result.isPresent());
		loc = result.get();
		System.out.println(loc);
		assertEquals(2, loc.getX(), delta);
		assertEquals(-2, loc.getY(), delta);
	}

	class ObstacleImpl implements Obstacle {

		final UUID uuid;
		final Geometry geom;

		ObstacleImpl(double x, double y) {
			uuid = UUID.randomUUID();
			geom = underTest.getContext().geometryUtils.asPoint(Location.from(x, y));
		}

		@Override
		public Literal wkt() {
			return underTest.getContext().graphGeomFactory.asWKT(geom);
		}

		@Override
		public Geometry geom() {
			return geom;
		}

		@Override
		public UUID uuid() {
			return uuid;
		}

		@Override
		public Resource rdf() {
			return ResourceFactory.createResource("urn:uuid:" + uuid().toString());
		}

		@Override
		public String toString() {
			return geom.toString();
		}
	}
}
