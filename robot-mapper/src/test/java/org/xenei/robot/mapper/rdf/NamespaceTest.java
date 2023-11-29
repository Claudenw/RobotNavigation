package org.xenei.robot.mapper.rdf;

import org.apache.jena.rdf.model.Resource;
import org.junit.jupiter.api.Test;
import org.xenei.robot.mapper.MapReports;
import org.xenei.robot.mapper.rdf.Namespace;

import mil.nga.sf.Point;

public class NamespaceTest {
    
    @Test
    public void x() {
        Point p = new Point( -1, 3 );
        Resource r = Namespace.asRDF(p, null);
        System.out.println( MapReports.dumpModel( r.getModel() ));
    }

}
