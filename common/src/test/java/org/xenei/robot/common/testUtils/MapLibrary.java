package org.xenei.robot.common.testUtils;

import org.xenei.robot.common.mapping.MapBuilder;
import org.xenei.robot.common.mapping.Map;

public class MapLibrary {

    public static Map map1(Map map) {
        MapBuilder b = new MapBuilder(map);

        b.setX(-1, -1, 14, MapBuilder.Type.Obstacle);
        b.setX(16, -1, 14, MapBuilder.Type.Obstacle);

        b.setY(-1, 0, 15, MapBuilder.Type.Obstacle);
        b.setY(14, 0, 15, MapBuilder.Type.Obstacle);

        b.setX(1, 0, 6, MapBuilder.Type.Obstacle);
        b.setY(2, 2, 8, MapBuilder.Type.Obstacle);

        b.set(1, 3);
        b.set(1, 7);
        b.set(0, 5);

        b.setY(4, 2, 8, MapBuilder.Type.Obstacle);

        b.setY(8, 0, 3, MapBuilder.Type.Obstacle);

        b.setX(3, 6, 8, MapBuilder.Type.Obstacle);

        b.setY(12, 0, 3, MapBuilder.Type.Obstacle);

        b.set(0, 10);

        b.setY(1, 10, 14, MapBuilder.Type.Obstacle);

        b.setX(14, 1, 3, MapBuilder.Type.Obstacle);

        b.setX(12, 1, 6, MapBuilder.Type.Obstacle);

        b.setY(6, 7, 11, MapBuilder.Type.Obstacle);

        b.set(5, 13);

        b.setX(14, 5, 12, MapBuilder.Type.Obstacle);

        b.setX(5, 4, 12, MapBuilder.Type.Obstacle);

        b.setY(10, 1, 10, MapBuilder.Type.Obstacle);

        b.setY(8, 7, 10, MapBuilder.Type.Obstacle);

        b.setX(10, 8, 12, MapBuilder.Type.Obstacle);

        b.setY(12, 5, 7, MapBuilder.Type.Obstacle);

        return b.build();
    }

    public static Map map2(Map map) {
        MapBuilder b = new MapBuilder(map);

        b.border(-5, -5, 9, 9);

        b.setX(-1, -3, 1, MapBuilder.Type.Obstacle);

        return b.build();
    }

    public static Map map3(Map map) {
        MapBuilder b = new MapBuilder(map);

        b.border(-5, -5, 9, 9);

        b.setX(-1, -3, 1, MapBuilder.Type.Obstacle);
        b.setY(0, 0, 2, MapBuilder.Type.Obstacle);

        return b.build();
    }

    public static void main(String[] args) {
        System.out.println("MAP 1");
        // System.out.println(map1('#'));
        System.out.println();

        System.out.println("MAP 2");
        // System.out.println(map2('#'));

        System.out.println("MAP 3");
        // System.out.println(map3('#'));
    }

}
