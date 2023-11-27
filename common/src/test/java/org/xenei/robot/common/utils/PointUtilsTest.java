package org.xenei.robot.common.utils;


import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import mil.nga.sf.Point;

public class PointUtilsTest {

    @Test
    public void angleToTest() {
        Point c = new Point(0, -2);
        Point p = new Point(2,-1);
        double angle2 = PointUtils.angleTo(c, p);
        assertEquals( 0.4636476090008062, angle2);
    }
    
}
