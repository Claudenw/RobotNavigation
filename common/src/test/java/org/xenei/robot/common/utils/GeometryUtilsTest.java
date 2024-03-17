package org.xenei.robot.common.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.xenei.robot.common.FrontsCoordinate;
import org.xenei.robot.common.FrontsCoordinateTest;
import org.xenei.robot.common.ScaleInfo;
import org.xenei.robot.common.testUtils.CoordinateUtils;
import org.xenei.robot.common.testUtils.TestChassisInfo;
import org.xenei.robot.common.testUtils.TestingPositionSupplier;

public class GeometryUtilsTest {
    
    private static RobutContext ctxt = new RobutContext(ScaleInfo.DEFAULT, TestChassisInfo.DEFAULT);

    @ParameterizedTest
    @MethodSource("polygonParameters")
    public void asPolygonTest(String name, Polygon geom, Coordinate[] expected) {
        Coordinate[] actual = geom.getCoordinates();
        assertEquals(expected.length, actual.length);
        for (int i = 0; i < actual.length; i++) {
            CoordinateUtils.assertEquivalent(expected[i], actual[i], 0.001);
        }
    }

    private static Stream<Arguments> polygonParameters() {
        List<Arguments> lst = new ArrayList<Arguments>();

        Coordinate center = new Coordinate(1, 1);

        Coordinate[] expected = new Coordinate[] { new Coordinate(1.25, 1.0), new Coordinate(1.125, 1.216),
                new Coordinate(0.875, 1.216), new Coordinate(0.75, 1.0), new Coordinate(0.875, 0.783),
                new Coordinate(1.125, 0.783), new Coordinate(1.25, 1.0) };

        lst.add(Arguments.of("radius", ctxt.geometryUtils.asPolygon(center, .25), expected));

        lst.add(Arguments.of("hasCoord radius", ctxt.geometryUtils.asPolygon(FrontsCoordinateTest.make(center), .25),
                expected));

        expected = new Coordinate[] { new Coordinate(1.25, 1.25), new Coordinate(0.75, 1.25),
                new Coordinate(0.75, 0.75), new Coordinate(1.25, 0.75), new Coordinate(1.25, 1.25) };

        lst.add(Arguments.of("radius edges", ctxt.geometryUtils.asPolygon(center, .25, 4), expected));

        lst.add(Arguments.of("hasCoord radius edges", ctxt.geometryUtils.asPolygon(FrontsCoordinateTest.make(1, 1), .25, 4),
                expected));

        expected = new Coordinate[] { new Coordinate(1, 1.25), new Coordinate(0.75, 1), new Coordinate(0.75, 0.75),
                new Coordinate(1.25, 0.75), new Coordinate(1, 1.25) };

        Polygon geom = ctxt.geometryUtils.asPolygon(new Coordinate(1, 1.25), new Coordinate(0.75, 1),
                new Coordinate(0.75, 0.75), new Coordinate(1.25, 0.75), new Coordinate(1, 1.25));

        lst.add(Arguments.of("coord array", geom, expected));
        lst.add(Arguments.of("coord collection", ctxt.geometryUtils.asPolygon(Arrays.asList(expected)), expected));

        List<FrontsCoordinate> hList = Arrays.stream(expected).map(FrontsCoordinateTest::make)
                .collect(Collectors.toList());

        lst.add(Arguments.of("hasCoord array", ctxt.geometryUtils.asPolygon(hList.toArray(new FrontsCoordinate[hList.size()])),
                expected));


        lst.add(Arguments.of("hasCoord", ctxt.geometryUtils.asPolygon(
                hList.get(0), hList.get(1), hList.get(2), hList.get(3), hList.get(4)),
                expected));
        
        return lst.stream();
    }

    @ParameterizedTest
    @MethodSource("pointParameters")
    public void asPointTest(String name, Point geom, Coordinate expected) {
        Coordinate[] actual = geom.getCoordinates();
        assertEquals(1, actual.length);
        CoordinateUtils.assertEquivalent(expected, actual[0], 0.001);
    }

    private static Stream<Arguments> pointParameters() {
        List<Arguments> lst = new ArrayList<Arguments>();

        Coordinate center = new Coordinate(1, 1);

        lst.add(Arguments.of("coord", ctxt.geometryUtils.asPoint(center), center));

        lst.add(Arguments.of("hasCoord", ctxt.geometryUtils.asPoint(FrontsCoordinateTest.make(center)), center));

        return lst.stream();
    }
    
    @Test
    public void asLineTest() {
        Coordinate[] expected = new Coordinate[] { new Coordinate(1,1), new Coordinate(3,3), new Coordinate(-4,-4) };
        
        LineString ls = ctxt.geometryUtils.asLine( expected[0], expected[1], expected[2] );
        Coordinate[] actual = ls.getCoordinates();
        assertEquals(expected.length, actual.length);
        for (int i = 0; i < actual.length; i++) {
            CoordinateUtils.assertEquivalent(expected[i], actual[i], 0.001);
        }
    }

//
//    public static Coordinate[] pathSegment(double width, Coordinate a, Coordinate b) {
//
//        double angle = CoordUtils.angleBetween(a, b);
//        double r = width / 2;
//        Coordinate cUp = CoordUtils.fromAngle(angle + AngleUtils.RADIANS_90, r);
//        Coordinate cDown = CoordUtils.fromAngle(angle - AngleUtils.RADIANS_90, r);
//        return new Coordinate[] { CoordUtils.add(a, cUp), CoordUtils.add(b, cUp), CoordUtils.add(a, cDown),
//                CoordUtils.add(b, cDown) };
//    }
//
//    public static Polygon asPath(Coordinate... points) {
//        int limit = points.length - 1;
//        Stack<Coordinate> down = new Stack<>();
//        List<Coordinate> all = new ArrayList<>();
//        all.add(points[0]);
//        down.push(points[0]);
//        for (int i = 0; i < limit; i++) {
//            Coordinate[] segment = pathSegment(.5, points[i], points[i + 1]);
//            all.add(segment[0]);
//            all.add(segment[1]);
//            down.push(segment[2]);
//            down.push(segment[3]);
//        }
//        all.add(points[limit]);
//        while (!down.empty()) {
//            all.add(down.pop());
//        }
//        return asPolygon(all);
//    }
//
//    public static Polygon asPath(Collection<Coordinate> points) {
//        return asPath(points.toArray(new Coordinate[points.size()]));
//    }
//
//    public static Polygon asPath(HasCoordinate... points) {
//        return asPath(asCollection(points));
//    }
//


}
