package org.xenei.robot.common;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.xenei.robot.common.mapping.Map;
import org.xenei.robot.common.mapping.MapPosition;
import org.xenei.robot.common.mapping.MapTest;
import org.xenei.robot.common.testUtils.FakeStepMonitor;
import org.xenei.robot.common.utils.RobutContext;

public class DeadReckoningTest {
    private RobutContext ctxt;
    private ScaleInfo scaleInfo;
    private Map map;
    private MapTest.TestingStorage testingStorage;

    @BeforeEach
    void beforeDeadReckoningTest() {
        RobutContext.Builder builder = RobutContext.builder();
        builder.setOptions(builder.defaultOptions())
                .setChassisInfo(ChassisInfoTest.ONE_METER);
        ctxt = builder.build();
        scaleInfo = ctxt.scaleInfo;
        testingStorage = new MapTest.TestingStorage();
        map = new Map(ctxt, testingStorage);
    }

    @AfterEach
    void afterDeadReckoningTest() {
        ctxt.close();
    }

    @Test
    void constructorTest() {
        DeadReckoning deadReckoning = DeadReckoning.from(ctxt, Position.ORIGIN);
        ScaleInfoTest.assertEquals(scaleInfo, Location.ORIGIN, deadReckoning.get());
        Assertions.assertEquals(0.0, deadReckoning.heading(), scaleInfo.getResolution());
        Assertions.assertEquals(2, deadReckoning.headingAccuracy());

        MapPosition position = map.asMapPosition(new Coordinate(5.5, 3.1), 7.8);
        deadReckoning = DeadReckoning.from(ctxt, position);
        ScaleInfoTest.assertEquals(scaleInfo, position, deadReckoning.get());
        Assertions.assertEquals(7.8, deadReckoning.heading(), scaleInfo.getResolution());
        Assertions.assertEquals(2, deadReckoning.headingAccuracy());
    }

    @Test
    void instantaneousHeadingTest() {
        Position position = Position.ORIGIN;
        FakeStepMonitor fakeStepMonitor = new FakeStepMonitor(ctxt, 5, 5);
        DeadReckoning deadReckoning = DeadReckoning.from(ctxt, position);
        deadReckoning.track(fakeStepMonitor);
        fakeStepMonitor.takeStep();
        ScaleInfoTest.assertEquals(scaleInfo, position, deadReckoning.get());
        fakeStepMonitor.takeStep();
        ScaleInfoTest.assertEquals(scaleInfo, Position.asPosition(new Coordinate(1, 0), 0), deadReckoning.get());
    }
}
