package org.xenei.robot.common;

import io.nats.client.Options;
import org.xenei.robot.common.utils.RobutContext;

public class TestingConfiguration {
    public static Options.Builder getOptionsBuilder() {
        return Options.builder()
                .userInfo("demo", "demo"); // Set a user and plain text password
    }

    public static RobutContext.Builder getContextBuilder(String id) {
        return RobutContext.builder()
                .id(id)
                .options(getOptionsBuilder())
                .chassisInfo(ChassisInfoTest.DEFAULT)
                .scaleInfo(ScaleInfo.DEFAULT);
    }
}
