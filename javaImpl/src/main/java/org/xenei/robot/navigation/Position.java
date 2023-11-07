package org.xenei.robot.navigation;

public class Position extends Coordinates {

    private double heading;

    public Position() {
        this(0.0, 0.0);
    }

    public Position(Coordinates coord) {
        this(coord, 0.0);
    }

    public Position(Coordinates coord, double heading) {
        super(coord);
        this.heading = heading;
    }

    public Position(double x, double y) {
        this(x, y, 0.0);
    }

    public Position(double x, double y, double heading) {
        super(Math.atan(y/x), Math.sqrt(x * x + y * y), x, y);
        this.heading = heading;
    }

    public double getHeadingRadians() {
        return heading;
    }

    public double getHeadingDegrees() {
        return Math.toDegrees(heading);
    }

    public void setHeading(double radians) {
        this.heading = radians;
    }

    public Position nextPosition(Coordinates cmd) {
        if (cmd.getRange() == 0) {
            return new Position(this.getX(), this.getY(), cmd.getThetaRadians());
        }
        Coordinates nextCoord = this.plus(cmd);
        return new Position(nextCoord, cmd.getThetaRadians());
    }

    @Override
    public String toString() {
        return String.format("Position[ %s heading:%.4f ]", super.toString(), Math.toDegrees(heading));
    }
}
