package org.xenei.robot.rpi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Scanner;

import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.arq.querybuilder.ExprFactory;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.geosparql.implementation.vocabulary.Geo;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.vocabulary.RDF;
import org.locationtech.jts.geom.Coordinate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.common.Compass;
import org.xenei.robot.common.DistanceSensor;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.Mover;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.ScaleInfo;
import org.xenei.robot.common.mapping.Map;
import org.xenei.robot.common.mapping.Mapper;
import org.xenei.robot.common.planning.Planner;
import org.xenei.robot.common.planning.Planner.Diff;
import org.xenei.robot.common.planning.Solution;
import org.xenei.robot.common.planning.Step;
import org.xenei.robot.common.utils.CoordUtils;
import org.xenei.robot.common.utils.DoubleUtils;
import org.xenei.robot.common.utils.RobutContext;
import org.xenei.robot.mapper.MapImpl;
import org.xenei.robot.mapper.MapReports;
import org.xenei.robot.mapper.MapperImpl;
import org.xenei.robot.mapper.rdf.Namespace;
import org.xenei.robot.planner.PlannerImpl;
import org.xenei.robot.rpi.sensors.Arduino;

public class Robut {
    private final RobutContext ctxt;
    private final Compass compass;
    private final DistanceSensor distSensor;
    private final Map map;
    private final Mapper mapper;
    private Position currentPosition;
    private double width = 0.24; // meters
    private double buffer = width / 2.0;
    private int wheelDiameter = 8; // cm
    private double maxSpeed = 60; // m/min
    private final Mover mover;
    private final Planner planner;

    private static final Logger LOG = LoggerFactory.getLogger(Robut.class);
    
    public Robut(Coordinate origin) throws InterruptedException {
        compass = new CompassImpl();
        ctxt = new RobutContext(ScaleInfo.DEFAULT);
        distSensor = new Arduino();
        map = new MapImpl(new RobutContext(ScaleInfo.DEFAULT));
        mapper = new MapperImpl(map);
        currentPosition = compass.getPosition(origin);
        LOG.debug("Initial position: ()", currentPosition);
        mover = new RpiMover(compass, currentPosition.getCoordinate(), width, wheelDiameter, maxSpeed);
        planner = new PlannerImpl(map, currentPosition, buffer);
        double d = compass.instantHeading();
        LOG.debug("{} {} degrees compas {} {} degrees", currentPosition, Math.toDegrees(currentPosition.getHeading()),
                d, Math.toDegrees(d));
    }

    public Collection<Step> readSensors(Coordinate target, Solution solution) {
        return mapper.processSensorData(currentPosition, width, target, distSensor.sense());
    }

    public void updatePosition() {
        currentPosition = compass.getPosition(currentPosition);
        System.out.println("Current position: " + currentPosition);
    }

    public void status() {
        System.out.format("Heading: %s\nPosition: %s\nObstacle: %s\n", compass.hashCode(), currentPosition,
                distSensor.sense());
    }

    private Collection<Step> processSensor() {
        return mapper.processSensorData(planner.getCurrentPosition(), buffer, planner.getRootTarget(),
                distSensor.sense());
    }

