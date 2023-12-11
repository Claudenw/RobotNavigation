package org.xenei.robot;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.xenei.robot.common.DistanceSensor;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.Mover;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.ScaleInfo;
import org.xenei.robot.common.SolutionTest;
import org.xenei.robot.common.mapping.CoordinateMap;
import org.xenei.robot.common.mapping.Map;
import org.xenei.robot.common.mapping.Mapper;
import org.xenei.robot.common.planning.Step;
import org.xenei.robot.common.planning.Planner;
import org.xenei.robot.common.planning.Planner.Diff;
import org.xenei.robot.common.planning.Solution;
import org.xenei.robot.common.testUtils.FakeMover;
import org.xenei.robot.common.testUtils.CoordinateUtils;
import org.xenei.robot.common.testUtils.FakeDistanceSensor;
import org.xenei.robot.common.testUtils.FakeDistanceSensor1;
import org.xenei.robot.common.testUtils.FakeDistanceSensor2;
import org.xenei.robot.common.testUtils.MapLibrary;
import org.xenei.robot.common.utils.AngleUtils;
import org.xenei.robot.mapper.MapImpl;
import org.xenei.robot.mapper.MapReports;
import org.xenei.robot.mapper.MapperImpl;
import org.xenei.robot.mapper.rdf.Namespace;
import org.xenei.robot.mapper.visualization.MapViz;
import org.xenei.robot.planner.PlannerImpl;


public class ProcessorTest {

    private final MapViz mapViz;
    private final Map map;
    private Planner planner;
    private final Mapper mapper;
    private FakeDistanceSensor sensor;
    
    
//    private Processor underTest;
//    private static final Location finalCoord = new Location(-1, 1);
//    private static final Location startCoord = new Location(-1, -3);
//
//    @BeforeEach
//    public void setup() {
//        Mover mover = new FakeMover(new Position(startCoord), 1);
//        FakeDistanceSensor sensor = new FakeDistanceSensor1(MapLibrary.map2('#'));
//        underTest = new Processor(sensor, mover);
//    }
//
//    @Test
//    @Disabled( "Rework to use messages?")
//    public void moveToTest() {
//        assertTrue(underTest.moveTo(finalCoord));
//        List<Coordinate> solution = underTest.getSolution().collect(Collectors.toList());
//        assertArrayEquals(SolutionTest.expectedSimplification, solution.toArray());
//        assertTrue(finalCoord.equals2D(solution.get(solution.size() - 1)));
//    }
//
//    @Test
//    @Disabled( "Rework to use messages?")
//    public void setTargetWhileMovingTest() {
//        Location nextCoord = new Location(4, 4);
//        underTest.moveTo(finalCoord);
//        
//        underTest.setTarget(nextCoord);
//
//        List<Coordinate> solution = underTest.getSolution().collect(Collectors.toList());
//        assertTrue(nextCoord.equals2D(solution.get(solution.size() - 1)));
//    }


    ProcessorTest() {
        map = new MapImpl(ScaleInfo.DEFAULT);
        mapViz = new MapViz(100, map, () -> planner.getSolution());
        mapper = new MapperImpl(map);
        planner = new PlannerImpl(map, new Location(-1, -3 ));
        sensor = new FakeDistanceSensor2(MapLibrary.map2('#'), AngleUtils.RADIANS_45);
        planner.addListener( () -> ((FakeDistanceSensor)sensor).setPosition(planner.getCurrentPosition()));
    }

    private void processSensor() {
        sensor.setPosition(planner.getCurrentPosition());
        Optional<Location> maybeTarget = mapper.processSensorData(planner.getCurrentPosition(), planner.getTarget(),
                planner.getSolution(), sensor.sense());
        if (maybeTarget.isPresent()) {
            planner.replaceTarget(maybeTarget.get());
        }
    }

