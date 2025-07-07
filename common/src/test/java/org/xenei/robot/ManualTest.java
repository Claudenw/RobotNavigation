//package org.xenei.robot;
//
//import org.junit.jupiter.api.Test;
//import org.xenei.robot.common.AbortedException;
//import org.xenei.robot.common.DistanceSensor;
//import org.xenei.robot.common.Location;
//import org.xenei.robot.common.Mover;
//import org.xenei.robot.common.Position;
//import org.xenei.robot.common.ScaleInfo;
//import org.xenei.robot.common.mapping.Map;
//import org.xenei.robot.common.planning.Solution;
//import org.xenei.robot.common.testUtils.DebugViz;
//import org.xenei.robot.common.testUtils.FakeDistanceSensor1;
//import org.xenei.robot.common.testUtils.FakeMover;
//import org.xenei.robot.common.testUtils.MapLibrary;
//import org.xenei.robot.common.testUtils.TestChassisInfo;
//import org.xenei.robot.common.testUtils.TestingPositionSupplier;
//import org.xenei.robot.common.utils.RobutContext;
//import org.xenei.robot.mapper.MapDistanceSensorAdapter;
//import org.xenei.robot.mapper.MapImpl;
//import org.xenei.robot.mapper.visualization.MapViz;
//
//import java.util.concurrent.TimeUnit;
//import java.util.function.Supplier;
//
//public class ManualTest {
//
//   public static void main(String[] args) throws Exception {
//        Supplier<Position> positionSupplier = new TestingPositionSupplier(Position.from(-1, -3) );
//        Solution solution = new Solution();
//        solution.add(positionSupplier.get());
//        Map map = new MapImpl(new RobutContext(ScaleInfo.DEFAULT, TestChassisInfo.DEFAULT));
//        underTest = new FakeDistanceSensor1(MapLibrary.map2(map), positionSupplier);
//        MapViz mapViz = new MapViz(1, underTest.map(), () -> solution, positionSupplier, () -> null);
//        map.getContext().scheduleAtFixedRate(mapViz::redraw, 0,500, TimeUnit.MILLISECONDS);
//        MapDistanceSensorAdapter adapter = new MapDistanceSensorAdapter(map, positionSupplier);
//        underTest.addListener(adapter);
//        underTest.run();
//
//        DebugViz debugViz = new DebugViz(1, map, () -> solution, positionSupplier, () -> null);
//        debugViz.redraw();
//        Thread.sleep(1000);
////        Set<Obstacle> obstacles = underTest.map().getObstacles().join();
////        underTest.run();
////        for (Location l : actual) {
////            assertCoordinateInObstacles(obstacles, position.nextPosition(l));
////        }
//
//       public static void main(String[] args) throws AbortedException, InterruptedException {
//           RobutContext ctxt = new RobutContext(ScaleInfo.DEFAULT, TestChassisInfo.DEFAULT);
//           Location startCoord = Location.from(-1, -3);
//           Mover mover = new FakeMover(Location.from(startCoord), 1);
//           Map m = new MapImpl(new RobutContext(ScaleInfo.DEFAULT, TestChassisInfo.DEFAULT));
//           DistanceSensor sensor = new FakeDistanceSensor1(MapLibrary.map2(m), mover::position);
//           Location target = Location.from(-1, 1);
//           Supplier<Position> positionSupplier = mover::position;
//           MapImpl map = new MapImpl(ctxt);
//           MapLibrary.map2(map);
//           Processor underTest = new Processor(ctxt, mover, positionSupplier, map);
//           MapViz mapViz = new MapViz(100, underTest.map, underTest.planner::getSolution, positionSupplier, () -> null);
//           underTest.add(mapViz);
//           mapViz.redraw();
//
//           while (true) {
//               Thread.sleep(1000);
//           }
//       }
//    }
//}
