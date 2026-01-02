package org.xenei.robot.common.sensor.distance;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.locationtech.jts.geom.Coordinate;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.serialization.SerializationException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;


public class DistanceSensorTest {
    DistanceSensor.Serde serde;

    void assertEquivalent(Position p1, Position p2) {
        assertThat(p1.heading()).as("Heading").isEqualTo(p2.heading());
        assertThat(p1.getX()).as("X").isEqualTo(p2.getX());
        assertThat(p1.getY()).as("Y").isEqualTo(p2.getY());
    }

    void assertEquivalent(Location l1, Location l2) {
        assertThat(l1.getX()).as("X").isEqualTo(l2.getX());
        assertThat(l1.getY()).as("Y").isEqualTo(l2.getY());
    }

    @ParameterizedTest
    @MethodSource("serdeData")
    void testSerde(DistanceSensor.Readings readings) throws SerializationException {
        serde = new DistanceSensor.Serde();
        byte[] result = serde.serialize(readings);
        DistanceSensor.Readings readings2 = serde.deserialize(result);
        assertEquivalent(readings2.origin(), readings.origin());
        assertThat(readings2.readings().size()).as("Reading size").isEqualTo(readings.readings().size());
        Iterator<? extends Location> one = readings.readings().iterator();
        Iterator<? extends Location> two = readings2.readings().iterator();
        for (int i = 0; i < readings.readings().size(); i++) {
            assertEquivalent(two.next(), one.next());
        }
    }

    static Stream<DistanceSensor.Readings> serdeData() {
        List<DistanceSensor.Readings> readings = new ArrayList<DistanceSensor.Readings>();
        readings.add(new DistanceSensor.Readings(Position.ORIGIN, Collections.singletonList(Location.ORIGIN)));
        readings.add(new DistanceSensor.Readings(Position.ORIGIN, Arrays.asList(
                Location.ORIGIN,
                Location.asLocation(new Coordinate(1, 1)),
                Location.asLocation(new Coordinate(Integer.MAX_VALUE, Integer.MIN_VALUE))
        )));
        readings.add(new DistanceSensor.Readings(
                Position.asPosition(new Coordinate(3, 3), Math.PI),
                Arrays.asList(
                        Location.ORIGIN,
                        Location.asLocation(new Coordinate(1, 1)),
                        Location.asLocation(new Coordinate(Integer.MAX_VALUE, Integer.MIN_VALUE))
                )));
        return readings.stream();
    }
}
