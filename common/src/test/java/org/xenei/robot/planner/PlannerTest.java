package org.xenei.robot.planner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Coordinate;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.SolutionTest;
import org.xenei.robot.common.DistanceSensor;
import org.xenei.robot.common.mapping.CoordinateMap;
import org.xenei.robot.common.mapping.Map;
import org.xenei.robot.common.planning.Solution;
import org.xenei.robot.common.planning.Step;
import org.xenei.robot.common.testUtils.FakeDistanceSensor;
import org.xenei.robot.common.testUtils.FakeDistanceSensor2;
import org.xenei.robot.common.testUtils.MapLibrary;
import org.xenei.robot.mapper.MapImpl;
import org.xenei.robot.mapper.MapImplTest;
import org.xenei.robot.mapper.MapperImpl;
import org.xenei.robot.mapper.visualization.MapViz;

public class PlannerTest {

    private PlannerImpl underTest;
    private MapViz mapViz;

    @Test
    public void setTargetTest() {
        CoordinateMap cmap = MapLibrary.map2('#');
        Map map = new MapImpl(1);
        Location origin = new Location(0, 0);
        underTest = new PlannerImpl(map, origin);
        for (int x = 0; x <= 13; x++) {
            for (int y = 0; y <= 15; y++) {
                Location c = new Location(x, y);
                if (!cmap.isEnabled(c)) {
                    underTest.setTarget(c);
                    verifyState(map, cmap);
                    assertEquals(0, underTest.getSolution().stepCount());
                }
            }
        }
    }

    private Function<Geometry,Coordinate> toCoord =  g -> {
        Point p = g.getCentroid();
        return new Coordinate(p.getX(), p.getY());
    };
    
    private CoordinateMap verifyState(Map map, CoordinateMap cmap) {
        
        for (Geometry c : map.getObstacles()) {
            assertTrue(cmap.isEnabled(toCoord.apply(c)), () -> c + " should have been sensed.");
        }
        CoordinateMap sensedMap = new CoordinateMap(cmap.scale());
        sensedMap.enable(map.getObstacles().stream().map( toCoord ).collect(Collectors.toList()), 'x');

        for (Step pr : map.getTargets()) {
            assertFalse(sensedMap.isEnabled(pr), () -> "Plan record " + pr + " should not have been sensed");
        }

        underTest.getSolution().stream().forEach(
                c -> assertFalse(sensedMap.isEnabled(c), () -> "Path record " + c + " should not have been enabled"));
        return sensedMap;
    }

    @Test
    public void stepTestMap2() {
        Coordinate[] expectedSolution = {
                new Coordinate(-1.0, -3.0), new Coordinate(-0.9999999999999999, -2.0), new Coordinate(-0.4472135954999578, -1.8944271909999157), new Coordinate(0.2928932188134528, -1.7071067811865475), new Coordinate(-1.5527864045000421, -1.8944271909999157), new Coordinate(-4.000468632496236, -1.9693888030933675), new Coordinate(-4.547420028549094, -0.8917238190389993), new Coordinate(-1.0, 1.0)
        };
        FakeDistanceSensor sensor = new FakeDistanceSensor(MapLibrary.map2('#'));
        Map map = new MapImpl(1);
        MapperImpl mapper = new MapperImpl(map);

        Location finalCoord = new Location(-1, 1);
        Location startCoord = new Location(-1, -3);

        underTest = new PlannerImpl(map, startCoord, finalCoord);
        mapViz = new MapViz( 100, map, underTest.getSolution());
        underTest.addListener( () -> mapViz.redraw() );
       
        int stepCount = 0;
        int maxLoops = 100;
        sensor.setPosition(underTest.getCurrentPosition());
        mapper.processSensorData(underTest.getCurrentPosition(), underTest.getTarget(), underTest.getSolution(), sensor.sense());

        while (underTest.step()) {
            if (maxLoops < stepCount++) {
                fail("Did not find solution in " + maxLoops + " steps");
            }
            double angle = underTest.getCurrentPosition().headingTo(underTest.getTarget());
            underTest.changeCurrentPosition(new Position(underTest.getTarget(), angle));
            sensor.setPosition(underTest.getCurrentPosition());
            mapper.processSensorData(underTest.getCurrentPosition(), underTest.getTarget(), underTest.getSolution(), sensor.sense());
        }
        assertEquals(SolutionTest.expectedSolution.length - 1, underTest.getSolution().stepCount());
        assertTrue(startCoord.equals2D(underTest.getSolution().start()));
        assertTrue(finalCoord.equals2D(underTest.getSolution().end()));
        List<Coordinate> solution = underTest.getSolution().stream().collect(Collectors.toList());
        assertEquals(SolutionTest.expectedSolution.length, solution.size());
        for (int i = 0; i < solution.size(); i++) {
            final int j = i;
            assertTrue(expectedSolution[i].equals2D(solution.get(i), 0.00001), ()-> 
            String.format( "failed at %s: %s == %s +/- 0.00001", j, SolutionTest.expectedSolution[j], solution.get(j)));
        }
    }

