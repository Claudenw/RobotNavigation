package org.xenei.robot.mapper;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.locationtech.jts.geom.Coordinate;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.ScaleInfo;
import org.xenei.robot.common.utils.AngleUtils;
import org.xenei.robot.mapper.MapperImpl.ObstacleMapper;

public class ObstacleMapperTest {

    private double buffer = .5;

    @ParameterizedTest(name = "{index} {0} {1}")
    @MethodSource("doMapParameters")
    public void doMapTest(Position currentPosition, Location relativeLocation, Coordinate expected,
            Coordinate expCoord) {
        MapImpl map = new MapImpl(ScaleInfo.DEFAULT);
        MapperImpl mapper = new MapperImpl(map);

        ObstacleMapper underTest = mapper.new ObstacleMapper(currentPosition, buffer);
        underTest.doMap(relativeLocation);
        assertTrue(underTest.newObstacles.contains(expected));
        assertTrue(underTest.coordSet.contains(expCoord));
    }

    public static Stream<Arguments> doMapParameters() {
        List<Arguments> args = new ArrayList<>();

        args.add(Arguments.of(Position.from(-1, -3, AngleUtils.RADIANS_90), Location.from(.5, 3),
                new Coordinate(-4.5, -2.5), new Coordinate(-3, -2.5)));

        return args.stream();
    }

}
