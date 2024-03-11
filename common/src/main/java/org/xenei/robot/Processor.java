package org.xenei.robot;

import java.util.Collection;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.arq.querybuilder.ExprFactory;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.geosparql.implementation.vocabulary.Geo;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.common.AbortedException;
import org.xenei.robot.common.DistanceSensor;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.Mover;
import org.xenei.robot.common.NavigationSnapshot;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.ScaleInfo;
import org.xenei.robot.common.mapping.Map;
import org.xenei.robot.common.mapping.Mapper;
import org.xenei.robot.common.planning.Planner;
import org.xenei.robot.common.planning.Solution;
import org.xenei.robot.common.planning.Step;
import org.xenei.robot.common.planning.Planner.Diff;
import org.xenei.robot.common.utils.AngleUtils;
import org.xenei.robot.common.utils.DoubleUtils;
import org.xenei.robot.common.utils.RobutContext;
import org.xenei.robot.mapper.MapImpl;
import org.xenei.robot.mapper.MapReports;
import org.xenei.robot.mapper.MapperImpl;
import org.xenei.robot.mapper.rdf.Namespace;
import org.xenei.robot.mapper.visualization.MapViz;
import org.xenei.robot.planner.PlannerImpl;

public class Processor {
    private static final Logger LOG = LoggerFactory.getLogger(Processor.class);

    public final Map map;
    private final RobutContext ctxt;
    public final Planner planner;
    private final Mapper mapper;
    private final DistanceSensor sensor;
    private final Mover mover;
    private final Supplier<Position> positionSupplier;

    public Processor(RobutContext ctxt, Mover mover, Supplier<Position> positionSupplier, DistanceSensor sensor) {
        this.ctxt = ctxt;
        this.mover = mover;
        this.positionSupplier = positionSupplier;
        this.sensor = sensor;
        map = new MapImpl(ctxt);
        mapper = new MapperImpl(map);
        LOG.debug("Initial position: ()", positionSupplier.get());
        planner = new PlannerImpl(map, positionSupplier);
    }
    
    public void add(Mapper.Visualization visualization) {
        planner.addListener(() -> visualization.redraw(planner.getTarget()));
    }


    private boolean checkTarget(NavigationSnapshot snapshot) {
        if (!mapper.equivalent(snapshot.currentPosition, planner.getRootTarget())) {
            // if we can see the final target go that way.
            if (mapper.isClearPath(snapshot.currentPosition, planner.getRootTarget())) {
                double newHeading = snapshot.currentPosition.headingTo(planner.getRootTarget());
                boolean cont = DoubleUtils.eq(newHeading, snapshot.currentPosition.getHeading());
                if (!cont) {
                    // heading is different so reset the heading, scan, and check again.
                    mover.setHeading(snapshot.currentPosition.headingTo(planner.getRootTarget()));
                    mapper.processSensorData(planner.getRootTarget(), snapshot, sensor.sense());
                    cont = mapper.isClearPath(snapshot.currentPosition, planner.getRootTarget());
                    if (!cont) {
                        // can't see the position really so reset the heading.
                        mover.setHeading(snapshot.currentPosition.getHeading());
                    }
                }
                if (cont) {
                    // we can really see the final position.
                    LOG.info("can see {} from {}", planner.getRootTarget(), snapshot.currentPosition);
                    Literal pathWkt = ctxt.graphGeomFactory.asWKTPath(ctxt.chassisInfo.radius, planner.getRootTarget(),
                            snapshot.currentPosition.getCoordinate());
                    Var wkt = Var.alloc("wkt");

                    ExprFactory exprF = new ExprFactory();
//                    System.out.println(MapReports.dumpQuery((MapImpl) map, new SelectBuilder() //
//                            .from(Namespace.UnionModel.getURI()) //
//                            .addWhere(Namespace.s, RDF.type, Namespace.Obst) //
//                            .addWhere(Namespace.s, Geo.AS_WKT_PROP, wkt)
//                            .addBind(ctxt.graphGeomFactory.calcDistance(exprF, pathWkt, wkt), "?dist")
//                            .addBind(exprF.eq(ctxt.graphGeomFactory.calcDistance(exprF, pathWkt, wkt), 0), "?le")));

                    AskBuilder ask = new AskBuilder().from(Namespace.UnionModel.getURI()) //
                            .addWhere(Namespace.s, RDF.type, Namespace.Obst) //
                            .addWhere(Namespace.s, Geo.AS_WKT_PROP, wkt)
                            .addFilter(exprF.eq(ctxt.graphGeomFactory.calcDistance(exprF, pathWkt, wkt), 0));
                    System.out.println(((MapImpl) map).ask(ask));
                    System.out.println(MapReports.dumpQuery((MapImpl) map,
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
        return mapper.isClearPath(snapshot.currentPosition, planner.getTarget());
    }
    
    private NavigationSnapshot newSnapshot() {
        return new NavigationSnapshot(positionSupplier.get(), planner.getTarget());
    }

    public void moveTo(Location finalCoord) throws AbortedException {
        moveTo(finalCoord, p -> {});
    }
    
    public void moveTo(Location finalCoord, AbortTest abortTest) throws AbortedException {
        NavigationSnapshot snapshot = new NavigationSnapshot(positionSupplier.get(), finalCoord.getCoordinate());
        mapper.processSensorData(planner.getRootTarget(), snapshot, sensor.sense());
        planner.setTarget(snapshot.target);
        while (planner.getTarget() != null) {
            Diff diff = planner.selectTarget();
            if (planner.getTarget() != null) {
                if (diff.didChange()) {
                    snapshot = newSnapshot();
                    // look where we are heading.
                    mapper.processSensorData(planner.getRootTarget(), snapshot, sensor.sense());
                    planner.notifyListeners();
                }
                // can we still see the target
                if (checkTarget(snapshot)) {
                    // move
                    Location relativeLoc = mover.position().relativeLocation(planner.getTarget());
                    map.setVisited(planner.getRootTarget(), mover.move(relativeLoc).getCoordinate());
                    snapshot = newSnapshot();
                    planner.registerPositionChange();
                    mapper.processSensorData(planner.getRootTarget(), snapshot, sensor.sense());
                }
                // should we abort
                abortTest.check(this);
                planner.notifyListeners();
                diff.reset();
            }
        }
        planner.notifyListeners();
        planner.recordSolution();
    }
    
    @FunctionalInterface
    interface AbortTest {
        void check(Processor processor) throws AbortedException;
    }
}