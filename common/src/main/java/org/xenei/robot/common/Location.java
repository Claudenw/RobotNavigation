package org.xenei.robot.common;

import java.util.Comparator;

import org.locationtech.jts.geom.Coordinate;
import org.xenei.robot.common.utils.AngleUtils;
import org.xenei.robot.common.utils.CoordUtils;
import org.xenei.robot.common.utils.DoubleUtils;

/**
 * An immutable definition of a set of Coordinates (X, Y) or (theta, r) values.
 * Holds both representations.
 */
public class Location extends AbstractFrontsCoordinate<Location> {
    public static final Location ORIGIN = new Location(0, 0);

    /**
     * Compares Coordinates by angle and then range.
     */
    public static Comparator<Location> ThetaCompr = (one, two) -> {
        int x = Double.compare(one.theta(), two.theta());
        return x == 0 ? Double.compare(one.range(), two.range()) : x;
    };

    /**
     * Compares Coordinates by range and then angle.
     */
    public static Comparator<Location> RangeCompr = (one, two) -> {
        int x = Double.compare(one.range(), two.range());
        return x == 0 ? Double.compare(one.theta(), two.theta()) : x;
    };

    /**
     * Construct coordinates from a Point.
     * 
     * @param p The point representing the X and Y positions
     */
    public Location(Coordinate p) {
        super(p);
    }

    /**
     * Construct coordinates from a Point.
     * 
     * @param p The point representing the X and Y positions
     */
    public Location(FrontsCoordinate p) {
        super(p.getCoordinate());
    }

    /**
     * Construct coordinates from X and Y positions.
     * 
     * @param x the X position.
     * @param y the Y position.
     */
    public Location(double x, double y) {
        super(new Coordinate(x, y));
    }
    
    @Override
    protected Location fromCoordinate(Coordinate base) {
        return new Location(base);
    }

    /**
     * Calculate the heading from this coordinate to the point.
     * 
     * @param point the heading to calculate angle to.
     * @return the heading to the point.
     */
    public double headingTo(Coordinate point) {
        Location c = new Location(this.minus(point));
        // vector will be pointing the wrong way. So reverse it.
        return AngleUtils.normalize(c.theta() + Math.PI);
    }

    /**
     * Calculate the heading from this coordinate to the point.
     * 
     * @param point the heading to calculate angle to.
     * @return the heading to the point.
     */
    public double headingTo(FrontsCoordinate point) {
        Location diff = this.minus(point);
        double theta = AngleUtils.normalize(Math.atan(diff.getY() / diff.getX()));
        boolean yNeg = DoubleUtils.isNeg(diff.getY());
        boolean tNeg = DoubleUtils.isNeg(theta);

        if (yNeg && !tNeg) {
            theta -= Math.PI;
        } else if (!yNeg && tNeg) {
            theta += Math.PI;
        }
        // angle will be pointing the wrong way, so reverse it.
        return AngleUtils.normalize(theta + Math.PI);

    }

    /**
     * Return the angle in radians from the origin.
     * 
     * @return the angle in radians from the origin to this coordinates.
     */
    public double theta() {
        return ORIGIN.headingTo(this);
    }

    /**
     * Get the range to the coordinates in meters.
     * 
     * @return the range to the coordinates.
     */
    public double range() {
        return ORIGIN.distance(this);
    }
    
    public boolean near(FrontsCoordinate c, double err) {
        return this.distance(c) <= err;
    }

    @Override
    public String toString() {
        return String.format("Location[ %s r:%.2f]", CoordUtils.toString(getCoordinate(), 4), range());
    }

    @Override
    public FrontsCoordinate copy() {
        return new Location(getCoordinate());
    }

}
