package org.xenei.robot.mapper;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.xenei.robot.common.ScaleInfo;
import org.xenei.robot.common.testUtils.TestChassisInfo;
import org.xenei.robot.common.utils.RobutContext;

public class PointCloudSorterTest {
    
    Coordinate[] coordinates = { new Coordinate(-1.5, -4.0), new Coordinate(1.5, -4.0), new Coordinate(3.0, -2.0),
            new Coordinate(3.0, 0.0), new Coordinate(3.0, -2.5), new Coordinate(-4.0, -4.0), new Coordinate(3.0, -1.5),
            new Coordinate(2.0, 0.5), new Coordinate(2.0, -0.5), new Coordinate(1.0, -1.0), new Coordinate(-1.0, -1.0),
            new Coordinate(-2.5, -4.0), new Coordinate(2.5, -4.0), new Coordinate(-3.0, -4.0),
            new Coordinate(3.0, -4.0), new Coordinate(-4.0, -0.5), new Coordinate(-0.5, -4.0),
            new Coordinate(0.5, -4.0), new Coordinate(3.0, -3.0), new Coordinate(2.0, -1.0), new Coordinate(0.0, -1.0),
            new Coordinate(-2.0, -1.0), new Coordinate(2.5, -0.5), new Coordinate(3.0, -3.5), new Coordinate(2.5, 0.5),
            new Coordinate(-4.0, -1.5), new Coordinate(3.0, -0.5), new Coordinate(3.0, 0.5), new Coordinate(1.5, -1.0),
            new Coordinate(-1.5, -1.0), new Coordinate(-3.5, -4.0), new Coordinate(-4.0, -1.0),
            new Coordinate(-1.0, -4.0), new Coordinate(1.0, -4.0), new Coordinate(2.0, 0.0), new Coordinate(-2.5, -1.0),
            new Coordinate(-4.0, -3.0), new Coordinate(3.0, -1.0), new Coordinate(-3.0, -1.0),
            new Coordinate(-4.0, -3.5), new Coordinate(-4.0, -2.0), new Coordinate(-2.0, -4.0),
            new Coordinate(0.0, -4.0), new Coordinate(2.0, -4.0), new Coordinate(0.5, -1.0), new Coordinate(-0.5, -1.0),
            new Coordinate(-4.0, -2.5) };

    RobutContext ctxt = new RobutContext(ScaleInfo.DEFAULT, TestChassisInfo.DEFAULT);
    
    @Test
    public void walkTest() {
        Set<Coordinate> coords = new HashSet<>();
        coords.addAll(Arrays.asList(coordinates));
        PointCloudSorter pcs = new PointCloudSorter(ctxt, coords);
      
        Geometry g = pcs.walk();
        
    }
}
