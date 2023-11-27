package org.xenei.robot.common.testUtils;

import static org.junit.Assert.assertArrayEquals;

import org.junit.jupiter.api.Test;
import org.xenei.robot.common.Coordinates;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.CoordinateMap;

public class FakeSensorTest {

    @Test
    public void map1Test() {
        CoordinateMap map = MapLibrary.map1('#');
        FakeSensor underTest = new FakeSensor(map);
        int x = 13;
        int y = 15;
        int h = 0;

        Coordinates[] expected = { Coordinates.fromXY(1.000000, 0.000000), Coordinates.fromXY(0.932472, 0.361242),
                Coordinates.fromXY(0.739009, 0.673696), Coordinates.fromXY(0.445738, 0.895163),
                Coordinates.fromXY(0.092268, 0.995734), Coordinates.fromXY(-0.273663, 0.961826),
                Coordinates.fromXY(-0.602635, 0.798017), Coordinates.fromXY(-0.850217, 0.526432),
                Coordinates.fromXY(-2.948919, 0.551249), Coordinates.fromXY(-2.948919, -0.551249),
                Coordinates.fromXY(-0.850217, -0.526432), Coordinates.fromXY(-0.602635, -0.798017),
                Coordinates.fromXY(-1.368315, -4.809128), Coordinates.fromXY(0.553610, -5.974405),
                Coordinates.fromXY(0.891477, -1.790327), Coordinates.fromXY(0.739009, -0.673696),
                Coordinates.fromXY(0.932472, -0.361242) };

        Position postition = new Position(Coordinates.fromXY(x, y), Math.toRadians(h));
        CoordinateUtils.assertEquivalent(expected, underTest.sense(postition), 0.000001);

        expected = new Coordinates[] { Coordinates.fromXY(-14.000000, 0.000000),
                Coordinates.fromXY(-1.864944, -0.722483), Coordinates.fromXY(-0.739009, -0.673696),
                Coordinates.fromXY(-2.674430, -5.370980), Coordinates.fromXY(-0.738147, -7.965873),
                Coordinates.fromXY(0.547326, -1.923651), Coordinates.fromXY(0.602635, -0.798017),
                Coordinates.fromXY(0.850217, -0.526432), Coordinates.fromXY(0.982973, -0.183750),
                Coordinates.fromXY(0.982973, 0.183750), Coordinates.fromXY(0.850217, 0.526432),
                Coordinates.fromXY(0.602635, 0.798017), Coordinates.fromXY(0.273663, 0.961826),
                Coordinates.fromXY(-0.092268, 0.995734), Coordinates.fromXY(-0.445738, 0.895163),
                Coordinates.fromXY(-0.739009, 0.673696), Coordinates.fromXY(-1.864944, 0.722483) };
        postition.setHeading(Math.PI);
        CoordinateUtils.assertEquivalent(expected, underTest.sense(postition), 0.000001);
    }
    
    
}
