package org.xenei.robot.common;

import org.locationtech.jts.geom.Coordinate;

public class UnmodifiableCoordinate extends Coordinate {

    private static final long serialVersionUID = 1674775782994045928L;

    public static UnmodifiableCoordinate make(Coordinate coordinate) {
        return coordinate instanceof UnmodifiableCoordinate ? (UnmodifiableCoordinate) coordinate
                : new UnmodifiableCoordinate(coordinate);
    }

    private UnmodifiableCoordinate(Coordinate delegate) {
        super(delegate);
    }

    @Override
    public void setX(double x) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setY(double y) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setZ(double z) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setM(double m) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setOrdinate(int ordinateIndex, double value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setCoordinate(Coordinate other) {
        throw new UnsupportedOperationException();
    }
}