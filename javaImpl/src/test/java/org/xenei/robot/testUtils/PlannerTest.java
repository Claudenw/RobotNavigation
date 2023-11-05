package org.xenei.robot.testUtils;

import org.xenei.robot.Processor;
import org.xenei.robot.navigation.Coordinates;
import org.xenei.robot.navigation.Position;
import org.xenei.robot.utils.ActiveMap;
import org.xenei.robot.utils.ActiveMap.Coord;
import org.xenei.robot.utils.Mover;

public class PlannerTest {
    static Processor processor;

    public static void main(String[] args) {
        int x = 13;
        int y = 15;

        Mover mover = new FakeMover(Coordinates.fromXY(x, y), 1);
        FakeSensor sensor = new FakeSensor(MapLibrary.map1());
        processor = new Processor(sensor, mover);
        Coordinates target = Coordinates.fromXY(0, 0);
        processor.gotoTarget(target);
        Position p = mover.position();
        while (!p.equals(target)) {
            p = processor.step();
            displayMap(sensor.map(), p);
        }

    }

    private static void displayMap(ActiveMap map, Position position) {

        String mapStr = map.toString();
        Coord c = map.new Coord(position);
        if (c.inRange()) {
            int offset = ActiveMap.dim - (c.x+ActiveMap.halfdim) - 1 ;
            offset *= (ActiveMap.dim + 1);
            offset += (c.y+ActiveMap.halfdim);
            mapStr = mapStr.substring(0, offset) + "+" + mapStr.substring(offset + 1);
        }
        System.out.println(mapStr);
        System.out.println(position);
    }

}