    public boolean checkTarget() {
        if (!mapper.equivalent(planner.getCurrentPosition(), planner.getRootTarget())) {
            // if we can see the final target go that way.
            if (mapper.isClearPath(planner.getCurrentPosition(), planner.getRootTarget(), buffer)) {
                double newHeading = planner.getCurrentPosition().headingTo(planner.getRootTarget());
                boolean cont = DoubleUtils.eq(newHeading, planner.getCurrentPosition().getHeading());
                if (!cont) {
                    // heading is different so reset the heading, scan, and check again.
                    mover.setHeading(planner.getCurrentPosition().headingTo(planner.getRootTarget()));
                    processSensor();
                    cont = mapper.isClearPath(planner.getCurrentPosition(), planner.getRootTarget(), buffer);
                    if (!cont) {
                        // can't see the position really so reset the heading.
                        mover.setHeading(planner.getCurrentPosition().getHeading());
                    }
                }
                if (cont) {
                    // we can really see the final position.
                    // LOG.info("can see {} from {}", planner.getRootTarget(),
                    // planner.getCurrentPosition());
                    Literal pathWkt = ctxt.graphGeomFactory.asWKTPath(buffer, planner.getRootTarget(),
                            planner.getCurrentPosition().getCoordinate());
                    Var wkt = Var.alloc("wkt");

                    ExprFactory exprF = new ExprFactory();
                    LOG.debug("checkTarget query:\n"+MapReports.dumpQuery((MapImpl) map, new SelectBuilder() //
                            .from(Namespace.UnionModel.getURI()) //
                            .addWhere(Namespace.s, RDF.type, Namespace.Obst) //
                            .addWhere(Namespace.s, Geo.AS_WKT_PROP, wkt)
                            .addBind(ctxt.graphGeomFactory.calcDistance(exprF, pathWkt, wkt), "?dist")
                            .addBind(exprF.eq(ctxt.graphGeomFactory.calcDistance(exprF, pathWkt, wkt), 0), "?le")));

                    AskBuilder ask = new AskBuilder().from(Namespace.UnionModel.getURI()) //
                            .addWhere(Namespace.s, RDF.type, Namespace.Obst) //
                            .addWhere(Namespace.s, Geo.AS_WKT_PROP, wkt)
                            .addFilter(exprF.eq(ctxt.graphGeomFactory.calcDistance(exprF, pathWkt, wkt), 0));
                    LOG.debug( "Query result: "+((MapImpl) map).ask(ask));
                    LOG.debug("Obstacles:\n"+MapReports.dumpQuery((MapImpl) map,
                            new SelectBuilder().from(Namespace.UnionModel.getURI())
                                    .addWhere(Namespace.s, Namespace.p, Namespace.o)
                                    .addWhere(Namespace.s, RDF.type, Namespace.Obst)));
                    planner.replaceTarget(planner.getRootTarget());
                    planner.notifyListeners();
                    return true;
                }
            }
        }
        // if we can not see the target replan.
        return mapper.isClearPath(planner.getCurrentPosition(), planner.getTarget(), buffer);
    }

    public void executeMovement() {
        processSensor();

        int stepCount = 0;
        int maxLoops = 100;
        while (planner.getTarget() != null) {
            Diff diff = planner.selectTarget();
            if (planner.getTarget() != null) {
                if (diff.didChange()) {
                    // look where we are heading.
                    processSensor();
                    planner.notifyListeners();
                }
                // can we still see the target
                if (checkTarget()) {
                    // move
                    Location relativeLoc = planner.getCurrentPosition().relativeLocation(planner.getTarget());
                    planner.changeCurrentPosition(mover.move(relativeLoc));
                    processSensor();
                }
                if (maxLoops < stepCount++) {
                    LOG.error("Did not find solution in " + maxLoops + " steps");
                    return;
                }
                planner.notifyListeners();
                LOG.debug(MapReports.dumpDistance((MapImpl) map));
                LOG.debug(MapReports.dumpObstacles((MapImpl) map));
                LOG.debug(MapReports.dumpObstacleDistance((MapImpl) map));
                diff.reset();
            }
            checkContinue();
        }
        planner.notifyListeners();
        Solution solution = planner.getSolution();
        // CoordinateUtils.assertEquivalent(startCoord, solution.start(), buffer);
        // CoordinateUtils.assertEquivalent(startCoord, solution.start(), buffer);
        LOG.info(MapReports.dumpDistance((MapImpl) map));
        LOG.info("Solution");
        solution.stream().forEach(s -> LOG.info(s.toString()));
        LOG.info("SUCCESS");
        solution.simplify((a, b) -> map.isClearPath(a, b, buffer));
        LOG.info("Solution 2");
        solution.stream().forEach(s -> LOG.info(s.toString()));
    }

    static BufferedReader BUFFER;
    
    private static void checkContinue() {
        try {
        System.out.print( "Continue: ");
        String line = BUFFER.readLine();
        if (line.toLowerCase().startsWith("y")) {
            return;
        }
        } catch (IOException e) {
            throw new RuntimeException( "Input error", e );
        }
        throw new RuntimeException( "Stop requested");
    }
    
    public static void main(String[] args) throws Exception {
        
        BUFFER = new BufferedReader(new InputStreamReader(System.in));
        
        Robut r = new Robut(new Coordinate(0, 0));

        while (true) {
            System.out.print( "Target (theta, range): ");
            String line = BUFFER.readLine();
            System.out.format("Read: %s\n", line);
            if (line == null || line.length() == 0) {
                return;
            }
            Scanner in = new Scanner(line);
            double angle = in.nextDouble();
            double range = in.nextDouble();
            LOG.debug(String.format("Attempting %s %s\n", angle, range));
            double theta = Math.toRadians(angle);
            Location relativeLocation = Location.from(CoordUtils.fromAngle(theta, range));
            Position target = r.currentPosition.nextPosition(relativeLocation);
            Position orientation = r.planner.setTarget(target);
            r.mover.setHeading(orientation.getHeading());
            r.executeMovement();
            r.status();
        }
    }
}
