package org.xenei.robot.mapper;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Stack;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.common.utils.DoubleUtils;
import org.xenei.robot.common.utils.RobutContext;

public class PointCloudSorter {
    private static final Logger LOG = LoggerFactory.getLogger(PointCloudSorter.class);
    RobutContext ctxt;
    List<Coordinate> points = new ArrayList<>();
    DoubleHalfMatrix dMatrix;
    IntHalfMatrix iMatrix;
    int[] connections;
    
    public PointCloudSorter(RobutContext ctxt, Set<Coordinate> cSet) {
        LOG.debug("Starting PCS >>>>>>>>>>>>>>>>>>>>>>");
        this.ctxt = ctxt;
        dMatrix = new DoubleHalfMatrix(cSet.size());
        iMatrix = new IntHalfMatrix(cSet.size());
        points.addAll(cSet);
        for (int i=0;i<points.size()-1;i++) {
            Coordinate pi = points.get(i);
            for (int j=i+1;j<points.size();j++) {
                Coordinate pj = points.get(j);
                double d = ctxt.scaleInfo.precise(pi.distance(pj));
                dMatrix.set(i, j, d);
                if (d <= ctxt.scaleInfo.getResolution()) {
                    iMatrix.add(i, j);
                }
            }
        }
        
        connections = iMatrix.reduction( IntHalfMatrix.plus );

        LOG.debug( dMatrix.toString() );
        LOG.debug( iMatrix.toString() );
        LOG.info("PCS created");
    }
    
    public Geometry walk() {

        List<Geometry> geoms =  new ArrayList<Geometry>();
        while (walk(geoms)) {
            // work done above
        }
        if (geoms.size() == 0) {
            throw new IllegalStateException();
        }
        if (geoms.size() == 1) {
            return geoms.get(0);
        }
        
        return ctxt.geometryFactory.createGeometryCollection( geoms.toArray(new Geometry[geoms.size()]));
    }
    
    private int processEdge(int i, List<Coordinate> lst) {
        for (int j=0;j<iMatrix.size();j++) {
            if (iMatrix.get(i,j) == 1) {
                lst.add(points.get(j));
                iMatrix.subtract(i,j);
                LOG.debug( "(%s,%s)", i,j);
                connections[i]--;
                connections[j]--;
                return j;
            }
        }
        return -1;
    }
    
    private boolean walk(List<Geometry> geom) {
        IntHalfMatrix.Reducer filter = new IntHalfMatrix.Reducer() {
            int min = Integer.MAX_VALUE;
            int pos = 0;
            @Override
            public int apply(int previousValue, int arg) {
                if (min == Integer.MAX_VALUE) {
                    previousValue = -1;
                }
                if (arg > 0 && arg < min) {
                    min = arg;
                    int ret = pos;
                    pos++;
                    return ret; 
                }
                pos++;
                return previousValue;
            }};
        List<Coordinate> lst = new ArrayList<Coordinate>();
        int i = IntHalfMatrix.arrayReducer(connections, filter);
        if (i != -1){
            lst.add(points.get(i));
            while (i != -1) {
                i = processEdge(i,lst);
            }
            LOG.debug( iMatrix.toString() );
            if (lst.size() == 1) {
                geom.add(ctxt.geometryFactory.createPoint(lst.get(0)));
            }
            else {
                geom.add(ctxt.geometryFactory.createLineString( lst.toArray(new Coordinate[lst.size()])));
            }
            LOG.debug( geom.get(geom.size()-1).toString());
            return true;
        }
        return false;
    }
}
