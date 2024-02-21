package org.xenei.robot.rpi;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import org.locationtech.jts.geom.Coordinate;
import org.xenei.robot.common.Compass;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.Mover;
import org.xenei.robot.common.DistanceSensor;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.ScaleInfo;
import org.xenei.robot.common.mapping.CoordinateMap;
import org.xenei.robot.common.mapping.Map;
import org.xenei.robot.common.mapping.Mapper;
import org.xenei.robot.common.planning.Solution;
import org.xenei.robot.common.planning.Step;
import org.xenei.robot.common.utils.TimingUtils;
import org.xenei.robot.common.utils.AngleUtils;
import org.xenei.robot.common.utils.CoordUtils;
import org.xenei.robot.common.utils.RobutContext;
import org.xenei.robot.mapper.MapImpl;
import org.xenei.robot.mapper.MapperImpl;
import org.xenei.robot.rpi.sensors.Arduino;

import com.diozero.sbc.BoardInfo;
import com.diozero.sbc.DeviceFactoryHelper;


public class Robut {
    private final Compass compass;
    private final DistanceSensor distSensor;
    private final Map map;
    private final Mapper mapper;
    private Position currentPosition;
    private double width = 0.5; // meters
    private int wheelDiameter = 8; // cm
    private double maxSpeed = 60; // m/min
    private final Mover mover;

    public Robut(Coordinate origin) {
        compass = new CompassImpl();
        distSensor = new Arduino();
        map = new MapImpl(new RobutContext(ScaleInfo.DEFAULT));
        mapper = new MapperImpl(map);
        currentPosition = compass.getPosition(origin);
        mover = new RpiMover(compass, width, wheelDiameter, maxSpeed);
    }

    public Collection<Step> readSensors(Coordinate target, Solution solution) {
        return mapper.processSensorData(currentPosition, width, target, distSensor.sense());
    }

    public void updatePosition() {
        currentPosition = compass.getPosition(currentPosition);
        System.out.println("Current position: " + currentPosition);
    }

    public static void main(String[] args) {
        System.out.format( "Attempting %s %s\n", args[0], args[1]);
        Robut r = new Robut(new Coordinate(0, 0));
        int range = Integer.parseInt(args[1]);
        double theta = Math.toRadians( Double.parseDouble(args[0]));
        System.out.format( "%s is the next position", r.mover.move( Location.from(CoordUtils.fromAngle(theta, range))));
        
    }
}
