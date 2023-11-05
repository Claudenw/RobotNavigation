package org.xenei.robot.navigation;

public class Navigate {
    Coordinates destination;
    Position current;

    
    public Navigate(Coordinates from, Coordinates to) {
        this.destination = to;
        this.current = new Position(from);
    }
    
    /**
     * Returns the next location given the 
     * @param cmd
     * @return
     */
    public Position execute(Coordinates cmd) {
        return current.nextPosition(cmd);
    }
}
