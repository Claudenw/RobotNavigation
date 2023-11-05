package org.xenei.robot.testUtils;

import org.xenei.robot.utils.ActiveMap;
import org.xenei.robot.utils.ActiveMapBuilder;

public class MapLibrary {

    public static ActiveMap map1() {
        ActiveMapBuilder b = new ActiveMapBuilder(1);

        b.setX(-1, -1, 14);
        b.setX(16, -1, 14);

        b.setY(-1, 0, 15);
        b.setY(14, 0, 15);

        b.setX(1, 0, 6);
        b.setY(2, 2, 8);

        b.set(1, 3);
        b.set(1, 7);
        b.set(0, 5);

        b.setY(4, 2, 8);

        b.setY(8, 0, 3);

        b.setX(3, 6, 8);

        b.setY(12, 0, 3);

        b.set(0, 10);
        b.setY(1, 10, 14);

        b.setX(14, 1, 3);

        b.setX(12, 1, 6);

        b.setY(6, 7, 11);

        b.set(5, 13);

        b.setX(14, 5, 12);

        b.setX(5, 4, 12);

        b.setY(10, 1, 10);

        b.setY(8, 7, 10);

        b.setX(10, 8, 12);

        b.setY(12, 5, 7);

        return b.getMap();
    }

    public static void main(String[] args) {
        System.out.println(map1());
    }

}
