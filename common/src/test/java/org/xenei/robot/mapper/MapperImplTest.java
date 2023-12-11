package org.xenei.robot.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Coordinates;
import org.locationtech.jts.geom.Geometry;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.ScaleInfo;
import org.xenei.robot.common.mapping.CoordinateMap;
import org.xenei.robot.common.mapping.Map;
import org.xenei.robot.common.mapping.Mapper;
import org.xenei.robot.common.planning.Solution;
import org.xenei.robot.common.planning.Step;
import org.xenei.robot.common.testUtils.FakeDistanceSensor1;
import org.xenei.robot.common.testUtils.CoordinateUtils;
import org.xenei.robot.common.testUtils.FakeDistanceSensor;
import org.xenei.robot.common.testUtils.MapLibrary;
import org.xenei.robot.common.utils.AngleUtils;
import org.xenei.robot.common.utils.CoordUtils;

public class MapperImplTest {
    
    private ArgumentCaptor<Coordinate> coordinateCaptor = ArgumentCaptor.forClass(Coordinate.class);
    private ArgumentCaptor<Step> stepCaptor = ArgumentCaptor.forClass(Step.class);


//    @Test
//    public void processSensorTestA() {
//        Map map = new MapImpl(ScaleInfo.DEFAULT);
//        MapperImpl underTest = new MapperImpl(map);
//        CoordinateMap data = MapLibrary.map2('#');
//        FakeDistanceSensor sensor = new FakeDistanceSensor1(MapLibrary.map2('#'));
//        List<Geometry> obsts = sensor.map().getObstacles().collect(Collectors.toList());
//        Position position = new Position(MapImplTest.p);
//        Solution solution = new Solution();
//        // List<Coordinate> obstacles = MapImplTest.obstacleList();
//        sensor.setPosition(position);
//        Location[] locs = sensor.sense();
//        Arrays.stream(locs).map(l -> position.plus(l))
//                .forEach(l -> MapImplTest.assertCoordinateInObstacles(obsts, l.getCoordinate()));
//
//        underTest.processSensorData(position, MapImplTest.t, solution, locs);
//        int idx[] = { 0 };
//        for (Geometry g : map.getObstacles()) {
//            assertTrue(testObstacleInObstacles(obsts, g.getCentroid()), () -> "missing " + idx[0]);
//            idx[0]++;
//        }
//        // map.getObstacles().forEach( o -> assertTrue(
//        // testObstacleInObstacles(obsts,o), ()->"missing obstacle"));
//    }

    private static boolean testObstacleInObstacles(Collection<? extends Geometry> obsts, Geometry o) {
        boolean found = false;
        for (Geometry geom : obsts) {
            if (geom.contains(o)) {
                found = true;
                break;
            }
        }
        return found;
    }
    
    @Test
    public void processSensorDataTest_TooClose() {
       
        Position currentPosition = new Position(-1,-3, AngleUtils.RADIANS_90);
        Coordinate target = new Coordinate( -1.0, 1.0 );
        Solution solution = new Solution();
        Map map = Mockito.mock(Map.class);
        when(map.getScale()).thenReturn(ScaleInfo.DEFAULT);
        Mapper underTest = new MapperImpl(map);
        
        // an obstacle one unit away is too close so no target generated.
        Location[] obstacles = { new Location(CoordUtils.fromAngle(0,1)) };
        Optional<Location> result = underTest.processSensorData(currentPosition, target, solution, obstacles);
        // verify obstacle was added
        verify(map).addObstacle(coordinateCaptor.capture());
        CoordinateUtils.assertEquivalent(currentPosition.plus(obstacles[0]), coordinateCaptor.getValue());
        assertTrue(result.isEmpty());
        
    }
    
    @Test
    public void processSensorDataTest_IntermediateTarget() {
       
        Position currentPosition = new Position(-1,-3, AngleUtils.RADIANS_90);
        Coordinate target = new Coordinate( -1.0, 1.0 );
        Solution solution = new Solution();
        Map map = Mockito.mock(Map.class);
        when(map.getScale()).thenReturn(ScaleInfo.DEFAULT);
        Mapper underTest = new MapperImpl(map);
        
        Location[] obstacles = { new Location(CoordUtils.fromAngle(0,2)) };
        Optional<Location> result = underTest.processSensorData(currentPosition, target, solution, obstacles);
        // verify obstacle was added
        verify(map).addObstacle(coordinateCaptor.capture());
        CoordinateUtils.assertEquivalent(currentPosition.plus(obstacles[0]), coordinateCaptor.getValue());
        assertFalse(result.isEmpty());
        
    }
        
        //Location[] obstacles = { new Location(CoordUtils.fromAngle(0,1)) };
        
            /*
             *     public Optional<Location> processSensorData(Position currentPosition, Coordinate target, Solution solution, Location[] obstacles) {
            // next target set if collision detected.
            ObstacleMapper obstacleMapper = new ObstacleMapper(currentPosition, target);
            LOG.trace("Sense position: {}", currentPosition);
            double halfScale = map.getScale().getScale()/2;
            // get the sensor readings and add obstacles to the map
            //@formatter:off
            Arrays.stream(obstacles)
                    // filter out any range < 1.0
                    .filter(c -> !DoubleUtils.inRange(c.range(), map.getScale().getTolerance()))
                    // create absolute coordinates
                    .map(c -> {
                        LOG.trace( "Checking {}", c);
                        return currentPosition.plus(c);
                    })
                    .filter( new UniqueFilter<Location>() )
                    .map(obstacleMapper::map)
                    // filter out non entries
                    .filter(c -> c.isPresent() && !c.get().near(currentPosition, .5))
                    .map(Optional::get)
                    .filter( new UniqueFilter<Location>() )
                    .forEach(c -> recordMapPoint(currentPosition, new Step( c.getCoordinate(), c.distance(target))));
            //@formatter::on
           
            if (obstacleMapper.nextTarget.isPresent() && !solution.isEmpty()) {
                map.cutPath(solution.end(), target);
                map.addPath(solution.end(), obstacleMapper.nextTarget.get().getCoordinate());
                
                if (currentPosition.distance(obstacleMapper.nextTarget.get()) >= currentPosition.distance(target)) {
                    obstacleMapper.nextTarget = Optional.empty();
                } else if (currentPosition.equals2D(target, halfScale)) {
                    obstacleMapper.nextTarget = Optional.empty();
                }
            }
            
            //System.out.println( MapReports.dumpModel((MapImpl) map));
           // ((MapImpl)map).doCluster(Namespace.Obst, 2.5, 2);

            return obstacleMapper.nextTarget;
        }
             */
 
}
