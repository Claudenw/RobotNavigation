package org.xenei.robot.rpi;

import java.util.Set;

import org.xenei.robot.common.Compass;
import org.xenei.robot.common.Coordinates;
import org.xenei.robot.common.DistanceSensor;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.mapping.CoordinateMap;
import org.xenei.robot.common.mapping.Map;
import org.xenei.robot.common.mapping.Mapper;
import org.xenei.robot.common.utils.TimingUtils;
import org.xenei.robot.mapper.MapImpl;
import org.xenei.robot.mapper.MapperImpl;
import org.xenei.robot.rpi.sensors.Arduino;

import mil.nga.sf.Point;

public class Robut {
    private Compass compass;
    private DistanceSensor distSensor;
    private Map map;
    private Mapper mapper;
    private Position currentPosition;

    public Robut(Point origin) {
        compass = new CompassImpl();
        distSensor = new Arduino();
        map = new MapImpl();
        mapper = new MapperImpl(map);
        currentPosition = compass.getPosition(origin);
    }

    public void readSensors() {
        mapper.processSensorData(currentPosition, distSensor.sense());
    }

    public void updatePosition() {
        currentPosition = compass.getPosition(currentPosition);
        System.out.println("Current position: " + currentPosition);
    }

    public static void main(String[] args) {
        Robut r = new Robut(new Point(0, 0));

        while (true) {
            r.updatePosition();
            r.readSensors();
            printMap(r);
            TimingUtils.delay(500);
        }
    }

    private static void printMap(Robut r) {
        Set<Coordinates> obstacles = r.map.getObstacles();
        double[] max = { Double.MIN_VALUE, Double.MIN_VALUE };
        double[] min = { Double.MAX_VALUE, Double.MAX_VALUE };
        obstacles.stream().forEach(o -> {
            max[0] = Math.max(max[0], o.getX());
            max[1] = Math.max(max[1], o.getY());
            min[0] = Math.min(min[0], o.getX());
            min[1] = Math.min(min[1], o.getY());
            System.out.println(o);
        });
        double scaleX = Math.round((max[0] - min[0]) / 25);
        double scaleY = Math.round((max[1] - min[1]) / 80);
        CoordinateMap cMap = new CoordinateMap(Math.max(scaleX, scaleY));
        cMap.enable(obstacles, '#');
        System.out.println(cMap.toString());
    }
}