    private void doTest(Location startCoord, Location finalCoord) {
        planner = new PlannerImpl(map, startCoord, finalCoord);
        planner.addListener( () -> sensor.setPosition(planner.getCurrentPosition()));
        planner.addListener( () -> mapViz.redraw(planner.getTarget()));

        Mover mover = new FakeMover(planner.getCurrentPosition(), 1);

        processSensor();

        int stepCount = 0;
        int maxLoops = 100;
        while (planner.getTarget() != null) {
            System.out.println(MapReports.dumpModel((MapImpl) mapper.getMap()));
            Diff diff = planner.selectTarget();
            if (diff.didChange()) {
                processSensor();
            }
            // move
            planner.changeCurrentPosition(mover.move(new Location(planner.getTarget())));
            sensor.setPosition(planner.getCurrentPosition());
            processSensor();
            if (mapper.getMap().clearView(planner.getCurrentPosition().getCoordinate(), planner.getRootTarget())) {
                planner.replaceTarget(planner.getRootTarget());
            }

            if (maxLoops < stepCount++) {
                fail("Did not find solution in " + maxLoops + " steps");
            }
            planner.notifyListeners();
        }
        planner.notifyListeners();
        Solution solution = planner.getSolution();
        CoordinateUtils.assertEquivalent( startCoord, solution.start(), map.getScale().getTolerance());
        CoordinateUtils.assertEquivalent( startCoord, solution.start(), map.getScale().getTolerance());
    }

    @Test
    public void stepTestMap2() {
//        Coordinate[] expectedSolution = { new Coordinate(-1.0, -3.0), new Coordinate(-0.9999999999999999, -2.0),
//                new Coordinate(-0.4472135954999578, -1.8944271909999157),
//                new Coordinate(0.2928932188134528, -1.7071067811865475),
//                new Coordinate(-1.5527864045000421, -1.8944271909999157),
//                new Coordinate(-4.000468632496236, -1.9693888030933675),
//                new Coordinate(-4.547420028549094, -0.8917238190389993), new Coordinate(-1.0, 1.0) };
        sensor = new FakeDistanceSensor1(MapLibrary.map2('#'));
        map.clear(Namespace.UnionModel.getURI());

        Location finalCoord = new Location(-1, 1);
        Location startCoord = new Location(-1, -3);
        doTest(startCoord, finalCoord);
//
//        assertEquals(SolutionTest.expectedSolution.length - 1, planner.getSolution().stepCount());
//        List<Coordinate> solution = planner.getSolution().stream().collect(Collectors.toList());
//        assertEquals(SolutionTest.expectedSolution.length, solution.size());
//        for (int i = 0; i < solution.size(); i++) {
//            final int j = i;
//            assertTrue(expectedSolution[i].equals2D(solution.get(i), 0.00001),
//                    () -> String.format("failed at %s: %s == %s +/- 0.00001", j, SolutionTest.expectedSolution[j],
//                            solution.get(j)));
//        }
    }

    @Test
    public void stepTestMap3() {
//        Coordinate[] expectedSimpleSolution = { new Coordinate(-1, -3), new Coordinate(-3, -2), new Coordinate(-4, -1),
//                new Coordinate(-4, 0), new Coordinate(-1, 1) };
        sensor = new FakeDistanceSensor2(MapLibrary.map3('#'), AngleUtils.RADIANS_45);
        map.clear(Namespace.UnionModel.getURI());

        Location finalCoord = new Location(-1, 1);
        Location startCoord = new Location(-1, -3);
        doTest(startCoord, finalCoord);
//
//        assertEquals(25, underTest.getSolution().stepCount());
//        assertEquals(33.29126786466034, underTest.getSolution().cost());
//
//        underTest.getSolution().simplify(map::clearView);
//        assertEquals(7.812559200041265, underTest.getSolution().cost());
//        assertTrue(startCoord.equals2D(underTest.getSolution().start()));
//        assertTrue(finalCoord.equals2D(underTest.getSolution().end()));
//        List<Coordinate> solution = underTest.getSolution().stream().collect(Collectors.toList());
//        assertEquals(expectedSimpleSolution.length, solution.size());
//        for (int i = 0; i < solution.size(); i++) {
//            assertTrue(expectedSimpleSolution[i].equals2D(solution.get(i)));
//        }
    }
}
