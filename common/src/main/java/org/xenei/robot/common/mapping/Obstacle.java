package org.xenei.robot.common.mapping;

import java.util.Comparator;
import java.util.Objects;
import java.util.UUID;

import org.apache.jena.geosparql.implementation.vocabulary.Geo;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.locationtech.jts.geom.Geometry;
import org.xenei.robot.mapper.rdf.Namespace;

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
   
    Geometry geom();
    
    UUID uuid();

    Literal wkt();

    default Resource rdf() {
        return ResourceFactory.createResource("urn:uuid:" + uuid().toString());
    }
    
    default Resource in(Model model) {
        Resource result = model.createResource(rdf().getURI(), Namespace.Obst);
        result.addLiteral(Geo.AS_WKT_PROP, wkt());
        return result;
    }
}
