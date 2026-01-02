package org.xenei.robot.common.testUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import io.nats.client.Nats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.mapping.Map;
import org.xenei.robot.common.mapping.ThetaAndRange;
import org.xenei.robot.common.planning.Solution;
import org.xenei.robot.common.utils.RobutContext;
import org.xenei.robot.mapper.visualization.TextViz;

public class FakeDistanceSensor1 implements FakeDistanceSensor {
    private static final Logger LOG = LoggerFactory.getLogger(FakeDistanceSensor1.class);
    private static final int BLOCKSIZE = 17;
    private static final double RADIANS = Math.toRadians(360.0 / BLOCKSIZE);
    private final Map map;
    private static final double MAX_RANGE = 15;
    private final Supplier<Position> positionSupplier;
    /**The topic that the distance sensor writes to */
    private final LinkedHashMap<Position, ThetaAndRange[]> history = new LinkedHashMap<>();
   // private final TextViz textViz;

    public FakeDistanceSensor1(Map map, Supplier<Position> positionSupplier) {
        this.map = map;
        this.positionSupplier = positionSupplier;
       // RobutContext ctxt = map.getContext();
      //  textViz = new TextViz(1.0, ctxt.getConnectionOptions(), ctxt.vizName, ctxt.scaleInfo, System.out);
    }

    @Override
    public Map map() {
        return map;
    }

//    public void writeHistory(OutputStream out) {
//        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out))) {
//            for (java.util.Map.Entry<Position, ThetaAndRange[]> entry : history.entrySet()) {
//                Position pos = entry.getKey();
//                StringBuilder sb = new StringBuilder(
//                        String.format("%s,%s,%s", pos.getCoordinate().getX(), pos.getCoordinate().getY(), pos.heading()));
//                for (ThetaAndRange l : entry.getValue()) {
//                    sb.append(String.format(",%s,%s", l.theta(), l.range()));
//                }
//                writer.write(sb.append('\n').toString());
//            }
//        } catch (IOException e) {
//            LOG.error("Unable to write history", e);
//        }
//    }

//    public void readHistory(InputStream in) {
//        for (String s : IOUtils.readLines(in, Charset.defaultCharset())) {
//            String[] numbers = s.split(",");
//            int i = 0;
//            double x = Double.parseDouble(numbers[i++]);
//            double y = Double.parseDouble(numbers[i++]);
//            double heading = Double.parseDouble(numbers[i++]);
//            Position position = Position.asPosition(new Coordinate(x, y), heading);
//            int limit = (numbers.length - 3) / 2;
//            ThetaAndRange[] locations = new ThetaAndRange[limit];
//            double theta;
//            double range;
//            for (int j = 0; j < limit; j++) {
//                theta = Double.parseDouble(numbers[i++]);
//                range = Double.parseDouble(numbers[i++]);
//                locations[j] = new ThetaAndRange(theta, range);
//            }
//            history.put(position, locations);
//        }
//    }

//    public Location[] replay(int idx) {
//        return history.values().toArray(new Location[0][0])[idx];
//    }

    @Override
    public void run() {
        Position position = positionSupplier.get();
        ThetaAndRange[] result = null; //history.get(position);
        if (result == null) {
            result = new ThetaAndRange[BLOCKSIZE];
            for (int i = 0; i < BLOCKSIZE; i++) {
                result[i] = look(position, position.heading() + (RADIANS * i));
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Reading {}: {}", i, result[i]);
                }
            }
//            history.put(position, result);
//            try {
//                writeHistory(new FileOutputStream("/tmp/sensorData.txt"));
//            } catch (IOException e) {
//                LOG.error("Can not write sensor data");
//            }
        }
        List<ThetaAndRange> resultList = Arrays.stream(result).filter(thetaAndRange -> {return thetaAndRange.range() < maxRange();})
                        .collect(Collectors.toList());
        map.getContext().distanceSensorTopic.send(new Readings(position, resultList));
    }

    @Override
    public double maxRange() {
        return MAX_RANGE;
    }
}
