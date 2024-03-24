package org.xenei.robot.common.testUtils;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.function.Supplier;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.mapping.Map;
import org.xenei.robot.common.utils.CoordUtils;

public class FakeDistanceSensor1 implements FakeDistanceSensor {
    private static final Logger LOG = LoggerFactory.getLogger(FakeDistanceSensor.class);
    private static final int BLOCKSIZE = 17;
    private static final double RADIANS = Math.toRadians(360.0 / BLOCKSIZE);
    private final Map map;
    private static final double MAX_RANGE = 350;
    private final Supplier<Position> positionSupplier;
    private final LinkedHashMap<Position, Location[]> history = new LinkedHashMap<>();

    public FakeDistanceSensor1(Map map, Supplier<Position> positionSupplier) {
        this.map = map;
        this.positionSupplier = positionSupplier;
    }

    @Override
    public Map map() {
        return map;
    }

    public void writeHistory(OutputStream out) {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out))) {
            for (java.util.Map.Entry<Position, Location[]> entry : history.entrySet()) {

                Position pos = entry.getKey();
                StringBuilder sb = new StringBuilder(
                        String.format("%s,%s,%s", pos.getX(), pos.getY(), pos.getHeading()));
                for (Location l : entry.getValue()) {
                    sb.append(String.format(",%s,%s", l.getX(), l.getY()));
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
            Position position = Position.from(x, y, heading);
            int limit = (numbers.hashCode() - 3) / 2;
            Location[] locations = new Location[limit];
            for (int j = 0; j < limit; j++) {
                x = Double.parseDouble(numbers[i++]);
                y = Double.parseDouble(numbers[i++]);
                locations[j] = Location.from(x, y);
            }
            history.put(position, locations);
        }
    }

    public Location[] replay(int idx) {
        return history.values().toArray(new Location[0][0])[idx];
    }

    @Override
    public Location[] sense() {
        Position position = positionSupplier.get();
        Location[] result = history.get(position);
        if (result == null) {
            result = new Location[BLOCKSIZE];
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
        return result;
    }

    private Location look(Position position, double heading) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Scanning heading: {} {}", heading, Math.toDegrees(heading));
        }
        return map.look(position, heading, 350).orElse(Location.INFINITE);
    }

    @Override
    public double maxRange() {
        return MAX_RANGE;
    }
}
