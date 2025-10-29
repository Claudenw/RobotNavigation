//package org.xenei.robot.mapper;
//
//import java.util.Arrays;
//import java.util.LinkedHashSet;
//import java.util.Random;
//import java.util.Set;
//import java.util.stream.Collectors;
//
//import org.junit.jupiter.api.Test;
//import org.locationtech.jts.algorithm.hull.ConcaveHull;
//import org.locationtech.jts.geom.Coordinate;
//import org.locationtech.jts.geom.Geometry;
//import org.locationtech.jts.geom.GeometryFactory;
//import org.locationtech.jts.geom.LineString;
//import org.locationtech.jts.geom.MultiLineString;
//import org.locationtech.jts.geom.Point;
//import org.xenei.robot.common.ScaleInfo;
//import org.xenei.robot.common.mapping.MapPathI;
//import org.xenei.robot.common.utils.DoubleUtils;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//
//public class PointCloudSorterTest {
//
//    PointCloudSorter underTest = new PointCloudSorter(DoubleUtils.SQRT2);
//    GeometryFactory factory = new GeometryFactory(ScaleInfo.DEFAULT.getPrecisionModel());
//
//    @Test
//    public void walkTest() {
//        Coordinate[] coordinates = {new Coordinate(-1.5, -4.0), new Coordinate(1.5, -4.0), new Coordinate(3.0, -2.0),
//                new Coordinate(3.0, 0.0), new Coordinate(3.0, -2.5), new Coordinate(-4.0, -4.0),
//                new Coordinate(3.0, -1.5), new Coordinate(2.0, 0.5), new Coordinate(2.0, -0.5),
//                new Coordinate(1.0, -1.0), new Coordinate(-1.0, -1.0), new Coordinate(-2.5, -4.0),
//                new Coordinate(2.5, -4.0), new Coordinate(-3.0, -4.0), new Coordinate(3.0, -4.0),
//                new Coordinate(-4.0, -0.5), new Coordinate(-0.5, -4.0), new Coordinate(0.5, -4.0),
//                new Coordinate(3.0, -3.0), new Coordinate(2.0, -1.0), new Coordinate(0.0, -1.0),
//                new Coordinate(-2.0, -1.0), new Coordinate(2.5, -0.5), new Coordinate(3.0, -3.5),
//                new Coordinate(2.5, 0.5), new Coordinate(-4.0, -1.5), new Coordinate(3.0, -0.5),
//                new Coordinate(3.0, 0.5), new Coordinate(1.5, -1.0), new Coordinate(-1.5, -1.0),
//                new Coordinate(-3.5, -4.0), new Coordinate(-4.0, -1.0), new Coordinate(-1.0, -4.0),
//                new Coordinate(1.0, -4.0), new Coordinate(2.0, 0.0), new Coordinate(-2.5, -1.0),
//                new Coordinate(-4.0, -3.0), new Coordinate(3.0, -1.0), new Coordinate(-3.0, -1.0),
//                new Coordinate(-4.0, -3.5), new Coordinate(-4.0, -2.0), new Coordinate(-2.0, -4.0),
//                new Coordinate(0.0, -4.0), new Coordinate(2.0, -4.0), new Coordinate(0.5, -1.0),
//                new Coordinate(-0.5, -1.0), new Coordinate(-4.0, -2.5)};
//
//        Set<Point> coords = Arrays.stream(coordinates).map(this::pt).collect(Collectors.toSet());
//
//        Geometry geometry = underTest.process(coords);
//
//    }
//
//    // @Test
//    // public void sortTest() {
//    // RobutContext ctxt = new RobutContext(ScaleInfo.DEFAULT,
//    // ChassisInfoTest.DEFAULT);
//    // GeometryFactory factory = ctxt.geometryFactory;
//    // Set<GeometricObject> lst = new HashSet<>();
//    // List<Coordinate>
//    // Coordinate[] coordinates = { new Coordinate(0, 0), new Coordinate(0,5), new
//    // Coordinate(5, 0),
//    // new Coordinate(5,5)});
//    // lst.add(createObj(lineString));
//    //
//    // lineString = factory.createLineString(new Coordinate[]{new Coordinate(-1, 1),
//    // new Coordinate(1,1)});
//    // lst.add(createObj(lineString));
//    //
//    // Point point = factory.createPoint(new Coordinate(2,2));
//    // lst.add(createObj(point));
//    //
//    // PointCloudSorter pcs = new PointCloudSorter(ctxt.geometryFactory, SQRT_2,
//    // lst);
//    //
//    // Collection<Geometry> result = pcs.walk();
//    // System.out.println(result);
//    // Geometry combined = factory.buildGeometry(result);
//    // System.out.println(combined);
//    // System.out.println(combined.convexHull());
//    //
//    // lineString = factory.createLineString(new Coordinate[]{new Coordinate(1, 2),
//    // new Coordinate(2,1)});
//    // System.out.println(lineString.crosses(combined));
//    // }
//
//    private LineString ls(Coordinate c1, Coordinate c2) {
//        return factory.createLineString(new Coordinate[]{c1, c2});
//    }
//
//    private Point pt(Coordinate c) {
//        return factory.createPoint(c);
//    }
//
//    private Point pt(int x, int y) {
//        return pt(new Coordinate(x, y));
//    }
//
//    private LineString[] lsa(Coordinate... coordinates) {
//        LineString[] lines = new LineString[coordinates.length - 1];
//        Point lastPoint = factory.createPoint(coordinates[0]);
//        for (int i = 1; i < coordinates.length; i++) {
//            Point point = factory.createPoint(coordinates[i]);
//            lines[i - 1] = factory.createLineString(new Coordinate[]{lastPoint.getCoordinate(), point.getCoordinate()});
//            lastPoint = point;
//        }
//        return lines;
//    }
//
//    // Implementing Fisher–Yates shuffle
//    static <T> void shuffleArray(T[] ar) {
//        Random rnd = new Random();
//        for (int i = ar.length - 1; i > 0; i--) {
//            int index = rnd.nextInt(i + 1);
//            // Simple swap
//            T a = ar[index];
//            ar[index] = ar[i];
//            ar[i] = a;
//        }
//    }
//
//    @Test
//    public void randomOrderTest() {
//
//        final Coordinate[] coordinates = {new Coordinate(0, 0), new Coordinate(0, 1), new Coordinate(0, 2),
//                new Coordinate(0, 3), new Coordinate(0, 4), new Coordinate(0, 5)};
//
//        // create random data
//        Coordinate[] tmp = Arrays.copyOf(coordinates, coordinates.length);
//        shuffleArray(tmp);
//        LinkedHashSet<Point> data = Arrays.stream(tmp).map(this::pt)
//                .collect(Collectors.toCollection(LinkedHashSet::new));
//
//        Geometry expected = factory.createMultiLineString(lsa(coordinates));
//        LineString lineString = factory
//                .createLineString(new Coordinate[]{new Coordinate(-1, 2.5), new Coordinate(1, 2.5)});
//
//        Geometry actual = underTest.process(data);
//        assertEquals(expected, actual);
//    }
//
//    @Test
//    public void pointAndPathAdditionTest() {
//
//        MultiLineString expected = factory.createMultiLineString(new LineString[]{
//                ls(new Coordinate(0, 0), new Coordinate(0, 1)), ls(new Coordinate(0, 0), new Coordinate(1, 1)),
//                ls(new Coordinate(0, 1), new Coordinate(0, 2)), ls(new Coordinate(0, 1), new Coordinate(1, 1)),
//                ls(new Coordinate(0, 2), new Coordinate(0, 3)), ls(new Coordinate(0, 2), new Coordinate(1, 1)),
//                ls(new Coordinate(0, 3), new Coordinate(0, 4)), ls(new Coordinate(0, 4), new Coordinate(0, 5)),});
//
//        LinkedHashSet<Point> data = new LinkedHashSet<>();
//        data.add(pt(0, 0));
//        data.add(pt(0, 1));
//        data.add(pt(0, 2));
//        data.add(pt(0, 3));
//        data.add(pt(0, 4));
//        data.add(pt(0, 5));
//        data.add(pt(1, 1));
//
//        Geometry actual = underTest.process(data);
//
//        // assertEquals(2, geometries.size());
//        // assertEquals(expected1.getGeometry(), geometries.get(0));
//        // assertEquals(expected2.getGeometry(), geometries.get(1));
//        // Geometry ex1 = expected1.getGeometry();
//        System.out.println("result: " + actual);
//        System.out.println("convexHull: " + actual.convexHull());
//        System.out.println("boundary: " + actual.getBoundary());
//        System.out.println("envelope: " + actual.getEnvelope());
//        System.out.println("norm: " + actual.norm());
//        System.out.println("concavehull" + ConcaveHull.concaveHullByLength(actual, DoubleUtils.SQRT2, true));
//        System.out.println("concavehull" + ConcaveHull.concaveHullByLength(actual, DoubleUtils.SQRT2, false));
//        LineString lineString = factory
//                .createLineString(new Coordinate[]{new Coordinate(-1, 2.5), new Coordinate(1, 2.5)});
//        System.out.println(lineString.intersects(actual));
//    }
//
//    // @Test
//    // public void multiPointTest() {
//    // RobutContext ctxt = new RobutContext(ScaleInfo.DEFAULT,
//    // ChassisInfoTest.DEFAULT);
//    // GeometryFactory factory = ctxt.geometryFactory;
//    //
//    //
//    //
//    //
//    // MultiPoint multiPoint = factory.createMultiPoint(new Coordinate[]{new
//    // Coordinate(0, 0), new Coordinate(0,2),
//    // new Coordinate(0, 3), new Coordinate(0,5)});
//    //
//    //
//    //
//    // System.out.println(multiPoint);
//    // System.out.println(multiPoint.convexHull());
//    //
//    // LineString lineString = factory.createLineString(new Coordinate[]{new
//    // Coordinate(-1, 2.5), new Coordinate(1,2.5)});
//    // System.out.println(lineString.crosses(multiPoint.convexHull()));
//    // System.out.println(lineString.crosses(multiPoint));
//    //
//    //
//    // }
//
//    // @Test
//    // public void multiPointTest2() {
//    // RobutContext ctxt = new RobutContext(ScaleInfo.DEFAULT,
//    // ChassisInfoTest.DEFAULT);
//    // GeometryFactory factory = ctxt.geometryFactory;
//    //
//    //
//    // MultiPoint multiPoint = factory.createMultiPoint(new Coordinate[]{new
//    // Coordinate(0, 0), new Coordinate(0,5),
//    // new Coordinate(1, 3)});
//    //
//    //
//    //
//    // System.out.println(multiPoint);
//    // System.out.println(multiPoint.convexHull());
//    //
//    // LineString lineString = factory.createLineString(new Coordinate[]{new
//    // Coordinate(-1, 2.5), new Coordinate(1,2.5)});
//    // System.out.println(lineString.crosses(multiPoint.convexHull()));
//    // System.out.println(lineString.crosses(multiPoint));
//    //
//    //
//    // }
//
//    private class TestingPath implements MapPathI {
//
//        Geometry geometry;
//
//        TestingPath(GeometryFactory factory, Coordinate... coordinates) {
//            LineString[] lines = new LineString[coordinates.length - 1];
//            Point lastPoint = factory.createPoint(coordinates[0]);
//            for (int i = 1; i < coordinates.length; i++) {
//                Point point = factory.createPoint(coordinates[i]);
//                lines[i - 1] = factory
//                        .createLineString(new Coordinate[]{lastPoint.getCoordinate(), point.getCoordinate()});
//                lastPoint = point;
//            }
//            geometry = lines.length == 1 ? lines[0] : factory.createMultiLineString(lines);
//        }
//
//        TestingPath(MultiLineString geometry) {
//            this.geometry = geometry;
//        }
//
//        public Geometry getGeometry() {
//            return geometry;
//        }
//    }
//}
