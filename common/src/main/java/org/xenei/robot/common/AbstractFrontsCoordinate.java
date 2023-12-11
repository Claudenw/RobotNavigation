package org.xenei.robot.common;

import org.locationtech.jts.geom.Coordinate;
import org.xenei.robot.common.utils.AngleUtils;
import org.xenei.robot.common.utils.CoordUtils;
import org.xenei.robot.common.utils.DoubleUtils;

public abstract class AbstractFrontsCoordinate implements FrontsCoordinate {

    UnmodifiableCoordinate coordinate;

    protected AbstractFrontsCoordinate(Coordinate coordinate) {
        this.coordinate = UnmodifiableCoordinate.make(coordinate);
    }

    protected AbstractFrontsCoordinate(AbstractFrontsCoordinate coordinate) {
        this.coordinate = coordinate.coordinate;
    }

    @Override
    public UnmodifiableCoordinate getCoordinate() {
        return coordinate;
    }

    @Override
    final public double getX() {
        return coordinate.getX();
    }

    @Override
    final public double getY() {
        return coordinate.getY();
    }

    @Override
    final public double getZ() {
        return coordinate.getZ();
    }

    @Override
    final public double getM() {
        return coordinate.getM();
    }

    @Override
    final public double getOrdinate(int ordinateIndex) {
        return coordinate.getOrdinate(ordinateIndex);
    }

    @Override
    final public boolean equals2D(Coordinate other) {
        return coordinate.equals2D(other);
    }

    @Override
    final public boolean equals2D(FrontsCoordinate other) {
        return coordinate.equals2D(other.getCoordinate());
    }

    @Override
    final public boolean equals2D(Coordinate c, double tolerance) {
        return coordinate.equals2D(c, tolerance);
    }

    @Override
    final public boolean equals2D(FrontsCoordinate c, double tolerance) {
        return coordinate.equals2D(c.getCoordinate(), tolerance);
    }

    @Override
    final public boolean equals3D(Coordinate other) {
        return coordinate.equals3D(other);
    }

    @Override
    final public boolean equals3D(FrontsCoordinate other) {
        return coordinate.equals3D(other.getCoordinate());
    }

    @Override
    final public boolean equalInZ(Coordinate c, double tolerance) {
        return coordinate.equalInZ(c, tolerance);
    }

    @Override
    final public boolean equalInZ(FrontsCoordinate c, double tolerance) {
        return coordinate.equalInZ(c.getCoordinate(), tolerance);
    }

    @Override
    final public int compareTo(Coordinate o) {
        return coordinate.compareTo(o);
    }

    @Override
    final public int compareTo(FrontsCoordinate o) {
        return coordinate.compareTo(o.getCoordinate());
    }

    @Override
    final public double distance(Coordinate c) {
        return coordinate.distance(c);
    }

    @Override
    final public double distance(FrontsCoordinate c) {
        return coordinate.distance(c.getCoordinate());
    }

    @Override
    final public double distance3D(Coordinate c) {
        return coordinate.distance3D(c);
    }

    @Override
    final public double distance3D(FrontsCoordinate c) {
        return coordinate.distance3D(c.getCoordinate());
    }
    
    abstract protected <T extends AbstractFrontsCoordinate> T fromCoordinate(Coordinate base); 

    public <T extends AbstractFrontsCoordinate> T minus(Coordinate other) {
        return fromCoordinate(new Coordinate( getX() - other.getX(), getY()-other.getY()));
    }
    
    public <T extends AbstractFrontsCoordinate> T minus(FrontsCoordinate other) {
        return fromCoordinate(new Coordinate( getX() - other.getX(), getY()-other.getY()));
    }
    
    public <T extends AbstractFrontsCoordinate> T plus(Coordinate other) {
        return fromCoordinate(new Coordinate( getX()+other.getX(), getY()+other.getY()));
    }
    
    public <T extends AbstractFrontsCoordinate> T plus(FrontsCoordinate other) {
        return fromCoordinate(new Coordinate( getX()+other.getX(), getY()+other.getY()));
    }
    
    public double angleBetween(FrontsCoordinate other) {
        return angleBetween(other.getCoordinate());
    }
    
    public double angleBetween(Coordinate dest) {
        return CoordUtils.angleBetween(this.getCoordinate(), dest);
    }
}
