//package org.xenei.robot;
//
//import static org.junit.jupiter.api.Assertions.assertArrayEquals;
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertTrue;
//
//import java.util.List;
//import java.util.stream.Collectors;
//
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Disabled;
//import org.junit.jupiter.api.Test;
//import org.locationtech.jts.geom.Coordinate;
//import org.xenei.robot.common.Location;
//import org.xenei.robot.common.Mover;
//import org.xenei.robot.common.Position;
//import org.xenei.robot.common.SolutionTest;
//import org.xenei.robot.common.testUtils.FakeMover;
//import org.xenei.robot.common.testUtils.FakeDistanceSensor;
//import org.xenei.robot.common.testUtils.FakeDistanceSensor1;
//import org.xenei.robot.common.testUtils.MapLibrary;
//
//
//public class ProcessorTest {
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
//
//}
