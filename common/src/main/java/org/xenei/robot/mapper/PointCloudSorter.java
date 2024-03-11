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
    /** The coordinates in the cloud */
    List<Coordinate> points = new ArrayList<>();
    /** matrix of distances */
    DoubleHalfMatrix dMatrix;
    /** matrix of flags for the shortest distances */
    IntHalfMatrix iMatrix;
    /** array of how many connections there are to each coordinate */
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
                    iMatrix.increment(i, j);
                }
            }
        }
        
        connections = iMatrix.reduction( IntHalfMatrix.plus );
        if (LOG.isDebugEnabled()) {
            LOG.debug( "dMatrix\n{}", dMatrix );
            LOG.debug( "iMatrix\n{}", iMatrix );
            LOG.debug("PCS created: connections:{}", connections.length);
        }
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
            LOG.debug("walk() returned one result");
            return geoms.get(0);
        }
        LOG.info("walk() returned a collection of {} results", geoms.size());
        return ctxt.geometryFactory.createGeometryCollection( geoms.toArray(new Geometry[geoms.size()]));
    }
    
    /**
     * Process the edge
     * @param i The index of the point we are processing
     * @param lst the list of coordinates that we are building.
     * @return the index of the next point or -1 if there are none.
     */
    private int processEdge(int i, List<Coordinate> lst) {
        for (int j=0;j<iMatrix.size();j++) {
            if (iMatrix.get(i,j) == 1) {
                lst.add(points.get(j));
                iMatrix.decrement(i,j);
                LOG.debug( "({},{})", i,j);
                connections[i]--;
                connections[j]--;
                return j;
            }
        }
        return -1;
    }
    
    private boolean walk(List<Geometry> geom) {
        // filter to find the minimum value greater than 0 for each row.
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
        // the list of coordinates t
        List<Coordinate> lst = new ArrayList<Coordinate>();
        
        // get the row with the lowest number of connections. -1 if no intersections were found
        int pointIdx = IntHalfMatrix.arrayReducer(connections, filter);
        if (pointIdx != -1){
            lst.add(points.get(pointIdx));
            while (pointIdx != -1) {
                pointIdx = processEdge(pointIdx,lst);
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
