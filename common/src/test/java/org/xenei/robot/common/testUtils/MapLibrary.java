package org.xenei.robot.common.testUtils;

import org.xenei.robot.common.CoordinateMap;
import org.xenei.robot.common.CoordinateMapBuilder;

public class MapLibrary {

    public static CoordinateMap map1(char c) {
        CoordinateMapBuilder b = new CoordinateMapBuilder(1);

        b.setX(-1, -1, 14, c);
        b.setX(16, -1, 14, c);

        b.setY(-1, 0, 15, c);
        b.setY(14, 0, 15, c);

        b.setX(1, 0, 6, c);
        b.setY(2, 2, 8, c);

        b.set(1, 3, c);
        b.set(1, 7, c);
        b.set(0, 5, c);

        b.setY(4, 2, 8, c);

        b.setY(8, 0, 3, c);

        b.setX(3, 6, 8, c);

        b.setY(12, 0, 3, c);

        b.set(0, 10, c);
        b.setY(1, 10, 14, c);

        b.setX(14, 1, 3, c);

        b.setX(12, 1, 6, c);

        b.setY(6, 7, 11, c);

        b.set(5, 13, c);

        b.setX(14, 5, 12, c);

        b.setX(5, 4, 12, c);

        b.setY(10, 1, 10, c);

        b.setY(8, 7, 10, c);

        b.setX(10, 8, 12, c);

        b.setY(12, 5, 7, c);

        return b.build();
    }

    public static CoordinateMap map2(char c) {
        CoordinateMapBuilder b = new CoordinateMapBuilder(1);

        b.border( -5, -5, 9, 9, c );
        
        b.setX(-1, -3, 1, c);

        return b.build();
    }

    public static CoordinateMap map3(char c) {
        CoordinateMapBuilder b = new CoordinateMapBuilder(1);

        b.border( -5, -5, 9, 9, c );
        
        b.setX(-1, -3, 1, c);
        b.setY(0, 0, 2, c);

        return b.build();
    }
    
    public static void main(String[] args) {
        System.out.println( "MAP 1");
        System.out.println(map1('#'));
        System.out.println();
        
        System.out.println( "MAP 2");
        System.out.println(map2('#'));
        
        System.out.println( "MAP 3");
        System.out.println(map3('#'));
    }

}
