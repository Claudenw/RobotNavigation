package org.xenei.robot.common;

public interface ListenerContainer {

    /**
     * Add a planner listener. The listener will be called when a planning move is
     * completed.
     * 
     * @param listener the listener to notify.
     */
    void addListener(Listener listener);

    /**
     * Notify listeners to reprocess data.
     */
    void notifyListeners();

}