    @Test
    public void stepTestMap3() {
        Coordinate[] expectedSimpleSolution = { new Coordinate(-1, -3), new Coordinate(-3, -2), new Coordinate(-4, -1), new Coordinate(-4, 0),
                new Coordinate(-1, 1) };
        FakeDistanceSensor2 sensor = new FakeDistanceSensor2(MapLibrary.map3('#'));
        Map map = new MapImpl(1);
        MapperImpl mapper = new MapperImpl(map);

        Location finalCoord = new Location(-1, 1);
        Location startCoord = new Location(-1, -3);

        underTest = new PlannerImpl(map, startCoord, finalCoord);
        mapViz = new MapViz( 100, map, underTest.getSolution());
        underTest.addListener( () -> mapViz.redraw() );
        
        int stepCount = 0;
        int maxLoops = 100;
        sensor.setPosition(underTest.getCurrentPosition());
        Optional<Location> shortTarget = mapper.processSensorData(underTest.getCurrentPosition(), underTest.getTarget(), underTest.getSolution(), sensor.sense());
        if (shortTarget.isPresent()) {
            underTest.replaceTarget(shortTarget.get());
        }
        underTest.notifyListeners();
        while (underTest.step()) {
            if (maxLoops < stepCount++) {
                fail("Did not find solution in " + maxLoops + " steps");
            }
            shortTarget = mapper.processSensorData(underTest.getCurrentPosition(), underTest.getTarget(), underTest.getSolution(), sensor.sense());
            if (shortTarget.isPresent()) {
                underTest.replaceTarget(shortTarget.get());
            }
            double angle = underTest.getCurrentPosition().headingTo(underTest.getTarget());
            underTest.changeCurrentPosition(new Position(underTest.getTarget(), angle));
            sensor.setPosition(underTest.getCurrentPosition());
            shortTarget = mapper.processSensorData(underTest.getCurrentPosition(), underTest.getTarget(), underTest.getSolution(), sensor.sense());
            if (shortTarget.isPresent()) {
                underTest.replaceTarget(shortTarget.get());
            }
            underTest.notifyListeners();
        }
        assertEquals(22, underTest.getSolution().stepCount());
        assertEquals(33.29126786466034, underTest.getSolution().cost());
        assertTrue(startCoord.equals2D(underTest.getSolution().start()));
        assertTrue(finalCoord.equals2D(underTest.getSolution().end()));
        underTest.getSolution().simplify(map::clearView);
        assertEquals(7.812559200041265, underTest.getSolution().cost());
        assertTrue(startCoord.equals2D(underTest.getSolution().start()));
        assertTrue(finalCoord.equals2D(underTest.getSolution().end()));
        List<Coordinate> solution = underTest.getSolution().stream().collect(Collectors.toList());
        assertEquals(expectedSimpleSolution.length, solution.size());
        for (int i = 0; i < solution.size(); i++) {
            assertTrue(expectedSimpleSolution[i].equals2D(solution.get(i)));
        }
    }
}
