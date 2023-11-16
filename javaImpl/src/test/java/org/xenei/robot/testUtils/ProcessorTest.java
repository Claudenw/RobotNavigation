package org.xenei.robot.testUtils;

import java.io.ByteArrayOutputStream;
import java.util.Optional;

import org.apache.jena.riot.Lang;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.Processor;
import org.xenei.robot.navigation.Coordinates;
import org.xenei.robot.navigation.Position;
import org.xenei.robot.planner.PlanRecord;
import org.xenei.robot.utils.CoordinateMap;
import org.xenei.robot.utils.CoordinateMapBuilder;
import org.xenei.robot.utils.Mover;

public class ProcessorTest {
    static Processor processor;
    private static final Logger LOG = LoggerFactory.getLogger(ProcessorTest.class);

    public static void main(String[] args) {

        Coordinates finalCoord = Coordinates.fromXY(-1, 1);
        Coordinates startCoord = Coordinates.fromXY(-1, -3);
        
        Mover mover = new FakeMover(startCoord, 1);
        FakeSensor sensor = new FakeSensor(MapLibrary.map2('#'));
        processor = new Processor(sensor, mover);
        
        processor.setTarget(finalCoord);
        Optional<Position> p = Optional.of(mover.position());
        //for (int i = 0; i < 5; i++) {
             while (p.isPresent()) {
            displayMap(sensor.map(), p.get());
            p = processor.step();
        }
        LOG.info("PROCESSOR DUMP");
        LOG.info("Map");
        processor.getPlanner().getPlanRecords().forEach(c -> LOG.info(c.toString()));
        LOG.info("Path");
        processor.getPlanner().getSolution().apply(c -> LOG.info(c.toString()));
        LOG.info("Sensed");
        processor.getPlanner().getMap().getObstacles().forEach(c -> LOG.info(c.toString()));
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        processor.getPlanner().getMap().getModel().write(bos, Lang.TURTLE.getName());
        LOG.debug( "\n"+bos.toString());
        StringBuilder sb = new StringBuilder();
        processor.getPlanner().getMap().costModel().forEach( l -> sb.append( l.toString()).append("\n"));
        LOG.debug( "\n{}", sb.toString());
        LOG.debug( "\n{}", processor.getPlanner().getMap().dumpBaseModel());
        
    }

    private static void displayMap(CoordinateMap initialMap, Position p) {
        CoordinateMap map = new CoordinateMapBuilder(initialMap.scale()).merge(initialMap).build();
        map.enable(processor.getPlanner().getMap().getObstacles(), '@');
        processor.getPlanner().getPlanRecords().stream().map(PlanRecord::coordinates)
        .forEach( c -> map.enable(c, '*'));
        processor.getPlanner().getSolution().apply( r -> map.enable( r.coordinates(), '=' ));
        map.enable(p.coordinates(), '+');
        LOG.info("\n{}", map.toString());
        LOG.info(p.toString());
    }

}
