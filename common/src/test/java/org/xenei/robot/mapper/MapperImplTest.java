package org.xenei.robot.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.ScaleInfo;
import org.xenei.robot.common.mapping.Map;
import org.xenei.robot.common.mapping.Mapper;
import org.xenei.robot.common.planning.Solution;
import org.xenei.robot.common.planning.Step;
import org.xenei.robot.common.testUtils.CoordinateUtils;
import org.xenei.robot.common.utils.AngleUtils;
import org.xenei.robot.common.utils.CoordUtils;

public class MapperImplTest {

    private ArgumentCaptor<Coordinate> coordinateCaptor = ArgumentCaptor.forClass(Coordinate.class);
    private ArgumentCaptor<Step> stepCaptor = ArgumentCaptor.forClass(Step.class);
    private ArgumentCaptor<Set> setCaptor = ArgumentCaptor.forClass(Set.class);
    private ArgumentCaptor<Double> doubleCaptor = ArgumentCaptor.forClass(Double.class);
    
    private double buffer = .5;

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

        Position currentPosition = Position.from(-1, -3, AngleUtils.RADIANS_90);
        Coordinate target = new Coordinate(-1, 1);
        Map map = Mockito.mock(Map.class);
        when(map.getScale()).thenReturn(ScaleInfo.DEFAULT);
        when(map.addObstacle(any())).thenReturn( new Coordinate(1,0));
        Mapper underTest = new MapperImpl(map);

        // an obstacle one unit away is too close so no target generated.
        Location[] obstacles = { Location.from(CoordUtils.fromAngle(0, 1)) };
        Collection<Step> result = underTest.processSensorData(currentPosition, buffer, target, obstacles);
        // should not look at result here.
        assertTrue(result.isEmpty());
        
        verify(map).updateIsIndirect(coordinateCaptor.capture(), doubleCaptor.capture(), setCaptor.capture());
        assertEquals(1,setCaptor.getValue().size());
        CoordinateUtils.assertEquivalent(new Coordinate(1,0), setCaptor.getValue().iterator().next());
        
        // verify obstacle was added
        verify(map).addObstacle(coordinateCaptor.capture());
        CoordinateUtils.assertEquivalent(new Coordinate(-1, -2), coordinateCaptor.getValue(),
                ScaleInfo.DEFAULT.getResolution());
    }

    @Test
    public void processSensorDataTest_IntermediateTarget() {

        Position currentPosition = Position.from(-1, -3, AngleUtils.RADIANS_90);
        Coordinate target = new Coordinate(-1, 1);
        Map map = Mockito.mock(Map.class);
        when(map.getScale()).thenReturn(ScaleInfo.DEFAULT);
        when(map.addObstacle(any())).thenReturn( new Coordinate(2,0));
        when(map.adopt(any())).thenReturn( new Coordinate(-1, -2));
        when(map.isObstacle(any())).thenReturn( false );
        Mapper underTest = new MapperImpl(map);

        Location[] obstacles = { Location.from(CoordUtils.fromAngle(0, 2)) };
        Collection<Step> result = underTest.processSensorData(currentPosition, buffer, target, obstacles);
        assertFalse(result.isEmpty());
        
        // verify indirects updated
        verify(map).updateIsIndirect(coordinateCaptor.capture(), doubleCaptor.capture(), setCaptor.capture());
        assertEquals(1,setCaptor.getValue().size());
        CoordinateUtils.assertEquivalent(new Coordinate(2,0), setCaptor.getValue().iterator().next());

        // verify obstacle was added
        verify(map).addObstacle(coordinateCaptor.capture());
        CoordinateUtils.assertEquivalent(new Coordinate(-1, -1), coordinateCaptor.getValue(),
                ScaleInfo.DEFAULT.getResolution());
        
        // verify coord was added
        ArgumentCaptor<Boolean> one = ArgumentCaptor.forClass(Boolean.class);
        ArgumentCaptor<Boolean> two = ArgumentCaptor.forClass(Boolean.class);
        verify(map).addCoord(coordinateCaptor.capture(), doubleCaptor.capture(), one.capture(), two.capture());
        CoordinateUtils.assertEquivalent(new Coordinate(-1,-2), coordinateCaptor.getValue());

    }
}
