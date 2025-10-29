package org.xenei.robot.common.testUtils;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.function.Supplier;

import org.apache.commons.io.IOUtils;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.common.DistanceSensor;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.mapping.Map;
import org.xenei.robot.common.mapping.ThetaAndRange;
import org.xenei.robot.common.messages.Topic;

public class FakeDistanceSensor1 implements FakeDistanceSensor {
    private static final Logger LOG = LoggerFactory.getLogger(FakeDistanceSensor1.class);
    private static final int BLOCKSIZE = 17;
    private static final double RADIANS = Math.toRadians(360.0 / BLOCKSIZE);
    private final Map map;
    private static final double MAX_RANGE = 5;
    private final Supplier<Position> positionSupplier;
    private final Topic<DistanceSensor.Readings> distanceTopic;
    private final LinkedHashMap<Position, ThetaAndRange[]> history = new LinkedHashMap<>();

    public FakeDistanceSensor1(Map map, Supplier<Position> positionSupplier) {
        this.map = map;
        this.positionSupplier = positionSupplier;
        this.distanceTopic = map.getContext().bus.distance;
    }

    @Override
    public Map map() {
        return map;
    }

    public void writeHistory(OutputStream out) {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out))) {
            for (java.util.Map.Entry<Position, ThetaAndRange[]> entry : history.entrySet()) {
                Position pos = entry.getKey();
                StringBuilder sb = new StringBuilder(
                        String.format("%s,%s,%s", pos.getCoordinate().getX(), pos.getCoordinate().getY(), pos.getHeading()));
                for (ThetaAndRange l : entry.getValue()) {
                    sb.append(String.format(",%s,%s", l.theta(), l.range()));
                }
                writer.write(sb.append('\n').toString());
            }
        } catch (IOException e) {
            LOG.error("Unable to write history", e);
        }
    }

    public void readHistory(InputStream in) {
        for (String s : IOUtils.readLines(in, Charset.defaultCharset())) {
            String[] numbers = s.split(",");
            int i = 0;
            double x = Double.parseDouble(numbers[i++]);
            double y = Double.parseDouble(numbers[i++]);
            double heading = Double.parseDouble(numbers[i++]);
            Position position = Position.asPosition(new Coordinate(x, y), heading);
            int limit = (numbers.length - 3) / 2;
            ThetaAndRange[] locations = new ThetaAndRange[limit];
            double theta;
            double range;
            for (int j = 0; j < limit; j++) {
                theta = Double.parseDouble(numbers[i++]);
                range = Double.parseDouble(numbers[i++]);
                locations[j] = new ThetaAndRange(theta, range);
            }
            history.put(position, locations);
        }
    }

    public Location[] replay(int idx) {
        return history.values().toArray(new Location[0][0])[idx];
    }

    @Override
    public void run() {
        Position position = positionSupplier.get();
        ThetaAndRange[] result = history.get(position);
        if (result == null) {
            result = new ThetaAndRange[BLOCKSIZE];
            for (int i = 0; i < BLOCKSIZE; i++) {
                result[i] = look(position, position.getHeading() + (RADIANS * i));
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Reading {}: {}", i, result[i]);
                }
            }
            history.put(position, result);
            try {
                writeHistory(new FileOutputStream("/tmp/sensorData.txt"));
            } catch (IOException e) {
                LOG.error("Can not write sensor data");
            }
        }
        distanceTopic.send(new Readings(position, Arrays.asList(result)));
    }

    @Override
    public double maxRange() {
        return MAX_RANGE;
    }
}
