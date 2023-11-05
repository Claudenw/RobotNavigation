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
        super(normalAtan(x, y),Math.sqrt(x*x+y*y), x, y);
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
        double newAngle = Coordinates.normalize(this.heading+cmd.getThetaRadians());
        if (cmd.getRange() == 0) {
            return new Position( this.getX(), this.getY(), newAngle);
        }
        
        double deltaX = cmd.getRange()*Math.cos(cmd.getThetaRadians()+this.getThetaRadians());
        double deltaY = cmd.getRange()*Math.sin(cmd.getThetaRadians()+this.getThetaRadians());
        
        return new Position( this.plus(Coordinates.fromRadians(newAngle, cmd.getRange())), newAngle);
    }
    
    @Override
    public String toString() {
        return String.format("Position[ x:%s y:%s heading:%s ]", getX(), getY(), Math.toDegrees(heading));
    }
}
