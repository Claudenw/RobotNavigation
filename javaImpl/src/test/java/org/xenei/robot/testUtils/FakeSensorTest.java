package org.xenei.robot.testUtils;

import static org.junit.Assert.assertArrayEquals;

import org.junit.jupiter.api.Test;
import org.xenei.robot.navigation.Coordinates;
import org.xenei.robot.navigation.Position;

public class FakeSensorTest {

    @Test
    public void map1Test() {
        int x = 13;
        int y = 15;
        int h = 0;

        // @formatter:off
        Coordinates[] expected = {
                Coordinates.fromXY(1.0, 0.0),
                Coordinates.fromXY(0.9324722294043558, 0.3612416661871529),
                Coordinates.fromXY(0.7390089172206591, 0.6736956436465572),
                Coordinates.fromXY(0.4457383557765383, 0.8951632913550623),
                Coordinates.fromXY(0.09226835946330202, 0.9957341762950345),
                Coordinates.fromXY(-0.2736629900720829, 0.961825643172819),
                Coordinates.fromXY(-0.6026346363792563, 0.7980172272802396),
                Coordinates.fromXY(-0.8502171357296142, 0.5264321628773557),
                Coordinates.fromXY(-0.9829730996839018, 0.18374951781657037),
                Coordinates.fromXY(-5.8978385981034105, -1.1024971068994207),
                Coordinates.fromXY(-1.7004342714592282, -1.0528643257547117),
                Coordinates.fromXY(-1.205269272758513, -1.596034454560479),
                Coordinates.fromXY(-1.6419779404324988, -5.770953859036914),
                Coordinates.fromXY(0.09226835946330154, -0.9957341762950346),
                Coordinates.fromXY(0.44573835577653853, -0.8951632913550622),
                Coordinates.fromXY(0.7390089172206592, -0.6736956436465571),
                Coordinates.fromXY(0.9324722294043558, -0.36124166618715303),
        };
        // @formatter:on
        Position postition = new Position(Coordinates.fromXY(x, y), Math.toRadians(h));

        Coordinates[] actual = new FakeSensor(MapLibrary.map1()).sense(postition);

        for (Coordinates c : actual)
            System.out.println(c);

        assertArrayEquals(expected, actual);
    }
}
