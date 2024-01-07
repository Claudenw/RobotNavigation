package org.xenei.robot.common.mapping;

import java.util.Comparator;
import java.util.Objects;
import java.util.UUID;

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Resource;
import org.locationtech.jts.geom.Geometry;

/**
 * Hashcode should be implemented as wkt().hashCode()

 */
public interface Obstacle {
    static Comparator<Obstacle> comp = (x,y) -> x.wkt().getLexicalForm().compareTo(y.wkt().getLexicalForm());
  
    static int hashCode(Obstacle o) {
        return o.wkt().hashCode();
    }
    
    static boolean equalsImpl(Obstacle left, Object right) {
        if (left == right)
            return true;
        if ((right == null) || !Obstacle.class.isAssignableFrom(right.getClass()))
            return false;
        
        return Objects.equals(left.wkt().getLexicalForm(), ((Obstacle)right).wkt().getLexicalForm());
    }
   
    Literal wkt();
    
    Geometry geom();
    
    UUID uuid();
    
    Resource rdf();
}
