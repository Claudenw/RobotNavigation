package org.xenei.robot.testUtils;

import org.xenei.robot.Processor;
import org.xenei.robot.navigation.Coordinates;
import org.xenei.robot.navigation.Position;
import org.xenei.robot.utils.CoordinateMap;
import org.xenei.robot.utils.CoordinateMapBuilder;
import org.xenei.robot.utils.Mover;

public class ProcessorTest {
    static Processor processor;

    public static void main(String[] args) {
        int x = 13;
        int y = 15;

        Mover mover = new FakeMover(Coordinates.fromXY(x, y), 1);
        FakeSensor sensor = new FakeSensor(MapLibrary.map1('#'));
        processor = new Processor(sensor, mover);
        Coordinates target = Coordinates.fromXY(0, 0);
        processor.gotoTarget(target);
        Position p = mover.position();
        displayMap(sensor.map(), p);
        while (!p.equals(target)) {
            p = processor.step();
            displayMap(sensor.map(), p);
        }

    }

    private static void displayMap(CoordinateMap initialMap, Position p) {
        CoordinateMap map = new CoordinateMapBuilder(initialMap.scale()).merge(initialMap).build();
        map.enable(processor.getPlanner().getSensed(), '@');
        map.enable(processor.getPlanner().getPlanRecords(), '*');
        map.enable(processor.getPlanner().getPath(), '=');
        map.enable(p, '+');
        System.out.println(map);
        System.out.println(p);
    }

}
