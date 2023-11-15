package org.xenei.robot.planner.rdf;

import org.apache.jena.arq.querybuilder.Converters;
import org.apache.jena.graph.Node;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.path.P_Alt;
import org.apache.jena.sparql.path.P_Distinct;
import org.apache.jena.sparql.path.P_FixedLength;
import org.apache.jena.sparql.path.P_Inverse;
import org.apache.jena.sparql.path.P_Link;
import org.apache.jena.sparql.path.P_Mod;
import org.apache.jena.sparql.path.P_Multi;
import org.apache.jena.sparql.path.P_NegPropSet;
import org.apache.jena.sparql.path.P_OneOrMore1;
import org.apache.jena.sparql.path.P_OneOrMoreN;
import org.apache.jena.sparql.path.P_ReverseLink;
import org.apache.jena.sparql.path.P_Seq;
import org.apache.jena.sparql.path.P_Shortest;
import org.apache.jena.sparql.path.P_ZeroOrMore1;
import org.apache.jena.sparql.path.P_ZeroOrMoreN;
import org.apache.jena.sparql.path.P_ZeroOrOne;
import org.apache.jena.sparql.path.Path;

public class PathFactory {
    
    private final PrefixMapping pMap;

    /**
     * Constructs an expression factor with the specified prefix definitions.
     *
     * @param pMap the PrefixMapping to use in the expressions.
     */
    public PathFactory(PrefixMapping pMap) {
        this.pMap = pMap;
    }

    /**
     * Constructs an expression factory with the prefix definitions found in
     * {@code PrefixMapping.Extended}
     *
     * @see PrefixMapping#Extended
     */
    public PathFactory() {
        this(PrefixMapping.Extended);
    }

    public Path makePath(Object o) {
        if (o instanceof Path) {
            return (Path)o;
        }
        if (o instanceof Node) {
            return new P_Link((Node) o);
        }
        return makePath(Converters.makeNodeOrPath(o, pMap));
    }
    
    public P_NegPropSet negPropSet() {
        return new P_NegPropSet();
    }
    
    public P_ReverseLink reverseLink(Object o) {
        if (o instanceof Node) {
            return new P_ReverseLink((Node)o);
        }
        return reverseLink(Converters.makeNode(o, pMap));
    }
    
    public P_Distinct distinct(Object o) {
        return new P_Distinct(makePath(o));
    }
    
    public P_FixedLength fixedLength(Object o, long maxHops) {
        return new P_FixedLength(makePath(o), maxHops);
    }
    
    public P_Inverse inverse(Object o) {
        return new P_Inverse(makePath(o));
    }
    
    public P_Mod mod(Object o, long min, long max) {
        return new P_Mod(makePath(o), min, max);
    }
    
    public P_Multi multi(Object o) {
        return new P_Multi(makePath(o));
    }
    
    public P_OneOrMore1 oneOrMoreUnique(Object o) {
        return new P_OneOrMore1(makePath(o));
    }
    
    public P_OneOrMoreN oneOrMore(Object o) {
        return new P_OneOrMoreN(makePath(o));
    }
    
    public P_Shortest shortest(Object o) {
        return new P_Shortest(makePath(o));
    }
    
    public P_ZeroOrMore1 zeroOrMoreUnique(Object o) {
        return new P_ZeroOrMore1(makePath(o));
    }
    
    public P_ZeroOrMoreN zeroOrMore(Object o) {
        return new P_ZeroOrMoreN(makePath(o));
    }
    
    public P_ZeroOrOne zeroOrOne(Object o) {
        return new P_ZeroOrOne(makePath(o));
    }
    
    public P_Alt alt(Object o1, Object o2) {
        return new P_Alt(makePath(o1), makePath(o2));
    }
    
    public P_Seq seq(Object o1, Object o2) {
        return new P_Seq(makePath(o1), makePath(o2));
    }
}
