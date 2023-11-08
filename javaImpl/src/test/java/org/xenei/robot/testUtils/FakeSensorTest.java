package org.xenei.robot.testUtils;

import static org.junit.Assert.assertArrayEquals;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.xenei.robot.navigation.Coordinates;
import org.xenei.robot.navigation.Position;
import org.xenei.robot.utils.CoordinateMap;

public class FakeSensorTest {

    @Test
    public void map1Test() {
        int x = 13;
        int y = 15;
        int h = 0;

        // @formatter:off
        Coordinates[] expected = {
                Coordinates.fromXY(12, 14),
                Coordinates.fromXY(14, 16),
                Coordinates.fromXY(10, 16),
                Coordinates.fromXY(12, 16),
                Coordinates.fromXY(14, 15),
                Coordinates.fromXY(13, 16),
                Coordinates.fromXY(14, 14),
                Coordinates.fromXY(14, 9),
                Coordinates.fromXY(10, 14),
                Coordinates.fromXY(12, 10),
                Coordinates.fromXY(14, 13),
        };
        // @formatter:on
        Position postition = new Position(Coordinates.fromXY(x, y), Math.toRadians(h));
        CoordinateMap map = MapLibrary.map1('#');
        Set<Coordinates> actual = Arrays.stream(new FakeSensor(map).sense(postition)).collect(Collectors.toSet());

        for (Coordinates c : actual)
            System.out.println(c);

        assertArrayEquals(expected, actual.toArray());
    }
}
